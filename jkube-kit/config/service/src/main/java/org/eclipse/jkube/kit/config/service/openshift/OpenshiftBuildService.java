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
package org.eclipse.jkube.kit.config.service.openshift;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.api.model.ImageStreamTag;
import org.eclipse.jkube.kit.build.api.auth.AuthConfig;
import org.eclipse.jkube.kit.build.service.docker.auth.AuthConfigFactory;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.ResourceFileType;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.common.util.OpenshiftHelper;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.ImageName;
import org.eclipse.jkube.kit.common.RegistryConfig;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.AbstractImageBuildService;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectReference;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import io.fabric8.openshift.api.model.Build;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.BuildConfigBuilder;
import io.fabric8.openshift.api.model.BuildConfigSpec;
import io.fabric8.openshift.api.model.BuildConfigSpecBuilder;
import io.fabric8.openshift.api.model.BuildOutput;
import io.fabric8.openshift.api.model.BuildSource;
import io.fabric8.openshift.api.model.BuildStrategy;
import io.fabric8.openshift.api.model.ImageStreamBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import org.apache.commons.codec.binary.Base64;

import static org.eclipse.jkube.kit.build.api.helper.BuildUtil.extractBaseFromConfiguration;
import static org.eclipse.jkube.kit.build.api.helper.BuildUtil.extractBaseFromDockerfile;

import static org.eclipse.jkube.kit.common.util.OpenshiftHelper.isOpenShift;
import static org.eclipse.jkube.kit.build.api.helper.RegistryUtil.getApplicablePullRegistryFrom;
import static org.eclipse.jkube.kit.build.api.helper.RegistryUtil.getApplicablePushRegistryFrom;
import static org.eclipse.jkube.kit.config.service.openshift.ImageStreamService.resolveImageStreamName;
import static org.eclipse.jkube.kit.config.service.openshift.ImageStreamService.resolveImageStreamTagName;
import static org.eclipse.jkube.kit.config.service.openshift.OpenShiftBuildServiceUtils.computeS2IBuildName;
import static org.eclipse.jkube.kit.config.service.openshift.OpenShiftBuildServiceUtils.createAdditionalTagsIfPresent;
import static org.eclipse.jkube.kit.config.service.openshift.OpenShiftBuildServiceUtils.createBuildArchive;
import static org.eclipse.jkube.kit.config.service.openshift.OpenShiftBuildServiceUtils.createBuildOutput;
import static org.eclipse.jkube.kit.config.service.openshift.OpenShiftBuildServiceUtils.createBuildStrategy;
import static org.eclipse.jkube.kit.config.service.openshift.OpenShiftBuildServiceUtils.getAdditionalTagsToCreate;

/**
 * @author nicola
 */
public class OpenshiftBuildService extends AbstractImageBuildService {

    protected static final String DEFAULT_S2I_BUILD_SUFFIX = "-s2i";
    public static final String DEFAULT_S2I_SOURCE_TYPE = "Binary";
    protected static final String IMAGE_STREAM_TAG =  "ImageStreamTag";
    protected static final String DOCKER_IMAGE =  "DockerImage";
    protected static final String DEFAULT_BUILD_OUTPUT_KIND = IMAGE_STREAM_TAG;
    public static final String REQUESTS = "requests";
    public static final String LIMITS = "limits";

    private final JKubeServiceHub jKubeServiceHub;
    private final KitLogger log;
    private final BuildServiceConfig buildServiceConfig;
    private final JKubeConfiguration jKubeConfiguration;
    private OpenShiftClient client;
    private String applicableOpenShiftNamespace;

    public OpenshiftBuildService(JKubeServiceHub jKubeServiceHub) {
        super(jKubeServiceHub);
        this.jKubeServiceHub = Objects.requireNonNull(jKubeServiceHub, "JKube Service Hub is required");
        this.log = Objects.requireNonNull(jKubeServiceHub.getLog(), "Log is required");
        this.buildServiceConfig = Objects.requireNonNull(jKubeServiceHub.getBuildServiceConfig(),
            "BuildServiceConfig is required");
        this.jKubeConfiguration = Objects.requireNonNull(jKubeServiceHub.getConfiguration(),
            "JKubeConfiguration is required");
        Objects.requireNonNull(jKubeServiceHub.getDockerServiceHub(), "Docker Service Hub is required");
        Objects.requireNonNull(jKubeServiceHub.getDockerServiceHub().getArchiveService(),
            "Docker Archive Service is required");
    }

