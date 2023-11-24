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
package org.eclipse.jkube.kit.build.service.docker.access.hc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.eclipse.jkube.kit.build.api.auth.AuthConfig;
import org.eclipse.jkube.kit.build.service.docker.access.BuildOptions;
import org.eclipse.jkube.kit.build.service.docker.access.CreateImageOptions;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccess;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccessException;
import org.eclipse.jkube.kit.build.service.docker.access.UrlBuilder;
import org.eclipse.jkube.kit.build.service.docker.access.chunked.BuildJsonResponseHandler;
import org.eclipse.jkube.kit.build.service.docker.access.chunked.PullOrPushResponseJsonHandler;
import org.eclipse.jkube.kit.build.service.docker.access.hc.http.HttpClientBuilder;
import org.eclipse.jkube.kit.build.service.docker.access.hc.unix.UnixSocketClientBuilder;
import org.eclipse.jkube.kit.build.service.docker.access.hc.util.ClientBuilder;
import org.eclipse.jkube.kit.build.service.docker.access.hc.win.NamedPipeClientBuilder;
import org.eclipse.jkube.kit.common.JsonFactory;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.ImageName;
import org.eclipse.jkube.kit.common.archive.ArchiveCompression;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;

import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;

/**
 * Implementation using <a href="http://hc.apache.org/">Apache HttpComponents</a>
 * for remotely accessing the docker host.
 * <p>
 * The design goal here is to provide only the functionality required for this plugin in order to
 * make it as robust as possible against docker API changes (which happen quite frequently). That's
 * also the reason, why no framework like JAX-RS or docker-java is used so that the dependencies are
 * kept low.
 * </p>
 * Of course, it's a bit more manual work, but it's worth the effort
 * (as long as the Docker API functionality required is not too much).
 *
 * @author roland
 * @since 26.03.14
 */
public class DockerAccessWithHcClient implements DockerAccess {

    // Base URL which is given through when using UnixSocket communication but is not really used
    private static final String UNIX_URL = "unix://127.0.0.1:1/";

    // Base URL which is given through when using NamedPipe communication but is not really used
    private static final String NPIPE_URL = "npipe://127.0.0.1:1/";

    // Minimal API version, independent of any feature used
    public static final String API_VERSION = "1.18";

    // Logging
    private final KitLogger log;

    private final ApacheHttpClientDelegate delegate;
    private final UrlBuilder urlBuilder;

