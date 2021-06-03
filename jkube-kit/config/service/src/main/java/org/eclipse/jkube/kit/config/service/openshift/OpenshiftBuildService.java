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

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.LocalObjectReferenceBuilder;
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
import io.fabric8.openshift.api.model.BuildOutputBuilder;
import io.fabric8.openshift.api.model.BuildSource;
import io.fabric8.openshift.api.model.BuildStrategy;
import io.fabric8.openshift.api.model.BuildStrategyBuilder;
import io.fabric8.openshift.api.model.ImageStreamBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import org.eclipse.jkube.kit.build.api.auth.AuthConfig;
import org.eclipse.jkube.kit.config.image.build.JKubeConfiguration;
import org.eclipse.jkube.kit.build.api.assembly.ArchiverCustomizer;
import org.eclipse.jkube.kit.build.api.assembly.AssemblyManager;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.RegistryConfig;
import org.eclipse.jkube.kit.build.service.docker.auth.AuthConfigFactory;
import org.eclipse.jkube.kit.build.api.helper.DockerFileUtil;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.ResourceFileType;
import org.eclipse.jkube.kit.common.util.EnvUtil;
import org.eclipse.jkube.kit.common.util.IoUtil;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.common.util.OpenshiftHelper;
import org.eclipse.jkube.kit.config.image.ImageName;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.service.BuildService;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.eclipse.jkube.kit.config.service.openshift.ImageStreamService.resolveImageStreamName;

/**
 * @author nicola
 */
public class OpenshiftBuildService implements BuildService {

    private static final String DEFAULT_S2I_BUILD_SUFFIX = "-s2i";
    public static final String DEFAULT_S2I_SOURCE_TYPE = "Binary";
    private static final String IMAGE_STREAM_TAG =  "ImageStreamTag";
    private static final String DOCKER_IMAGE =  "DockerImage";
    private static final String DEFAULT_BUILD_OUTPUT_KIND = IMAGE_STREAM_TAG;
    public static final String REQUESTS = "requests";
    public static final String LIMITS = "limits";

    private final OpenShiftClient client;
    private final KitLogger log;
    private final JKubeServiceHub jKubeServiceHub;
    private final BuildServiceConfig config;
    private AuthConfigFactory authConfigFactory;


    public OpenshiftBuildService(OpenShiftClient client, KitLogger log, JKubeServiceHub jKubeServiceHub) {
        Objects.requireNonNull(client, "client");
        Objects.requireNonNull(log, "log");
        Objects.requireNonNull(jKubeServiceHub.getConfiguration(), "JKubeConfiguration");
        Objects.requireNonNull(jKubeServiceHub.getDockerServiceHub(), "dockerServiceHub");
        Objects.requireNonNull(jKubeServiceHub.getBuildServiceConfig(), "config");

        this.client = client;
        this.log = log;
        this.jKubeServiceHub = jKubeServiceHub;
        config = jKubeServiceHub.getBuildServiceConfig();
    }

