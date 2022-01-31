/**
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
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.eclipse.jkube.kit.build.api.model.Container;
import org.eclipse.jkube.kit.build.api.model.ContainerDetails;
import org.eclipse.jkube.kit.build.api.model.ContainersListElement;
import org.eclipse.jkube.kit.build.api.model.ExecDetails;
import org.eclipse.jkube.kit.build.api.model.Network;
import org.eclipse.jkube.kit.build.api.model.NetworkCreateConfig;
import org.eclipse.jkube.kit.build.api.model.NetworksListElement;
import org.eclipse.jkube.kit.build.api.model.VolumeCreateConfig;
import org.eclipse.jkube.kit.build.api.auth.AuthConfig;
import org.eclipse.jkube.kit.build.service.docker.access.BuildOptions;
import org.eclipse.jkube.kit.build.service.docker.access.ContainerCreateConfig;
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
import org.eclipse.jkube.kit.build.service.docker.access.log.DefaultLogCallback;
import org.eclipse.jkube.kit.build.service.docker.access.log.LogCallback;
import org.eclipse.jkube.kit.build.service.docker.access.log.LogGetHandle;
import org.eclipse.jkube.kit.build.service.docker.access.log.LogOutputSpec;
import org.eclipse.jkube.kit.build.service.docker.access.log.LogRequestor;
import org.eclipse.jkube.kit.build.service.docker.helper.Timestamp;
import org.eclipse.jkube.kit.common.JsonFactory;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.EnvUtil;
import org.eclipse.jkube.kit.config.image.ImageName;
import org.eclipse.jkube.kit.common.archive.ArchiveCompression;
import org.eclipse.jkube.kit.config.image.build.Arguments;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;

import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_NOT_MODIFIED;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
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

    /** {@inheritDoc} */
    @Override
    public String getServerApiVersion() throws DockerAccessException {
        try {
            String url = urlBuilder.version();
            String response = delegate.get(url, 200);
            JsonObject info = JsonFactory.newJsonObject(response);
            return info.get("ApiVersion").getAsString();
        } catch (Exception e) {
            throw new DockerAccessException(e, "Cannot extract API version from server %s", urlBuilder.getBaseUrl());
        }
    }

    @Override
    public void startExecContainer(String containerId, LogOutputSpec outputSpec) throws DockerAccessException {
        try {
            String url = urlBuilder.startExecContainer(containerId);
            JsonObject request = new JsonObject();
            request.addProperty("Detach", false);
            request.addProperty("Tty", true);

            delegate.post(url, request.toString(), createExecResponseHandler(outputSpec), HTTP_OK);
        } catch (Exception e) {
            throw new DockerAccessException(e, "Unable to start container id [%s]", containerId);
        }
    }

    private ResponseHandler<Object> createExecResponseHandler(LogOutputSpec outputSpec) {
        final LogCallback callback = new DefaultLogCallback(outputSpec);
        return response -> {
            try (InputStream stream = response.getEntity().getContent();
                 LineNumberReader reader = new LineNumberReader(new InputStreamReader(stream))) {
                String line;
                try {
                    callback.open();
                    while ( (line = reader.readLine()) != null) {
                        callback.log(1, new Timestamp(), line);
                    }
                } catch (LogCallback.DoneException e) {
                    // Ok, we stop here ...
                } finally {
                    callback.close();
                }
            }
            return null;
        };
    }

    @Override
    public String createExecContainer(String containerId, Arguments arguments) throws DockerAccessException {
        String url = urlBuilder.createExecContainer(containerId);
        JsonObject request = new JsonObject();
        request.addProperty("Tty", true);
        request.addProperty("AttachStdin", false);
        request.addProperty("AttachStdout", true);
        request.addProperty("AttachStderr", true);
        request.add("Cmd", JsonFactory.newJsonArray(arguments.getExec()));

        String execJsonRequest = request.toString();
        try {
            String response = delegate.post(url, execJsonRequest, new ApacheHttpClientDelegate.BodyResponseHandler(), HTTP_CREATED);
            JsonObject json = JsonFactory.newJsonObject(response);
            if (json.has("Warnings")) {
                logWarnings(json);
            }

            return json.get("Id").getAsString();
        } catch (IOException e) {
            throw new DockerAccessException(e, "Unable to exec [%s] on container [%s]", request.toString(),
                    containerId);
        }

    }

    @Override
    public String createContainer(ContainerCreateConfig containerConfig, String containerName)
            throws DockerAccessException {
        String createJson = containerConfig.toJson();
        log.debug("Container create config: %s", createJson);

        try {
            String url = urlBuilder.createContainer(containerName);
            String response =
                    delegate.post(url, createJson, new ApacheHttpClientDelegate.BodyResponseHandler(), HTTP_CREATED);
            JsonObject json = JsonFactory.newJsonObject(response);
            logWarnings(json);

            // only need first 12 to id a container
            return json.get("Id").getAsString().substring(0, 12);
        } catch (IOException e) {
            throw new DockerAccessException(e, "Unable to create container for [%s]",
                    containerConfig.getImageName());
        }
    }

    @Override
    public void startContainer(String containerId) throws DockerAccessException {
        try {
            String url = urlBuilder.startContainer(containerId);
            delegate.post(url, HTTP_NO_CONTENT, HTTP_OK);
        } catch (IOException e) {
            throw new DockerAccessException(e, "Unable to start container id [%s]", containerId);
        }
    }

    @Override
    public void stopContainer(String containerId, int killWait) throws DockerAccessException {
        try {
            String url = urlBuilder.stopContainer(containerId, killWait);
            delegate.post(url, HTTP_NO_CONTENT, HTTP_NOT_MODIFIED);
        } catch (IOException e) {
            throw new DockerAccessException(e, "Unable to stop container id [%s]", containerId);
        }
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
    public void copyArchive(String containerId, File archive, String targetPath)
            throws DockerAccessException {
        try {
            String url = urlBuilder.copyArchive(containerId, targetPath);
            delegate.put(url, archive, HTTP_OK);
        } catch (IOException e) {
            throw new DockerAccessException(e, "Unable to copy archive %s to container [%s] with path %s",
                    archive.toPath(), containerId, targetPath);
        }
    }

    @Override
    public void getLogSync(String containerId, LogCallback callback) {
        LogRequestor extractor = new LogRequestor(delegate.getHttpClient(), urlBuilder, containerId, callback);
        extractor.fetchLogs();
    }

    @Override
    public LogGetHandle getLogAsync(String containerId, LogCallback callback) {
        LogRequestor extractor = new LogRequestor(delegate.createBasicClient(), urlBuilder, containerId, callback);
        extractor.start();
        return extractor;
    }

    @Override
    public List<Container> getContainersForImage(String image, boolean all) throws DockerAccessException {
        String url;
        String serverApiVersion = getServerApiVersion();
        if (EnvUtil.greaterOrEqualsVersion(serverApiVersion, "1.23")) {
            // For Docker >= 1.11 we can use a new filter when listing containers
            url = urlBuilder.listContainers(all, "ancestor",image);
        } else {
            // For older versions (< Docker 1.11) we need to iterate over the containers.
            url = urlBuilder.listContainers(all);
        }

        try {
            String response = delegate.get(url, HTTP_OK);
            JsonArray array = JsonFactory.newJsonArray(response);
            List<Container> containers = new ArrayList<>();

            for (int i = 0; i < array.size(); i++) {
                JsonObject element = array.get(i).getAsJsonObject();
                if (image.equals(element.get("Image").getAsString())) {
                    containers.add(new ContainersListElement(element));
                }
            }
            return containers;
        } catch (IOException e) {
            throw new DockerAccessException(e.getMessage());
        }
    }

    @Override
    public ContainerDetails getContainer(String containerIdOrName) throws DockerAccessException {
        ApacheHttpClientDelegate.HttpBodyAndStatus response = inspectContainer(containerIdOrName);
        if (response.getStatusCode() == HTTP_NOT_FOUND) {
            return null;
        } else {
            return new ContainerDetails(JsonFactory.newJsonObject(response.getBody()));
        }
    }

    @Override
    public ExecDetails getExecContainer(String containerIdOrName) throws DockerAccessException {
        ApacheHttpClientDelegate.HttpBodyAndStatus response = inspectExecContainer(containerIdOrName);
        if (response.getStatusCode() == HTTP_NOT_FOUND) {
            return null;
        } else {
            return new ExecDetails(JsonFactory.newJsonObject(response.getBody()));
        }
    }

    private ApacheHttpClientDelegate.HttpBodyAndStatus inspectContainer(String containerIdOrName) throws DockerAccessException {
        try {
            String url = urlBuilder.inspectContainer(containerIdOrName);
            return delegate.get(url, new ApacheHttpClientDelegate.BodyAndStatusResponseHandler(), HTTP_OK, HTTP_NOT_FOUND);
        } catch (IOException e) {
            throw new DockerAccessException(e, "Unable to retrieve container name for [%s]", containerIdOrName);
        }
    }

    private ApacheHttpClientDelegate.HttpBodyAndStatus inspectExecContainer(String containerIdOrName) throws DockerAccessException {
        try {
            String url = urlBuilder.inspectExecContainer(containerIdOrName);
            return delegate.get(url, new ApacheHttpClientDelegate.BodyAndStatusResponseHandler(), HTTP_OK, HTTP_NOT_FOUND);
        } catch (IOException e) {
            throw new DockerAccessException(e, "Unable to retrieve container name for [%s]", containerIdOrName);
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
    public void removeContainer(String containerId, boolean removeVolumes)
            throws DockerAccessException {
        try {
            String url = urlBuilder.removeContainer(containerId, removeVolumes);
            delegate.delete(url, HTTP_NO_CONTENT);
        } catch (IOException e) {
            throw new DockerAccessException(e, "Unable to remove container [%s]", containerId);
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
        String temporaryImage = tagTemporaryImage(name, registry);
        DockerAccessException dae = null;
        try {
            doPushImage(pushUrl, createAuthHeader(authConfig), createPullOrPushResponseHandler(), HTTP_OK, retries);
        } catch (IOException e) {
            dae = new DockerAccessException(e, "Unable to push '%s'%s", image, (registry != null) ? " to registry '" + registry + "'" : "");
        }
        if (temporaryImage != null && !removeImage(temporaryImage, true)) {
            if (dae == null) {
                throw new DockerAccessException("Image %s could be pushed, but the temporary tag could not be removed", temporaryImage);
            } else {
                throw new DockerAccessException(dae.getCause(), dae.getMessage() + " and also temporary tag [%s] could not be removed, too.", temporaryImage);
            }
        } else if (dae != null) {
            throw dae;
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

    @Override
    public List<Network> listNetworks() throws DockerAccessException {
        String url = urlBuilder.listNetworks();

        try {
            String response = delegate.get(url, HTTP_OK);
            JsonArray array = JsonFactory.newJsonArray(response);
            List<Network> networks = new ArrayList<>(array.size());

            for (int i = 0; i < array.size(); i++) {
                networks.add(new NetworksListElement(array.get(i).getAsJsonObject()));
            }

            return networks;
        } catch (IOException e) {
            throw new DockerAccessException(e.getMessage());
        }
    }

    @Override
    public String createNetwork(NetworkCreateConfig networkConfig)
            throws DockerAccessException {
        String createJson = networkConfig.toJson();
        log.debug("Network create config: " + createJson);
        try {
            String url = urlBuilder.createNetwork();
            String response =
                    delegate.post(url, createJson, new ApacheHttpClientDelegate.BodyResponseHandler(), HTTP_CREATED);
            log.debug(response);
            JsonObject json = JsonFactory.newJsonObject(response);
            if (json.has("Warnings")) {
                logWarnings(json);
            }

            // only need first 12 to id a container
            return json.get("Id").getAsString().substring(0, 12);
        } catch (IOException e) {
            throw new DockerAccessException(e, "Unable to create network for [%s]",
                    networkConfig.getName());
        }
    }

    @Override
    public boolean removeNetwork(String networkId)
            throws DockerAccessException {
        try {
            String url = urlBuilder.removeNetwork(networkId);
            int status = delegate.delete(url, HTTP_OK, HTTP_NO_CONTENT, HTTP_NOT_FOUND);
            return status == HTTP_OK || status == HTTP_NO_CONTENT;
        } catch (IOException e) {
            throw new DockerAccessException(e, "Unable to remove network [%s]", networkId);
        }
    }

    @Override
    public String createVolume(VolumeCreateConfig containerConfig)
            throws DockerAccessException
    {
        String createJson = containerConfig.toJson();
        log.debug("Volume create config: %s", createJson);

        try
        {
            String url = urlBuilder.createVolume();
            String response =
                    delegate.post(url,
                            createJson,
                            new ApacheHttpClientDelegate.BodyResponseHandler(),
                            HTTP_CREATED);
            JsonObject json = JsonFactory.newJsonObject(response);
            logWarnings(json);

            return json.get("Name").getAsString();
        }
        catch (IOException e)
        {
            throw new DockerAccessException(e, "Unable to create volume for [%s]",
                    containerConfig.getName());
        }
    }

    @Override
    public void removeVolume(String name) throws DockerAccessException {
        try {
            String url = urlBuilder.removeVolume(name);
            delegate.delete(url, HTTP_NO_CONTENT, HTTP_NOT_FOUND);
        } catch (IOException e) {
            throw new DockerAccessException(e, "Unable to remove volume [%s]", name);
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

    private String tagTemporaryImage(ImageName name, String registry) throws DockerAccessException {
        String targetImage = name.getFullName(registry);
        if (!name.hasRegistry() && registry != null) {
            if (hasImage(targetImage)) {
                throw new DockerAccessException(
                        String.format("Cannot temporarily tag %s with %s because target image already exists. " +
                                        "Please remove this and retry.",
                                name.getFullName(), targetImage));
            }
            tag(name.getFullName(), targetImage, false);
            return targetImage;
        }
        return null;
    }

    // ===========================================================================================================

    private void logWarnings(JsonObject body) {
        if (body.has("Warnings")) {
            JsonElement warningsObj = body.get("Warnings");
            if (!warningsObj.isJsonNull()) {
                JsonArray warnings = (JsonArray) warningsObj;
                for (int i = 0; i < warnings.size(); i++) {
                    log.warn(warnings.get(i).getAsString());
                }
            }
        }
    }

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
}