    @Override
    public boolean isApplicable() {
        return jKubeServiceHub.getRuntimeMode() == RuntimeMode.OPENSHIFT;
    }

    @Override
    public void buildSingleImage(ImageConfiguration imageConfig) throws JKubeServiceException {
        initClient();
        String buildName = null;
        try {
            final ImageConfiguration applicableImageConfig = getApplicableImageConfiguration(imageConfig, jKubeConfiguration.getRegistryConfig());
            ImageName imageName = new ImageName(applicableImageConfig.getName());

            File dockerTar = createBuildArchive(jKubeServiceHub, applicableImageConfig);

            KubernetesListBuilder builder = new KubernetesListBuilder();

            // Check for buildconfig / imagestream / pullSecret and create them if necessary
            String openshiftPullSecret = buildServiceConfig.getOpenshiftPullSecret();
            final boolean usePullSecret = checkOrCreatePullSecret(client, builder, openshiftPullSecret, applicableImageConfig);
            if (usePullSecret) {
                buildName = updateOrCreateBuildConfig(buildServiceConfig, client, builder, applicableImageConfig, openshiftPullSecret);
            } else {
                buildName = updateOrCreateBuildConfig(buildServiceConfig, client, builder, applicableImageConfig, null);
            }

            if (buildServiceConfig.getBuildOutputKind() == null || IMAGE_STREAM_TAG.equals(buildServiceConfig.getBuildOutputKind())) {
                checkOrCreateImageStream(buildServiceConfig, client, builder, resolveImageStreamName(imageName));

                applyBuild(buildName, dockerTar, builder);

                // Create a file with generated image streams
                addImageStreamToFile(getImageStreamFile(), imageName, client);

                createAdditionalTags(imageConfig, imageName);
            } else {
                applyBuild(buildName, dockerTar, builder);
            }

        } catch (JKubeServiceException e) {
            throw e;
        } catch (Exception ex) {
            // Log additional details in case of any IOException
            if (ex.getCause() instanceof IOException) {
                log.error("Build for %s failed: %s", buildName, ex.getCause().getMessage());
                logBuildFailure(client, buildName);
            } else {
                throw new JKubeServiceException("Unable to build the image using the OpenShift build service", ex);
            }
        }
    }

    @Override
    protected void pushSingleImage(ImageConfiguration imageConfiguration, int retries, RegistryConfig registryConfig, boolean skipTag) {
        // Do nothing. Image is pushed as part of build phase
        log.warn("Image is pushed to OpenShift's internal registry during oc:build goal. Skipping...");
    }

    private void applyBuild(String buildName, File dockerTar, KubernetesListBuilder builder)
            throws Exception {
        applyResourceObjects(buildServiceConfig, client, builder);

        // Start the actual build
        Build build = startBuild(client, dockerTar, buildName);

        // Wait until the build finishes
        waitForOpenShiftBuildToComplete(client, build);
    }

    private File getImageStreamFile() {
        return ResourceFileType.yaml.addExtensionIfMissing(new File(buildServiceConfig.getBuildDirectory(),
            String.format("%s-is", jKubeConfiguration.getProject().getArtifactId())));
    }

    @Override
    public void postProcess() {
        buildServiceConfig.attachArtifact("is", getImageStreamFile());
    }