    @Override
    public void build(ImageConfiguration imageConfig) throws JKubeServiceException {
        String buildName = null;
        try {
            ImageName imageName = new ImageName(imageConfig.getName());

            File dockerTar = createBuildArchive(imageConfig);

            KubernetesListBuilder builder = new KubernetesListBuilder();

            // Check for buildconfig / imagestream / pullSecret and create them if necessary
            String openshiftPullSecret = config.getOpenshiftPullSecret();
            final boolean usePullSecret = checkOrCreatePullSecret(client, builder, openshiftPullSecret, imageConfig);
            if (usePullSecret) {
                buildName = updateOrCreateBuildConfig(config, client, builder, imageConfig, openshiftPullSecret);
            } else {
                buildName = updateOrCreateBuildConfig(config, client, builder, imageConfig, null);
            }

            if (config.getBuildOutputKind() == null || IMAGE_STREAM_TAG.equals(config.getBuildOutputKind())) {
                checkOrCreateImageStream(config, client, builder, resolveImageStreamName(imageName));

                applyBuild(buildName, dockerTar, builder);

                // Create a file with generated image streams
                addImageStreamToFile(getImageStreamFile(), imageName, client);
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

    private void applyBuild(String buildName, File dockerTar, KubernetesListBuilder builder)
            throws Exception, IOException {
        applyResourceObjects(config, client, builder);

        // Start the actual build
        Build build = startBuild(client, dockerTar, buildName);

        // Wait until the build finishes
        waitForOpenShiftBuildToComplete(client, build);
    }

    @Override
    public void push(Collection<ImageConfiguration> imageConfigs, int retries, RegistryConfig registryConfig, boolean skipTag) throws JKubeServiceException {
        // Do nothing. Image is pushed as part of build phase
        log.warn("Image is pushed to OpenShift's internal registry during oc:build goal. Skipping...");
    }

    protected File createBuildArchive(ImageConfiguration imageConfig) throws JKubeServiceException {
        // Adding S2I artifacts such as environment variables in S2I mode
        final ArchiverCustomizer customizer = getS2ICustomizer(imageConfig);

        try {
            return jKubeServiceHub.getDockerServiceHub().getArchiveService()
                .createDockerBuildArchive(imageConfig, jKubeServiceHub.getConfiguration(), customizer);
        } catch (IOException e) {
            throw new JKubeServiceException("Unable to create the build archive", e);
        }
    }

    private ArchiverCustomizer getS2ICustomizer(ImageConfiguration imageConfiguration) throws JKubeServiceException {
        try {
            if (imageConfiguration.getBuildConfiguration() != null && imageConfiguration.getBuildConfiguration().getEnv() != null) {
                String fileName = IoUtil.sanitizeFileName("s2i-env-" + imageConfiguration.getName());
                final File environmentFile = new File(config.getBuildDirectory(), fileName);

                try (PrintWriter out = new PrintWriter(new FileWriter(environmentFile))) {
                    for (Map.Entry<String, String> e : imageConfiguration.getBuildConfiguration().getEnv().entrySet()) {
                        out.println(e.getKey() + "=" + e.getValue());
                    }
                }

                return tarArchiver -> {
                    tarArchiver.includeFile(environmentFile, ".s2i/environment");
                    return tarArchiver;
                };
            } else {
                return null;
            }
        } catch (IOException e) {
            throw new JKubeServiceException("Unable to add environment variables to the S2I build archive", e);
        }
    }

    private File getImageStreamFile() {
        return ResourceFileType.yaml.addExtensionIfMissing(new File(config.getBuildDirectory(),
            String.format("%s-is", jKubeServiceHub.getConfiguration().getProject().getArtifactId())));
    }

    @Override
    public void postProcess(BuildServiceConfig config) {
        config.attachArtifact("is", getImageStreamFile());
    }

    protected String updateOrCreateBuildConfig(BuildServiceConfig config, OpenShiftClient client, KubernetesListBuilder builder, ImageConfiguration imageConfig, String openshiftPullSecret) {
        ImageName imageName = new ImageName(imageConfig.getName());
        String buildName = getS2IBuildName(config, imageName);

        BuildStrategy buildStrategyResource = createBuildStrategy(imageConfig, config.getJKubeBuildStrategy(), openshiftPullSecret);
        BuildOutput buildOutput = createBuildOutput(config, imageName);

        // Fetch existing build config
        BuildConfig buildConfig = client.buildConfigs().withName(buildName).get();
        if (buildConfig != null) {
            // lets verify the BC
            BuildConfigSpec spec = getBuildConfigSpec(buildConfig);
            validateSourceType(buildName, spec);

            if (config.getBuildRecreateMode().isBuildConfig()) {
                // Delete and recreate afresh
                client.buildConfigs().withName(buildName).delete();
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

    private void validateSourceType(String buildName, BuildConfigSpec spec) {
        BuildSource source = spec.getSource();
        if (source != null) {
            String sourceType = source.getType();
            if (!Objects.equals(DEFAULT_S2I_SOURCE_TYPE, sourceType)) {
                log.warn("BuildServiceConfig %s is not of type: 'Binary' but is '%s' !", buildName, sourceType);
            }
        }
    }

    private BuildConfigSpec getBuildConfigSpec(BuildConfig buildConfig) {
        BuildConfigSpec spec = buildConfig.getSpec();
        if (spec == null) {
            spec = new BuildConfigSpec();
            buildConfig.setSpec(spec);
        }
        return spec;
    }

    private BuildConfigSpec getBuildConfigSpec(BuildStrategy buildStrategyResource, BuildOutput buildOutput) {
        BuildConfigSpecBuilder specBuilder = null;

        // Check for BuildConfig resource fragment
        File buildConfigResourceFragment = KubernetesHelper.getResourceFragmentFromSource(config.getResourceDir(), config.getResourceConfig() != null ? config.getResourceConfig().getRemotes() : null, "buildconfig.yml", log);
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
            client.buildConfigs().withName(buildName).edit(bc -> new BuildConfigBuilder(bc)
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

    private BuildStrategy createBuildStrategy(ImageConfiguration imageConfig, JKubeBuildStrategy osBuildStrategy, String openshiftPullSecret) {
        final BuildConfiguration buildConfig = imageConfig.getBuildConfiguration();
        final Map<String, String> fromExt = buildConfig.getFromExt();
        final String fromName;
        if(buildConfig.isDockerFileMode()) {
            fromName = extractBaseFromDockerfile(buildConfig, jKubeServiceHub.getConfiguration());
        } else {
            fromName = getMapValueWithDefault(fromExt, JKubeBuildStrategy.SourceStrategy.name, buildConfig.getFrom());
        }
        final String fromKind = getMapValueWithDefault(fromExt, JKubeBuildStrategy.SourceStrategy.kind, DOCKER_IMAGE);
        final String fromNamespace = getMapValueWithDefault(fromExt, JKubeBuildStrategy.SourceStrategy.namespace, IMAGE_STREAM_TAG.equals(fromKind) ? "openshift" : null);
        if (osBuildStrategy == JKubeBuildStrategy.docker) {
            BuildStrategy buildStrategy = new BuildStrategyBuilder()
                    .withType("Docker")
                    .withNewDockerStrategy()
                        .withNewFrom()
                            .withKind(fromKind)
                            .withName(fromName)
                            .withNamespace(StringUtils.isEmpty(fromNamespace) ? null : fromNamespace)
                        .endFrom()
                        .withEnv(checkForEnv(imageConfig))
                        .withNoCache(checkForNocache(imageConfig))
                    .endDockerStrategy().build();

            if (openshiftPullSecret != null) {
                buildStrategy.getDockerStrategy().setPullSecret(new LocalObjectReferenceBuilder()
                .withName(openshiftPullSecret)
                .build());
            }
            return buildStrategy;
        } else if (osBuildStrategy == JKubeBuildStrategy.s2i) {
            BuildStrategy buildStrategy = new BuildStrategyBuilder()
                    .withType("Source")
                    .withNewSourceStrategy()
                        .withNewFrom()
                            .withKind(fromKind)
                            .withName(fromName)
                            .withNamespace(StringUtils.isEmpty(fromNamespace) ? null : fromNamespace)
                        .endFrom()
                        .withForcePull(config.isForcePull())
                    .endSourceStrategy()
                    .build();
            if (openshiftPullSecret != null) {
                buildStrategy.getSourceStrategy().setPullSecret(new LocalObjectReferenceBuilder()
                .withName(openshiftPullSecret)
                .build());
            }
            return buildStrategy;

        } else {
            throw new IllegalArgumentException("Unsupported BuildStrategy " + osBuildStrategy);
        }
    }

    private BuildOutput createBuildOutput(BuildServiceConfig config, ImageName imageName) {
        final String buildOutputKind = Optional.ofNullable(config.getBuildOutputKind()).orElse(DEFAULT_BUILD_OUTPUT_KIND);
        final String outputImageStreamTag = resolveImageStreamName(imageName) + ":" + (imageName.getTag() != null ? imageName.getTag() : "latest");
        final BuildOutputBuilder buildOutputBuilder = new BuildOutputBuilder();
        buildOutputBuilder.withNewTo().withKind(buildOutputKind).withName(outputImageStreamTag).endTo();
        if (DOCKER_IMAGE.equals(buildOutputKind)) {
            buildOutputBuilder.editTo().withName(imageName.getFullName()).endTo();
        }
        if(StringUtils.isNotBlank(config.getOpenshiftPushSecret())) {
            buildOutputBuilder.withNewPushSecret().withName(config.getOpenshiftPushSecret()).endPushSecret();
        }
        return buildOutputBuilder.build();
    }

    private Boolean checkForNocache(ImageConfiguration imageConfig) {
        String nocache = System.getProperty("docker.nocache");
        if (nocache != null) {
            return nocache.length() == 0 || Boolean.parseBoolean(nocache);
        } else {
            BuildConfiguration buildConfig = imageConfig.getBuildConfiguration();
            return buildConfig.nocache();
        }
    }

    private List<EnvVar> checkForEnv(ImageConfiguration imageConfiguration) {
        BuildConfiguration buildImageConfiguration = imageConfiguration.getBuildConfiguration();
        if (buildImageConfiguration.getArgs() != null) {
            return KubernetesHelper.convertToEnvVarList(buildImageConfiguration.getArgs());
        }

        return Collections.emptyList();
    }

    private boolean checkOrCreatePullSecret(OpenShiftClient client, KubernetesListBuilder builder, String pullSecretName, ImageConfiguration imageConfig)
            throws Exception {
        JKubeConfiguration configuration = jKubeServiceHub.getConfiguration();
        BuildConfiguration buildConfig = imageConfig.getBuildConfiguration();

        String fromImage;
        if (buildConfig.isDockerFileMode()) {
            fromImage = extractBaseFromDockerfile(buildConfig, configuration);
        } else {
            fromImage = extractBaseFromConfiguration(buildConfig);
        }

        String pullRegistry = EnvUtil.firstRegistryOf(new ImageName(fromImage).getRegistry(), configuration.getRegistryConfig().getRegistry(), configuration.getRegistryConfig().getRegistry());

        if (pullRegistry != null) {
            RegistryConfig registryConfig = configuration.getRegistryConfig();
            final AuthConfig authConfig = new AuthConfigFactory(log).createAuthConfig(false, registryConfig.isSkipExtendedAuth(), registryConfig.getAuthConfig(),
                    registryConfig.getSettings(), null, pullRegistry, registryConfig.getPasswordDecryptionMethod());

            final Secret secret = Optional.ofNullable(pullSecretName)
                .map(psn ->  client.secrets().withName(psn).get()).orElse(null);

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
            client.secrets().withName(pullSecretName).edit(s -> new SecretBuilder(s)
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


    private String extractBaseFromConfiguration(BuildConfiguration buildConfig) {
        String fromImage;
        fromImage = buildConfig.getFrom();
        if (fromImage == null) {
            AssemblyConfiguration assemblyConfig = buildConfig.getAssembly();
            if (assemblyConfig == null) {
                fromImage = AssemblyManager.DEFAULT_DATA_BASE_IMAGE;
            }
        }
        return fromImage;
    }

    private String extractBaseFromDockerfile(BuildConfiguration buildConfig, JKubeConfiguration configuration) {
        String fromImage;
        try {
            File fullDockerFilePath = buildConfig.getAbsoluteDockerFilePath(configuration.getSourceDirectory(),
                Optional.ofNullable(configuration.getProject().getBaseDirectory()).map(File::toString) .orElse(null));
            fromImage = DockerFileUtil.extractBaseImages(
                    fullDockerFilePath, configuration.getProperties(), buildConfig.getFilter(), buildConfig.getArgs()).stream().findFirst().orElse(null);
        } catch (IOException e) {
            // Cant extract base image, so we wont try an auto pull. An error will occur later anyway when
            // building the image, so we are passive here.
            fromImage = null;
        }
        return fromImage;
    }

    private void checkOrCreateImageStream(BuildServiceConfig config, OpenShiftClient client, KubernetesListBuilder builder, String imageStreamName) {
        boolean hasImageStream = client.imageStreams().withName(imageStreamName).get() != null;
        if (hasImageStream && config.getBuildRecreateMode().isImageStream()) {
            client.imageStreams().withName(imageStreamName).delete();
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
            client.lists().create(k8sList);
        }
    }

    private Build startBuild(OpenShiftClient client, File dockerTar, String buildName) {
        log.info("Starting Build %s", buildName);
        try {
            return client.buildConfigs().withName(buildName)
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
        try (LogWatch logWatch = client.pods().withName(buildName + "-build").watchLog()) {
            KubernetesHelper.printLogsAsync(logWatch,
                    "Failed to tail build log", logTerminateLatch, log);
            Watcher<Build> buildWatcher = getBuildWatcher(latch, buildName, buildHolder);
            try (Watch watcher = client.builds().withName(buildName).watch(buildWatcher)) {
                // Check if the build is already finished to avoid waiting indefinitely
                Build lastBuild = client.builds().withName(buildName).get();
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
                    build = client.builds().withName(buildName).get();
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
        try (Watch watch = client.pods().withName(podName).watch(new Watcher<Pod>() {
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
            BuildConfig build = client.buildConfigs().withName(buildName).get();
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
            List<Build> builds = client.builds().inNamespace(client.getNamespace()).list().getItems();
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
        ImageStreamService imageStreamHandler = new ImageStreamService(client, log);
        imageStreamHandler.appendImageStreamResource(imageName, imageStreamFile);
    }

    // == Utility methods ==========================

    private String getS2IBuildName(BuildServiceConfig config, ImageName imageName) {
        final StringBuilder s2IBuildName = new StringBuilder(resolveImageStreamName(imageName));
        if (!StringUtils.isEmpty(config.getS2iBuildNameSuffix())) {
            s2IBuildName.append(config.getS2iBuildNameSuffix());
        } else if (config.getJKubeBuildStrategy() == JKubeBuildStrategy.s2i) {
            s2IBuildName.append(DEFAULT_S2I_BUILD_SUFFIX);
        }
        return s2IBuildName.toString();
    }

    private String getMapValueWithDefault(Map<String, String> map, JKubeBuildStrategy.SourceStrategy strategy, String defaultValue) {
        return getMapValueWithDefault(map, strategy.key(), defaultValue);
    }

    private String getMapValueWithDefault(Map<String, String> map, String field, String defaultValue) {
        if (map == null) {
            return defaultValue;
        }
        String value = map.get(field);
        return value != null ? value : defaultValue;
    }

    private Map<String, Map<String, Quantity>> getRequestsAndLimits() {
        Map<String, Map<String, Quantity>> keyToQuantityMap = new HashMap<>();
        if (config.getResourceConfig() != null && config.getResourceConfig().getOpenshiftBuildConfig() != null) {
            Map<String, Quantity> limits = KubernetesHelper.getQuantityFromString(config.getResourceConfig().getOpenshiftBuildConfig().getLimits());
            if (!limits.isEmpty()) {
                keyToQuantityMap.put(LIMITS, limits);
            }
            Map<String, Quantity> requests = KubernetesHelper.getQuantityFromString(config.getResourceConfig().getOpenshiftBuildConfig().getRequests());
            if (!requests.isEmpty()) {
                keyToQuantityMap.put(REQUESTS, requests);
            }
        }
        return keyToQuantityMap;
    }

}
