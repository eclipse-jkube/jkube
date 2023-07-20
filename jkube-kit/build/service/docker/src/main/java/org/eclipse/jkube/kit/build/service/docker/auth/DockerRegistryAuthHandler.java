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
package org.eclipse.jkube.kit.build.service.docker.auth;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Optional;
import java.util.function.UnaryOperator;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.eclipse.jkube.kit.build.api.auth.RegistryAuthConfig;
import org.eclipse.jkube.kit.build.api.auth.RegistryAuthHandler;
import org.eclipse.jkube.kit.build.api.auth.AuthConfig;
import org.eclipse.jkube.kit.common.KitLogger;

/**
 * @author roland
 * @since 21.10.18
 */
public class DockerRegistryAuthHandler implements RegistryAuthHandler {

    static final String DOCKER_LOGIN_DEFAULT_REGISTRY = "https://index.docker.io/v1/";

    private final KitLogger log;
    private final Gson gson;

    public DockerRegistryAuthHandler(KitLogger log) {
        this.log = log;
        this.gson = new Gson();

    }

    @Override
    public String getId() {
        return "docker";
    }

    @Override
    public AuthConfig create(RegistryAuthConfig.Kind kind, String user, String registry, UnaryOperator<String> decryptor) {
        return readDockerConfig().map(d -> extractAuthConfigFromDocker(d, registry)).orElse(null);
    }

    private AuthConfig extractAuthConfigFromDocker(JsonObject dockerConfig, String registry) {
        String registryToLookup = registry != null ? registry : DOCKER_LOGIN_DEFAULT_REGISTRY;

        try {
            if (dockerConfig.has("credHelpers") || dockerConfig.has("credsStore")) {
                if (dockerConfig.has("credHelpers")) {
                    final JsonObject credHelpers = dockerConfig.getAsJsonObject("credHelpers");
                    if (credHelpers.has(registryToLookup)) {
                        return extractAuthConfigFromCredentialsHelper(registryToLookup, credHelpers.get(registryToLookup).getAsString());
                    }
                }
                if (dockerConfig.has("credsStore")) {
                    return extractAuthConfigFromCredentialsHelper(registryToLookup, dockerConfig.get("credsStore").getAsString());
                }
            }

            if (dockerConfig.has("auths")) {
                return extractAuthConfigFromAuths(registryToLookup, dockerConfig.getAsJsonObject("auths"));
            }
        } catch (IOException exception) {

        }

        return null;
    }

    private Optional<JsonObject> readDockerConfig() {
        String dockerConfig = System.getenv("DOCKER_CONFIG");

        Optional<Reader> reader = dockerConfig == null
            ? getFileReaderFromDir(new File(getHomeDir(), ".docker/config.json"))
            : getFileReaderFromDir(new File(dockerConfig, "config.json"));
        return reader.map(r -> gson.fromJson(r, JsonObject.class));
    }

    private AuthConfig extractAuthConfigFromAuths(String registryToLookup, JsonObject auths) {
        JsonObject credentials = getCredentialsNode(auths, registryToLookup);
        if (credentials == null || !credentials.has("auth")) {
            return null;
        }
        String auth = credentials.get("auth").getAsString();
        String email = credentials.has("email") ? credentials.get("email").getAsString() : null;
        return AuthConfig.fromCredentialsEncoded(auth, email);
    }

    private AuthConfig extractAuthConfigFromCredentialsHelper(String registryToLookup, String credConfig) throws IOException {
        CredentialHelperClient credentialHelper = new CredentialHelperClient(log, credConfig);
        log.debug("AuthConfig: credentials from credential helper/store %s version %s",
                  credentialHelper.getName(),
                  credentialHelper.getVersion());
        return credentialHelper.getAuthConfig(registryToLookup);
    }

    private JsonObject getCredentialsNode(JsonObject auths, String registryToLookup) {
        if (auths.has(registryToLookup)) {
            return auths.getAsJsonObject(registryToLookup);
        }
        String registryWithScheme = Optional.of(registryToLookup)
                                            .filter(r -> r.toLowerCase().startsWith("http"))
                                            .orElse("https://" + registryToLookup);
        if (auths.has(registryWithScheme)) {
            return auths.getAsJsonObject(registryWithScheme);
        }
        return null;
    }

    private Optional<Reader> getFileReaderFromDir(File file) {
        try {
            return Optional.of(new FileReader(file));
        } catch (FileNotFoundException e) {
            return Optional.empty();
        }
    }

    private File getHomeDir() {
        String homeDir = Optional.ofNullable(System.getProperty("user.home")).orElse(System.getenv("HOME"));
        return new File(homeDir);
    }


}