    protected String updateOrCreateBuildConfig(BuildServiceConfig config, OpenShiftClient client, KubernetesListBuilder builder, ImageConfiguration imageConfig, String openshiftPullSecret) {
        ImageName imageName = new ImageName(imageConfig.getName());
        String buildName = computeS2IBuildName(config, imageName);

        BuildStrategy buildStrategyResource = createBuildStrategy(jKubeServiceHub, imageConfig, openshiftPullSecret);
        BuildOutput buildOutput = createBuildOutput(config, imageName);

        // Fetch existing build config
        BuildConfig buildConfig = client.buildConfigs().inNamespace(applicableOpenShiftNamespace).withName(buildName).get();
        if (buildConfig != null) {
            // lets verify the BC
            BuildConfigSpec spec = OpenShiftBuildServiceUtils.getBuildConfigSpec(buildConfig);
            validateSourceType(buildName, spec);

            if (config.getBuildRecreateMode().isBuildConfig()) {
                // Delete and recreate afresh
                client.buildConfigs().inNamespace(applicableOpenShiftNamespace).withName(buildName).delete();
                return createBuildConfig(builder, buildName, buildStrategyResource, buildOutput);
            } else {
                // Update & return
                return updateBuildConfig(client, buildName, buildStrategyResource, buildOutput, spec);
            }
        } else {
            // Create afresh
            return createBuildConfig(builder, buildName, buildStrategyResource, buildOutput);
        }
    }

    ImageConfiguration getApplicableImageConfiguration(ImageConfiguration imageConfig, RegistryConfig registryConfig) {
        final ImageConfiguration.ImageConfigurationBuilder applicableImageConfigBuilder = imageConfig.toBuilder();
        if (imageConfig.getBuildConfiguration() != null && !imageConfig.getBuildConfiguration().isDockerFileMode()
            && imageConfig.getBuildConfiguration().getAssembly() != null) {
            applicableImageConfigBuilder.build(imageConfig.getBuild().toBuilder().assembly(
                    imageConfig.getBuildConfiguration().getAssembly().getFlattenedClone(jKubeServiceHub.getConfiguration()))
                .build());
        }
        if (buildServiceConfig.getBuildOutputKind() != null && buildServiceConfig.getBuildOutputKind().equals(DOCKER_IMAGE)) {
            String applicableRegistry = getApplicablePushRegistryFrom(imageConfig, registryConfig);
            applicableImageConfigBuilder.name(new ImageName(imageConfig.getName()).getFullName(applicableRegistry));
        }
        return applicableImageConfigBuilder.build();
    }

    private void initClient() {
        KubernetesClient k8sClient = jKubeServiceHub.getClient();
        if (!isOpenShift(k8sClient)) {
            throw new IllegalStateException("OpenShift platform has been specified but OpenShift has not been detected!");
        }
        client = OpenshiftHelper.asOpenShiftClient(k8sClient);
        if (buildServiceConfig.getResourceConfig() != null && buildServiceConfig.getResourceConfig().getNamespace() != null) {
            applicableOpenShiftNamespace = buildServiceConfig.getResourceConfig().getNamespace();
        } else {
            applicableOpenShiftNamespace = jKubeServiceHub.getClusterAccess().getNamespace();
        }
    }

    private void validateSourceType(String buildName, BuildConfigSpec spec) {
        BuildSource source = spec.getSource();
        if (source != null) {
            String sourceType = source.getType();
            if (!Objects.equals(DEFAULT_S2I_SOURCE_TYPE, sourceType)) {
                log.warn("BuildServiceConfig %s is not of type: 'Binary' but is '%s' !", buildName, sourceType);
            }
        }
    }

    private BuildConfigSpec getBuildConfigSpec(BuildStrategy buildStrategyResource, BuildOutput buildOutput) {
        BuildConfigSpecBuilder specBuilder = null;

        // Check for BuildConfig resource fragment
        File buildConfigResourceFragment = KubernetesHelper.getResourceFragmentFromSource(buildServiceConfig.getResourceDir(), buildServiceConfig.getResourceConfig() != null ? buildServiceConfig.getResourceConfig().getRemotes() : null, "buildconfig.yml", log);
        if (buildConfigResourceFragment != null) {
            BuildConfig buildConfigFragment = client.buildConfigs().load(buildConfigResourceFragment).get();
            specBuilder = new BuildConfigSpecBuilder(buildConfigFragment.getSpec());
        } else {
            specBuilder = new BuildConfigSpecBuilder();
        }

        if (specBuilder.buildSource() == null) {
            specBuilder.withNewSource()
                    .withType(DEFAULT_S2I_SOURCE_TYPE)
                    .endSource();
        }

        if (specBuilder.buildStrategy() == null) {
            specBuilder.withStrategy(buildStrategyResource);
        }

        if (specBuilder.buildOutput() == null) {
            specBuilder.withOutput(buildOutput);
        }

        Map<String, Map<String, Quantity>> requestsLimitsMap = getRequestsAndLimits();
        if (requestsLimitsMap.containsKey(REQUESTS)) {
            specBuilder.editOrNewResources().addToRequests(requestsLimitsMap.get(REQUESTS)).endResources();
        }
        if (requestsLimitsMap.containsKey(LIMITS)) {
            specBuilder.editOrNewResources().addToLimits(requestsLimitsMap.get(LIMITS)).endResources();
        }
        return specBuilder.build();
    }