    /**
     * Create a new access for the given URL
     *
     * @param baseUrl  base URL for accessing the docker Daemon
     * @param certPath used to build up a keystore with the given keys and certificates found in this
     *                 directory
     * @param maxConnections maximum parallel connections allowed to docker daemon (if a pool is used)
     * @param log      a log handler for printing out logging information
     * @throws IOException in case of I/O exception
     */
    public DockerAccessWithHcClient(String baseUrl,
                                    String certPath,
                                    int maxConnections,
                                    KitLogger log) throws IOException {
        URI uri = URI.create(Objects.requireNonNull(baseUrl, "Docker daemon baseUrl is required"));
        if (uri.getScheme() == null) {
            throw new IllegalArgumentException("The docker access url '" + baseUrl + "' must contain a schema tcp://, unix:// or npipe://");
        }
        if (uri.getScheme().equalsIgnoreCase("unix")) {
            this.delegate = createHttpClient(new UnixSocketClientBuilder(uri.getPath(), maxConnections, log));
            baseUrl = UNIX_URL;
        } else if (uri.getScheme().equalsIgnoreCase("npipe")) {
            this.delegate = createHttpClient(new NamedPipeClientBuilder(uri.getPath(), maxConnections, log), false);
            baseUrl = NPIPE_URL;
        } else {
            this.delegate = createHttpClient(new HttpClientBuilder(isSSL(baseUrl) ? certPath : null, maxConnections));
        }

        // Strip trailing slashes if any
        while(baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        this.urlBuilder = new UrlBuilder(baseUrl, "v" + fetchApiVersionFromServer(baseUrl, this.delegate));
        this.log = log;
    }

    @Override
    public void buildImage(String image, File dockerArchive, BuildOptions options) throws DockerAccessException {
        try {
            String url = urlBuilder.buildImage(image, options);
            delegate.post(url, dockerArchive, createBuildResponseHandler(), HTTP_OK);
        } catch (IOException e) {
            throw new DockerAccessException(e, "Unable to build image [%s]", image);
        }
    }

    @Override
    public boolean hasImage(String name) throws DockerAccessException {
        String url = urlBuilder.inspectImage(name);
        try {
            return delegate.get(url, new ApacheHttpClientDelegate.StatusCodeResponseHandler(), HTTP_OK, HTTP_NOT_FOUND) == HTTP_OK;
        } catch (IOException e) {
            throw new DockerAccessException(e, "Unable to check image [%s]", name);
        }
    }

    @Override
    public String getImageId(String name) throws DockerAccessException {
        ApacheHttpClientDelegate.HttpBodyAndStatus response = inspectImage(name);
        if (response.getStatusCode() == HTTP_NOT_FOUND) {
            return null;
        }
        JsonObject imageDetails = JsonFactory.newJsonObject(response.getBody());
        return imageDetails.get("Id").getAsString().substring(0, 12);
    }

    private ApacheHttpClientDelegate.HttpBodyAndStatus inspectImage(String name) throws DockerAccessException {
        String url = urlBuilder.inspectImage(name);
        try {
            return delegate.get(url, new ApacheHttpClientDelegate.BodyAndStatusResponseHandler(), HTTP_OK, HTTP_NOT_FOUND);
        } catch (IOException e) {
            throw new DockerAccessException(e, "Unable to inspect image [%s]", name);
        }
    }

    @Override
    public void loadImage(String image, File tarArchive) throws DockerAccessException {
        String url = urlBuilder.loadImage();

        try {
            delegate.post(url, tarArchive, new ApacheHttpClientDelegate.BodyAndStatusResponseHandler(), HTTP_OK);
        } catch (IOException e) {
            throw new DockerAccessException(e, "Unable to load %s", tarArchive);
        }
    }

    @Override
    public void pullImage(String image, AuthConfig authConfig, String registry, CreateImageOptions options)
            throws DockerAccessException {
        String pullUrl = urlBuilder.pullImage(options);
        try {
            delegate.post(pullUrl, null, createAuthHeader(authConfig),
                    createPullOrPushResponseHandler(), HTTP_OK);
        } catch (IOException e) {
            throw new DockerAccessException(e, "Unable to pull '%s'%s", image, (registry != null) ? " from registry '" + registry + "'" : "");
        }
    }

    @Override
    public void pushImage(String image, AuthConfig authConfig, String registry, int retries)
            throws DockerAccessException {
        ImageName name = new ImageName(image);
        String pushUrl = urlBuilder.pushImage(name, registry);
        TemporaryImageHandler temporaryImageHandler = tagTemporaryImage(name, registry);
        DockerAccessException dae = null;
        try {
            doPushImage(pushUrl, createAuthHeader(authConfig), createPullOrPushResponseHandler(), HTTP_OK, retries);
        } catch (IOException e) {
            dae = new DockerAccessException(e, "Unable to push '%s'%s", image, (registry != null) ? " to registry '" + registry + "'" : "");
            throw dae;
        } finally {
            temporaryImageHandler.handle(dae);
        }
    }

    @Override
    public void saveImage(String image, String filename, ArchiveCompression compression) throws DockerAccessException {
        ImageName name = new ImageName(image);
        String url = urlBuilder.getImage(name);
        try {
            delegate.get(url, getImageResponseHandler(filename, compression), HTTP_OK);
        } catch (IOException e) {
            throw new DockerAccessException(e, "Unable to save '%s' to '%s'", image, filename);
        }

    }

    private ResponseHandler<Object> getImageResponseHandler(final String filename, final ArchiveCompression compression) {
        return response -> {
            try (InputStream stream = response.getEntity().getContent();
                 OutputStream out = compression.wrapOutputStream(new FileOutputStream(filename))) {
                IOUtils.copy(stream, out, 65536);
            }
            return null;
        };
    }

    @Override
    public void tag(String sourceImage, String targetImage, boolean force)
            throws DockerAccessException {
        ImageName source = new ImageName(sourceImage);
        ImageName target = new ImageName(targetImage);
        try {
            String url = urlBuilder.tagContainer(source, target, force);
            delegate.post(url, HTTP_CREATED);
        } catch (IOException e) {
            throw new DockerAccessException(e, "Unable to add tag [%s] to image [%s]", targetImage,
                    sourceImage, e);
        }
    }

    @Override
    public boolean removeImage(String image, boolean... forceOpt) throws DockerAccessException {
        boolean force = forceOpt != null && forceOpt.length > 0 && forceOpt[0];
        try {
            String url = urlBuilder.deleteImage(image, force);
            ApacheHttpClientDelegate.HttpBodyAndStatus response = delegate.delete(url, new ApacheHttpClientDelegate.BodyAndStatusResponseHandler(), HTTP_OK, HTTP_NOT_FOUND);
            if (log.isDebugEnabled()) {
                logRemoveResponse(JsonFactory.newJsonArray(response.getBody()));
            }

            return response.getStatusCode() == HTTP_OK;
        } catch (IOException e) {
            throw new DockerAccessException(e, "Unable to remove image [%s]", image);
        }
    }

    // ---------------
    // Lifecycle methods not needed here
    @Override
    public void start() {
    }

    @Override
    public void shutdown() {
        try {
            delegate.close();
        } catch (IOException exp) {
            log.error("Error while closing HTTP client: " + exp,exp);
        }
    }

    ApacheHttpClientDelegate createHttpClient(ClientBuilder builder) throws IOException {
        return createHttpClient(builder, true);
    }

    ApacheHttpClientDelegate createHttpClient(ClientBuilder builder, boolean pooled) throws IOException {
        return new ApacheHttpClientDelegate(builder, pooled);
    }

    // visible for testing?
    private HcChunkedResponseHandlerWrapper createBuildResponseHandler() {
        return new HcChunkedResponseHandlerWrapper(new BuildJsonResponseHandler(log));
    }

    // visible for testing?
    private HcChunkedResponseHandlerWrapper createPullOrPushResponseHandler() {
        return new HcChunkedResponseHandlerWrapper(new PullOrPushResponseJsonHandler(log));
    }

    private Map<String, String> createAuthHeader(AuthConfig authConfig) {
        if (authConfig == null) {
            authConfig = AuthConfig.EMPTY_AUTH_CONFIG;
        }
        return Collections.singletonMap("X-Registry-Auth", authConfig.toHeaderValue(log));
    }

    private boolean isRetryableErrorCode(int errorCode) {
        // there eventually could be more then one of this
        return errorCode == HTTP_INTERNAL_ERROR;
    }

    private void doPushImage(String url, Map<String, String> header, HcChunkedResponseHandlerWrapper handler, int status,
                             int retries) throws IOException {
        // 0: The original attemp, 1..retry: possible retries.
        for (int i = 0; i <= retries; i++) {
            try {
                delegate.post(url, null, header, handler, HTTP_OK);
                return;
            } catch (HttpResponseException e) {
                if (isRetryableErrorCode(e.getStatusCode()) && i != retries) {
                    log.warn("failed to push image to [{}], retrying...", url);
                } else {
                    throw e;
                }
            }
        }
    }

    private TemporaryImageHandler tagTemporaryImage(ImageName name, String registry) throws DockerAccessException {
        String targetImage = name.getFullName(registry);
        String fullName = name.getFullName();
        if ((name.isFullyQualifiedName() && name.hasRegistry()) || targetImage.equals(fullName) || registry == null) {
            return () ->
                log.info("Temporary image tag skipped. Target image '%s' already has registry set or no registry is available",
                    targetImage);
        }

        boolean alreadyHasImage = hasImage(targetImage);
        if (alreadyHasImage) {
            log.warn("Target image '%s' already exists. Tagging of '%s' will replace existing image",
                targetImage, fullName);
        }

        tag(fullName, targetImage, false);
        return alreadyHasImage ?
            () -> log.info("Tagged image '%s' won't be removed after tagging as it already existed", targetImage) :
            new RemovingTemporaryImageHandler(targetImage);
    }

    // ===========================================================================================================

    // Callback for processing response chunks
    private void logRemoveResponse(JsonArray logElements) {
        for (int i = 0; i < logElements.size(); i++) {
            JsonObject entry = logElements.get(i).getAsJsonObject();
            for (Object key : entry.keySet()) {
                log.debug("%s: %s", key, entry.get(key.toString()));
            }
        }
    }

    private static boolean isSSL(String url) {
        return url != null && url.toLowerCase().startsWith("https");
    }

    public String fetchApiVersionFromServer(String baseUrl, ApacheHttpClientDelegate delegate) throws IOException {
        HttpGet get = new HttpGet(baseUrl + (baseUrl.endsWith("/") ? "" : "/") + "version");
        get.addHeader(HttpHeaders.ACCEPT, "*/*");
        get.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        try (CloseableHttpResponse response = delegate.getHttpClient().execute(get)) {

            return response.getFirstHeader("Api-Version") != null ? response.getFirstHeader("Api-Version").getValue() : API_VERSION;
        }
    }

    @FunctionalInterface
    private interface TemporaryImageHandler {
        void handle() throws DockerAccessException;

        default void handle(DockerAccessException interruptingError) throws DockerAccessException {
            handle();

            if (interruptingError == null) {
                return;
            }
            throw interruptingError;
        }
    }

    private final class RemovingTemporaryImageHandler implements TemporaryImageHandler {
        private final String targetImage;

        private RemovingTemporaryImageHandler(String targetImage) {
            this.targetImage = targetImage;
        }

        @Override
        public void handle() throws DockerAccessException {
            boolean imageRemoved = removeImage(targetImage, true);
            if (imageRemoved) {
                return;
            }
            throw new DockerAccessException(
                "Image %s could be pushed, but the temporary tag could not be removed",
                targetImage
            );
        }
    }
}
