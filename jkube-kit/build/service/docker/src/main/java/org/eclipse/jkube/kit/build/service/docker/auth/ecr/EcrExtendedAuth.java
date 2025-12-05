/*
 * Copyright (c) 2019 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at:
 *
 *     https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.jkube.kit.build.service.docker.auth.ecr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.eclipse.jkube.kit.build.api.auth.AuthConfig;
import org.eclipse.jkube.kit.common.KitLogger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/**
 * Exchange local stored credentials for temporary ecr credentials
 *
 * @author chas
 * @since 2016-12-9
 */
public class EcrExtendedAuth {

    private static final Pattern AWS_REGISTRY =
            Pattern.compile("^(\\d{12})\\.dkr\\.ecr\\.([a-z\\-0-9]+)\\.amazonaws\\.com$");

    private static final Pattern LOCALSTACK_REGISTRY =
            Pattern.compile("^(\\d{12})\\.dkr\\.ecr\\.([a-z0-9-]+)\\.localhost\\.localstack\\.cloud:(\\d+)$");
    private final KitLogger logger;
    private final boolean isAwsRegistry;
    private final String accountId;
    private final String region;

    /**
     * Is given the registry an ecr registry?
     *
     * @param registry the registry name
     * @return true, if the registry matches the ecr pattern
     */
    public static boolean isAwsRegistry(String registry) {
        return (registry != null) && (AWS_REGISTRY.matcher(registry).matches() || LOCALSTACK_REGISTRY.matcher(registry).matches());
    }

    /**
     * Initialize an extended authentication for ecr registry.
     *
     * @param logger Kit Logger
     * @param registry The registry, we may or may not be an ecr registry.
     */
    public EcrExtendedAuth(KitLogger logger, String registry) {
        this.logger = logger;
        Matcher matcher = AWS_REGISTRY.matcher(registry);
        Matcher matcherLocalStack = LOCALSTACK_REGISTRY.matcher(registry);
        isAwsRegistry = matcher.matches() || matcherLocalStack.matches();
        if (isAwsRegistry) {
            if (matcher.matches()) {
                accountId = matcher.group(1);
                region = matcher.group(2);
            } else {
                accountId = matcherLocalStack.group(1);
                region = matcherLocalStack.group(2);
            }
        } else {
            accountId = null;
            region = null;
        }
        logger.debug("registry = %s, isValid= %b", registry, isAwsRegistry);
    }

    /**
     * Is the registry an ecr registry?
     * @return true, if the registry matches the ecr pattern
     */
    public boolean isAwsRegistry() {
        return isAwsRegistry;
    }

    /**
     * Perform extended authentication.  Use the provided credentials as IAM credentials and
     * get a temporary ECR token.
     *
     * @param localCredentials IAM id/secret
     * @return ECR base64 encoded username:password
     * @throws IOException IO Exception
     */
    public AuthConfig extendedAuth(AuthConfig localCredentials) throws IOException {
        JsonObject jo = getAuthorizationToken(localCredentials);

        JsonArray authorizationDatas = jo.getAsJsonArray("authorizationData");
        JsonObject authorizationData = authorizationDatas.get(0).getAsJsonObject();
        String authorizationToken = authorizationData.get("authorizationToken").getAsString();

        return AuthConfig.fromCredentialsEncoded(authorizationToken, "none");
    }

    private JsonObject getAuthorizationToken(AuthConfig localCredentials) throws IOException {
        HttpPost request = createSignedRequest(localCredentials, new Date());
        return executeRequest(createClient(), request);
    }

    CloseableHttpClient createClient() {
        return HttpClients.custom().useSystemProperties().build();
    }

    private JsonObject executeRequest(CloseableHttpClient client, HttpPost request) throws IOException {
        try {
            CloseableHttpResponse response = client.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            logger.debug("Response status %d", statusCode);

            HttpEntity entity = response.getEntity();
            if (statusCode != HttpStatus.SC_OK) {
                // More robust error handling for clear message
                Reader errorReader = new InputStreamReader(entity.getContent(), StandardCharsets.UTF_8);
                String errorMessage = new BufferedReader(errorReader).lines().collect(Collectors.joining("\n"));
                logger.error("AWS authentication failure. Status: %d, Response: %s", statusCode, errorMessage);
                throw new IOException("AWS authentication failure - Status: " + statusCode + ", Response: " + errorMessage);
            }

            Reader jr = new InputStreamReader(entity.getContent(), StandardCharsets.UTF_8);
            return new Gson().fromJson(jr, JsonObject.class);
        }
        finally {
            client.close();
        }
    }

    /**
     * Get the ECR endpoint URL. Can be overridden for testing.
     * @return endpoint URL from AWS_ENDPOINT_URL environment variable, or null for standard AWS ECR
     */
    protected String getEndpointUrl() {
        return System.getenv("AWS_ENDPOINT_URL");
    }

    HttpPost createSignedRequest(AuthConfig localCredentials, Date time) {
        // Determine if using LocalStack or AWS ECR. If using localstack for testing/dev create an env variable AWS_ENDPOINT_URL which points to localstack api endpoint
        String endpoint = getEndpointUrl();
        String requestUrl;
        String hostForSigning;

        if (endpoint != null && !endpoint.isEmpty()) {
            // Using LocalStack or custom endpoint
            // Preserve the protocol (http/https) from the endpoint
            if (!endpoint.endsWith("/")) {
                endpoint = endpoint + "/";
            }
            requestUrl = endpoint;

            // Extract host:port from endpoint URL for signing
            String cleanEndpoint = endpoint.replace("https://", "").replace("http://", "");
            if (cleanEndpoint.endsWith("/")) {
                cleanEndpoint = cleanEndpoint.substring(0, cleanEndpoint.length() - 1);
            }

            // For signing, remove port if present
            int portIndex = cleanEndpoint.lastIndexOf(':');
            if (portIndex > 0 && cleanEndpoint.substring(portIndex + 1).matches("\\d+")) {
                hostForSigning = cleanEndpoint.substring(0, portIndex);
            } else {
                hostForSigning = cleanEndpoint;
            }
        } else {
            // Using real AWS ECR
            hostForSigning = "api.ecr." + region + ".amazonaws.com";
            requestUrl = "https://" + hostForSigning + "/";
        }

        logger.debug("Get ECR AuthorizationToken from %s", requestUrl);

        HttpPost request = new HttpPost(requestUrl);
        // IMPORTANT: host header for signing must NOT include port
        request.setHeader("host", hostForSigning);
        request.setHeader("Content-Type", "application/x-amz-json-1.1");
        request.setHeader("X-Amz-Target", "AmazonEC2ContainerRegistry_V20150921.GetAuthorizationToken");
        request.setEntity(new StringEntity("{\"registryIds\":[\""+ accountId + "\"]}", StandardCharsets.UTF_8));

        AwsSigner4 signer = new AwsSigner4(region, "ecr");
        signer.sign(request, localCredentials, time);
        return request;
    }
}