    private String createBuildConfig(KubernetesListBuilder builder, String buildName, BuildStrategy buildStrategyResource, BuildOutput buildOutput) {
        log.info("Creating BuildServiceConfig %s for %s build", buildName, buildStrategyResource.getType());
        builder.addToItems(new BuildConfigBuilder()
            .withNewMetadata()
            .withName(buildName)
            .endMetadata()
            .withSpec(getBuildConfigSpec(buildStrategyResource, buildOutput))
            .build()
        );
        return buildName;
    }

    private String updateBuildConfig(OpenShiftClient client, String buildName, BuildStrategy buildStrategy,
                                     BuildOutput buildOutput, BuildConfigSpec spec) {
        // lets check if the strategy or output has changed and if so lets update the BC
        // e.g. the S2I builder image or the output tag and
        if (!Objects.equals(buildStrategy, spec.getStrategy()) || !Objects.equals(buildOutput, spec.getOutput())) {
            client.buildConfigs().inNamespace(applicableOpenShiftNamespace).withName(buildName).edit(bc -> new BuildConfigBuilder(bc)
                    .editSpec()
                    .withStrategy(buildStrategy)
                    .withOutput(buildOutput)
                    .endSpec()
                    .build());
            log.info("Updating BuildServiceConfig %s for %s strategy", buildName, buildStrategy.getType());
        } else {
            log.info("Using BuildServiceConfig %s for %s strategy", buildName, buildStrategy.getType());
        }
        return buildName;
    }

    private boolean checkOrCreatePullSecret(OpenShiftClient client, KubernetesListBuilder builder, String pullSecretName, ImageConfiguration imageConfig)
            throws Exception {
        BuildConfiguration buildConfig = imageConfig.getBuildConfiguration();

        String fromImage;
        if (buildConfig.isDockerFileMode()) {
            fromImage = extractBaseFromDockerfile(jKubeConfiguration, buildConfig);
        } else {
            fromImage = extractBaseFromConfiguration(buildConfig);
        }

        String pullRegistry = getApplicablePullRegistryFrom(fromImage, jKubeConfiguration.getRegistryConfig());

        if (pullRegistry != null) {
            RegistryConfig registryConfig = jKubeConfiguration.getRegistryConfig();
            final AuthConfig authConfig = new AuthConfigFactory(log).createAuthConfig(false, registryConfig.isSkipExtendedAuth(), registryConfig.getAuthConfig(),
                    registryConfig.getSettings(), null, pullRegistry, registryConfig.getPasswordDecryptionMethod());

            final Secret secret = Optional.ofNullable(pullSecretName)
                .map(psn ->  client.secrets().inNamespace(applicableOpenShiftNamespace).withName(psn).get()).orElse(null);

            if (secret != null) {
                log.info("Adding to Secret %s", pullSecretName);
                return updateSecret(client, pullSecretName, secret.getData());
            }

            if (authConfig != null) {
                JsonObject auths = new JsonObject();
                JsonObject auth = new JsonObject();
                JsonObject item = new JsonObject();

                String authString = authConfig.getUsername() + ":" + authConfig.getPassword();
                item.add("auth", new JsonPrimitive(Base64.encodeBase64String(authString.getBytes(StandardCharsets.UTF_8))));
                auth.add(pullRegistry, item);
                auths.add("auths", auth);

                String credentials = Base64.encodeBase64String(auths.toString().getBytes(StandardCharsets.UTF_8));

                Map<String, String> data = new HashMap<>();
                data.put(".dockerconfigjson", credentials);

                log.info("Creating Secret");
                builder.addNewSecretItem()
                        .withNewMetadata()
                        .withName(pullSecretName)
                        .endMetadata()
                        .withData(data)
                        .withType("kubernetes.io/dockerconfigjson")
                        .endSecretItem();
                return true;
            }
        }
        return false;
    }

    private boolean updateSecret(OpenShiftClient client, String pullSecretName, Map<String, String> data) {
        if (!Objects.equals(data, client.secrets().withName(pullSecretName).get().getData())) {
            client.secrets().inNamespace(applicableOpenShiftNamespace).withName(pullSecretName).edit(s -> new SecretBuilder(s)
                    .editMetadata()
                    .withName(pullSecretName)
                    .endMetadata()
                    .withData(data)
                    .withType("kubernetes.io/dockerconfigjson")
                    .build());
            log.info("Updating Secret %s", pullSecretName);
        } else {
            log.info("Using Secret %s", pullSecretName);
        }
        return true;
    }

    private void checkOrCreateImageStream(BuildServiceConfig config, OpenShiftClient client, KubernetesListBuilder builder, String imageStreamName) {
        boolean hasImageStream = client.imageStreams().inNamespace(applicableOpenShiftNamespace).withName(imageStreamName).get() != null;
        if (hasImageStream && config.getBuildRecreateMode().isImageStream()) {
            client.imageStreams().inNamespace(applicableOpenShiftNamespace).withName(imageStreamName).delete();
            hasImageStream = false;
        }
        if (!hasImageStream) {
            log.info("Creating ImageStream %s", imageStreamName);
            builder.addToItems(new ImageStreamBuilder()
                .withNewMetadata()
                    .withName(imageStreamName)
                .endMetadata()
                .withNewSpec()
                    .withNewLookupPolicy()
                        .withLocal(config.isS2iImageStreamLookupPolicyLocal())
                    .endLookupPolicy()
                .endSpec()
                .build()
            );
        } else {
            log.info("Adding to ImageStream %s", imageStreamName);
        }
    }

    private void applyResourceObjects(BuildServiceConfig config, OpenShiftClient client, KubernetesListBuilder builder) throws Exception {
        if (config.getEnricherTask() != null) {
            config.getEnricherTask().execute(builder);
        }

        if (builder.hasItems()) {
            KubernetesList k8sList = builder.build();
            client.resourceList(k8sList).inNamespace(applicableOpenShiftNamespace).create();
        }
    }

    private Build startBuild(OpenShiftClient client, File dockerTar, String buildName) {
        log.info("Starting Build %s", buildName);
        try {
            return client.buildConfigs().inNamespace(applicableOpenShiftNamespace).withName(buildName)
                    .instantiateBinary()
                    .fromFile(dockerTar);
        } catch (KubernetesClientException exp) {
            Status status = exp.getStatus();
            if (status != null) {
                log.error("OpenShift Error: [%d %s] [%s] %s", status.getCode(), status.getStatus(), status.getReason(), status.getMessage());
            }
            if (exp.getCause() instanceof IOException && exp.getCause().getMessage().contains("Stream Closed")) {
                log.error("Build for %s failed: %s", buildName, exp.getCause().getMessage());
                logBuildFailedDetails(client, buildName);
            }
            throw exp;
        }
    }

    private void waitForOpenShiftBuildToComplete(OpenShiftClient client, Build build) throws IOException {
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch logTerminateLatch = new CountDownLatch(1);
        final String buildName = KubernetesHelper.getName(build);

        final AtomicReference<Build> buildHolder = new AtomicReference<>();

        // Don't query for logs directly, Watch over the build pod:
        waitUntilPodIsReady(buildName + "-build", 120, log);
        log.info("Waiting for build " + buildName + " to complete...");
        try (LogWatch logWatch = client.pods().inNamespace(applicableOpenShiftNamespace).withName(buildName + "-build").watchLog()) {
            KubernetesHelper.printLogsAsync(logWatch,
                    "Failed to tail build log", logTerminateLatch, log);
            Watcher<Build> buildWatcher = getBuildWatcher(latch, buildName, buildHolder);
            try (Watch watcher = client.builds().inNamespace(applicableOpenShiftNamespace).withName(buildName).watch(buildWatcher)) {
                // Check if the build is already finished to avoid waiting indefinitely
                Build lastBuild = client.builds().inNamespace(applicableOpenShiftNamespace).withName(buildName).get();
                if (OpenshiftHelper.isFinished(KubernetesHelper.getBuildStatusPhase(lastBuild))) {
                    log.debug("Build %s is already finished", buildName);
                    buildHolder.set(lastBuild);
                    latch.countDown();
                }

                waitUntilBuildFinished(latch);
                logTerminateLatch.countDown();

                build = buildHolder.get();
                if (build == null) {
                    log.debug("Build watcher on %s was closed prematurely", buildName);
                    build = client.builds().inNamespace(applicableOpenShiftNamespace).withName(buildName).get();
                }
                String status = KubernetesHelper.getBuildStatusPhase(build);
                if (OpenshiftHelper.isFailed(status) || OpenshiftHelper.isCancelled(status)) {
                    throw new IOException("OpenShift Build " + buildName + " failed: " + KubernetesHelper.getBuildStatusReason(build));
                }

                if (!OpenshiftHelper.isFinished(status)) {
                    log.warn("Could not wait for the completion of build %s. It may be  may be still running (status=%s)", buildName, status);
                } else {
                    log.info("Build %s in status %s", buildName, status);
                }
            }
        }
    }

    /**
     * A Simple utility function to watch over pod until it gets ready
     *
     * @param podName Name of the pod
     * @param nAwaitTimeout Time in seconds upto which pod must be watched
     * @param log Logger object
     */
    private void waitUntilPodIsReady(String podName, int nAwaitTimeout, final KitLogger log) {
        final CountDownLatch readyLatch = new CountDownLatch(1);
        try (Watch watch = client.pods().inNamespace(applicableOpenShiftNamespace).withName(podName).watch(new Watcher<Pod>() {
            @Override
            public void eventReceived(Action action, Pod aPod) {
                if(KubernetesHelper.isPodReady(aPod)) {
                    readyLatch.countDown();
                }
            }

            @Override
            public void onClose(WatcherException e) {
                // ignore
            }
        })) {
            readyLatch.await(nAwaitTimeout, TimeUnit.SECONDS);
        } catch (KubernetesClientException e) {
            log.error("Could not watch pod", e);
        } catch (InterruptedException e) {
            log.error("Could not watch pod (Thread interrupted)", e);
            Thread.currentThread().interrupt();
        }
    }

    private void waitUntilBuildFinished(CountDownLatch latch) {
        while (latch.getCount() > 0L) {
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private Watcher<Build> getBuildWatcher(final CountDownLatch latch, final String buildName, final AtomicReference<Build> buildHolder) {
        return new Watcher<Build>() {

            String lastStatus = "";

            @Override
            public void eventReceived(Action action, Build build) {
                buildHolder.set(build);
                String status = KubernetesHelper.getBuildStatusPhase(build);
                log.verbose("BuildWatch: Received event %s , build status: %s", action, build.getStatus());
                if (!lastStatus.equals(status)) {
                    lastStatus = status;
                    log.verbose("Build %s status: %s", buildName, status);
                }
                if (OpenshiftHelper.isFinished(status)) {
                    latch.countDown();
                }
            }

            @Override
            public void onClose(WatcherException cause) {
                if (cause != null) {
                    log.error("Error while watching for build to finish: %s ",
                            cause.getMessage());
                }
                latch.countDown();
            }
        };
    }

    private void logBuildFailedDetails(OpenShiftClient client, String buildName) {
        try {
            BuildConfig build = client.buildConfigs().inNamespace(applicableOpenShiftNamespace).withName(buildName).get();
            ObjectReference ref = build.getSpec().getStrategy().getSourceStrategy().getFrom();
            String kind = ref.getKind();
            String name = ref.getName();

            if (DOCKER_IMAGE.equals(kind)) {
                log.error("Please, ensure that the Docker image '%s' exists and is accessible by OpenShift", name);
            } else if (IMAGE_STREAM_TAG.equals(kind)) {
                String namespace = ref.getNamespace();
                String namespaceInfo = "current";
                String namespaceParams = "";
                if (namespace != null && !namespace.isEmpty()) {
                    namespaceInfo = "'" + namespace + "'";
                    namespaceParams = " -n " + namespace;
                }

                log.error("Please, ensure that the ImageStream Tag '%s' exists in the %s namespace (with 'oc get is%s')", name, namespaceInfo, namespaceParams);
            }
        } catch (Exception ex) {
            log.error("Unable to get detailed information from the BuildServiceConfig: " + ex.getMessage());
        }
    }

    private void logBuildFailure(OpenShiftClient client, String buildName) throws JKubeServiceException {
        try {
            List<Build> builds = client.builds().inNamespace(applicableOpenShiftNamespace).list().getItems();
            for(Build build : builds) {
                if(build.getMetadata().getName().contains(buildName)) {
                    log.error(build.getMetadata().getName() + "\t" + "\t" + build.getStatus().getReason() + "\t" + build.getStatus().getMessage());
                    throw new JKubeServiceException("Unable to build the image using the OpenShift build service", new KubernetesClientException(build.getStatus().getReason() + " " + build.getStatus().getMessage()));
                }
            }

            log.error("Also, check cluster events via `oc get events` to see what could have possibly gone wrong");
        } catch (KubernetesClientException clientException) {
            Status status = clientException.getStatus();
            if (status != null)
                log.error("OpenShift Error: [%d] %s", status.getCode(), status.getMessage());
        }
    }

    private void addImageStreamToFile(File imageStreamFile, ImageName imageName, OpenShiftClient client) throws IOException {
        ImageStreamService imageStreamHandler = new ImageStreamService(client, applicableOpenShiftNamespace, log);
        imageStreamHandler.appendImageStreamResource(imageName, imageStreamFile);
    }

    private void createAdditionalTags(ImageConfiguration imageConfig, ImageName imageName) {
        List<String> additionalTagsToCreate = getAdditionalTagsToCreate(imageConfig);
        if (!additionalTagsToCreate.isEmpty()) {
            ImageStreamTag imageStreamTag = client.imageStreamTags().inNamespace(applicableOpenShiftNamespace).withName(resolveImageStreamTagName(imageName)).get();
            List<ImageStreamTag> imageStreamTags = createAdditionalTagsIfPresent(imageConfig, applicableOpenShiftNamespace, imageStreamTag);
            client.resourceList(imageStreamTags.toArray(new ImageStreamTag[0])).inNamespace(applicableOpenShiftNamespace).createOrReplace();
            log.info("Tags [%s] set to %s", String.join(",", additionalTagsToCreate), imageName.getNameWithoutTag());
        }
    }

    // == Utility methods ==========================
    private Map<String, Map<String, Quantity>> getRequestsAndLimits() {
        Map<String, Map<String, Quantity>> keyToQuantityMap = new HashMap<>();
        if (buildServiceConfig.getResourceConfig() != null && buildServiceConfig.getResourceConfig().getOpenshiftBuildConfig() != null) {
            Map<String, Quantity> limits = KubernetesHelper.getQuantityFromString(buildServiceConfig.getResourceConfig().getOpenshiftBuildConfig().getLimits());
            if (!limits.isEmpty()) {
                keyToQuantityMap.put(LIMITS, limits);
            }
            Map<String, Quantity> requests = KubernetesHelper.getQuantityFromString(buildServiceConfig.getResourceConfig().getOpenshiftBuildConfig().getRequests());
            if (!requests.isEmpty()) {
                keyToQuantityMap.put(REQUESTS, requests);
            }
        }
        return keyToQuantityMap;
    }


}
