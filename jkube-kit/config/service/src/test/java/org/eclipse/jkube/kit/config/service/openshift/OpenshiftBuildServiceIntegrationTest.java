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

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.WatchEvent;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.openshift.api.model.Build;
import io.fabric8.openshift.api.model.BuildBuilder;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.BuildConfigBuilder;
import io.fabric8.openshift.api.model.BuildStrategyBuilder;
import io.fabric8.openshift.api.model.ImageStream;
import io.fabric8.openshift.api.model.ImageStreamBuilder;
import io.fabric8.openshift.api.model.ImageStreamStatusBuilder;
import io.fabric8.openshift.api.model.NamedTagEventListBuilder;
import io.fabric8.openshift.client.server.mock.OpenShiftMockServer;
import io.fabric8.openshift.client.server.mock.OpenShiftServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.commons.io.FileUtils;
import org.eclipse.jkube.kit.build.api.assembly.AssemblyManager;
import org.eclipse.jkube.kit.build.api.assembly.BuildDirs;
import org.eclipse.jkube.kit.build.api.assembly.JKubeBuildTarArchiver;
import org.eclipse.jkube.kit.build.service.docker.ArchiveService;
import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.RegistryConfig;
import org.eclipse.jkube.kit.common.archive.ArchiveCompression;
import org.eclipse.jkube.kit.common.util.OpenshiftHelper;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.BuildRecreateMode;
import org.eclipse.jkube.kit.config.resource.OpenshiftBuildConfig;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;
import mockit.Expectations;
import mockit.Mocked;

import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class OpenshiftBuildServiceIntegrationTest {

    private static final int MAX_TIMEOUT_RETRIES = 5;

    @Rule
    public final OpenShiftServer mockServer = new OpenShiftServer(false);

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mocked
    private JKubeServiceHub jKubeServiceHub;

    @Mocked
    private OpenshiftHelper openshiftHelper;

    @Mocked
    private ClusterAccess clusterAccess;

    @Mocked
    private JKubeBuildTarArchiver jKubeBuildTarArchiver;

    private File baseDirectory;

    private String baseDir;

    private File target;

    private String projectName;

    private KitLogger logger;

    private ImageConfiguration image;

    private BuildServiceConfig.BuildServiceConfigBuilder defaultConfig;

    private BuildServiceConfig.BuildServiceConfigBuilder defaultConfigSecret;

    private BuildServiceConfig.BuildServiceConfigBuilder dockerImageConfigSecret;

    private BuildServiceConfig.BuildServiceConfigBuilder dockerImageConfig;

    @Before
    public void init() throws Exception {
        baseDirectory = temporaryFolder.newFolder("openshift-build-service");
        target = temporaryFolder.newFolder("openshift-build-service", "target");
        final File emptyDockerBuildTar = new File(target, "docker-build.tar");
        FileUtils.touch(emptyDockerBuildTar);
        baseDir = baseDirectory.getAbsolutePath();
        projectName = "myapp";
        logger = new KitLogger.StdoutLogger();
        FileUtils.deleteDirectory(new File(baseDir, projectName));
        final File dockerFile = new File(baseDir, "Docker.tar");
        FileUtils.touch(dockerFile);

        // @formatter:off
        new Expectations(openshiftHelper) {{
            // Partial Mock
            openshiftHelper.isOpenShift(mockServer.getOpenshiftClient()); result = true;

            clusterAccess.createDefaultClient(); result = mockServer.getOpenshiftClient();
            jKubeServiceHub.getLog(); result = logger;
            jKubeServiceHub.getDockerServiceHub(); minTimes = 0;
            jKubeServiceHub.getDockerServiceHub().getArchiveService(); minTimes = 0;
            result = new ArchiveService(AssemblyManager.getInstance(), logger);
            jKubeServiceHub.getBuildServiceConfig().getBuildDirectory(); result = target.getAbsolutePath(); minTimes = 0;
            jKubeServiceHub.getConfiguration();
            result = JKubeConfiguration.builder()
                .outputDirectory(target.getAbsolutePath())
                .project(JavaProject.builder()
                    .baseDirectory(baseDirectory)
                    .buildDirectory(target)
                    .build())
                .registryConfig(RegistryConfig.builder().build())
                .build();
            jKubeBuildTarArchiver.createArchive(withInstanceOf(File.class), withInstanceOf(BuildDirs.class), withEqual(ArchiveCompression.none));
            result = emptyDockerBuildTar; minTimes = 0;
        }};
        // @formatter:on

        image = ImageConfiguration.builder()
                .name(projectName)
                .build(BuildConfiguration.builder()
                        .from(projectName)
                        .build()
                ).build();

        defaultConfig = BuildServiceConfig.builder()
                .buildDirectory(baseDir)
                .buildRecreateMode(BuildRecreateMode.none)
                .s2iBuildNameSuffix("-s2i-suffix2")
                .jKubeBuildStrategy(JKubeBuildStrategy.s2i);

        defaultConfigSecret = defaultConfig.build().toBuilder().openshiftPullSecret("pullsecret-fabric8");

        dockerImageConfig = defaultConfig.build().toBuilder().buildOutputKind("DockerImage");

        dockerImageConfigSecret = defaultConfig.build().toBuilder()
                .openshiftPullSecret("pullsecret-fabric8")
                .openshiftPushSecret("pushsecret-fabric8")
                .buildOutputKind("DockerImage");
    }

    @Test
    public void testSuccessfulBuild() throws Exception {
        retryInMockServer(() -> {
            final BuildServiceConfig config = withBuildServiceConfig(defaultConfig.build());
            final WebServerEventCollector collector = createMockServer(config, true, 50, false, false);

            new OpenshiftBuildService(jKubeServiceHub).build(image);

            // we should find a better way to assert that a certain call has been made
            assertTrue(mockServer.getOpenShiftMockServer().getRequestCount() > 8);
            collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed");
            assertEquals("{\"apiVersion\":\"build.openshift.io/v1\",\"kind\":\"BuildConfig\",\"metadata\":{\"name\":\"myapp-s2i-suffix2\"},\"spec\":{\"output\":{\"to\":{\"kind\":\"ImageStreamTag\",\"name\":\"myapp:latest\"}},\"source\":{\"type\":\"Binary\"},\"strategy\":{\"sourceStrategy\":{\"forcePull\":false,\"from\":{\"kind\":\"DockerImage\",\"name\":\"myapp\"}},\"type\":\"Source\"}}}", collector.getBodies().get(1));
            collector.assertEventsNotRecorded("patch-build-config");
            assertTrue(containsRequest("imagestreams"));
        });
    }

    @Test
    public void build_withDockerfileModeAndFlattenedAssembly_shouldThrowException() {
        //Given
        image.setBuild(BuildConfiguration.builder()
            .dockerFile(new File(target, "Dockerfile").getAbsolutePath())
            .assembly(AssemblyConfiguration.builder()
                .layer(Assembly.builder().id("one").build())
                .build().getFlattenedClone(jKubeServiceHub.getConfiguration()))
            .build());
        image.getBuildConfiguration().initAndValidate();
        final OpenshiftBuildService openshiftBuildService = new OpenshiftBuildService(jKubeServiceHub);
        // When
        final JKubeServiceException result = assertThrows(JKubeServiceException.class, () ->
            openshiftBuildService.build(image));
        // Then
        assertThat(result)
            .getCause().hasMessage("This image has already been flattened, you can only flatten the image once");
    }

    @Test
    public void build_withDockerfileModeAndAssembly_shouldSucceed() throws Exception {
        //Given
        final File dockerFile = new File(target, "Dockerfile");
        FileUtils.write(dockerFile, "FROM busybox\n", StandardCharsets.UTF_8);
        image.setBuild(BuildConfiguration.builder()
            .from(projectName)
            .dockerFile(dockerFile.getAbsolutePath())
            .assembly(AssemblyConfiguration.builder()
                .layer(Assembly.builder().id("one").baseDirectory(baseDirectory).build())
                .build())
            .build());
        image.getBuildConfiguration().initAndValidate();
        final WebServerEventCollector collector = createMockServer(withBuildServiceConfig(defaultConfig.build()),
            true, 50, false, false);
        // When
        new OpenshiftBuildService(jKubeServiceHub).build(image);
        // Then
        collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed");
    }

    @Test
    public void testSuccessfulBuildNoS2iSuffix() throws Exception {
        retryInMockServer(() -> {
            final BuildServiceConfig config = withBuildServiceConfig(defaultConfig.s2iBuildNameSuffix(null).build());
            final WebServerEventCollector collector = createMockServer(
                config, true, 50, false, false);

            new OpenshiftBuildService(jKubeServiceHub).build(image);

            // we should find a better way to assert that a certain call has been made
            assertTrue(mockServer.getOpenShiftMockServer().getRequestCount() > 8);
            collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed");
            assertEquals("{\"apiVersion\":\"build.openshift.io/v1\",\"kind\":\"BuildConfig\",\"metadata\":{\"name\":\"myapp-s2i\"},\"spec\":{\"output\":{\"to\":{\"kind\":\"ImageStreamTag\",\"name\":\"myapp:latest\"}},\"source\":{\"type\":\"Binary\"},\"strategy\":{\"sourceStrategy\":{\"forcePull\":false,\"from\":{\"kind\":\"DockerImage\",\"name\":\"myapp\"}},\"type\":\"Source\"}}}", collector.getBodies().get(1));
            collector.assertEventsNotRecorded("patch-build-config");
        });
    }

    @Test
    public void testDockerBuild() throws Exception {
        retryInMockServer(() -> {
            final BuildServiceConfig dockerConfig = withBuildServiceConfig(BuildServiceConfig.builder()
                .buildDirectory(baseDir)
                .buildRecreateMode(BuildRecreateMode.none)
                .s2iBuildNameSuffix("-docker")
                .jKubeBuildStrategy(JKubeBuildStrategy.docker).build());
            final WebServerEventCollector collector = createMockServer(dockerConfig, true, 50,
                    false, false);

            new OpenshiftBuildService(jKubeServiceHub).build(image);

            assertTrue(mockServer.getOpenShiftMockServer().getRequestCount() > 8);
            collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed");
            assertEquals("{\"apiVersion\":\"build.openshift.io/v1\",\"kind\":\"BuildConfig\",\"metadata\":{\"name\":\"myapp-docker\"},\"spec\":{\"output\":{\"to\":{\"kind\":\"ImageStreamTag\",\"name\":\"myapp:latest\"}},\"source\":{\"type\":\"Binary\"},\"strategy\":{\"dockerStrategy\":{\"from\":{\"kind\":\"DockerImage\",\"name\":\"myapp\"},\"noCache\":false},\"type\":\"Docker\"}}}", collector.getBodies().get(1));
            collector.assertEventsNotRecorded("patch-build-config");
        });
    }

    @Test
    public void testDockerBuildWithMultiComponentImageName() throws Exception {
        retryInMockServer(() -> {
            final BuildServiceConfig dockerConfig = withBuildServiceConfig(BuildServiceConfig.builder()
                .buildDirectory(baseDir)
                .buildRecreateMode(BuildRecreateMode.none)
                .s2iBuildNameSuffix("-docker")
                .jKubeBuildStrategy(JKubeBuildStrategy.docker).build());
            image.setName("docker.io/registry/component1/component2/name:tag");
            final WebServerEventCollector collector = createMockServer("component1-component2-name",
                dockerConfig, true, 50, false, false);

            new OpenshiftBuildService(jKubeServiceHub).build(image);

            assertTrue(mockServer.getOpenShiftMockServer().getRequestCount() > 8);
            collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed");
            assertEquals("{\"apiVersion\":\"build.openshift.io/v1\",\"kind\":\"BuildConfig\",\"metadata\":{\"name\":\"component1-component2-name-docker\"},\"spec\":{\"output\":{\"to\":{\"kind\":\"ImageStreamTag\",\"name\":\"component1-component2-name:tag\"}},\"source\":{\"type\":\"Binary\"},\"strategy\":{\"dockerStrategy\":{\"from\":{\"kind\":\"DockerImage\",\"name\":\"myapp\"},\"noCache\":false},\"type\":\"Docker\"}}}", collector.getBodies().get(1));
            collector.assertEventsNotRecorded("patch-build-config");
        });
    }

    @Test
    public void testDockerBuildNoS2iSuffix() throws Exception {
        retryInMockServer(() -> {
            final BuildServiceConfig dockerConfig = withBuildServiceConfig(BuildServiceConfig.builder()
                .buildDirectory(baseDir)
                .buildRecreateMode(BuildRecreateMode.none)
                .jKubeBuildStrategy(JKubeBuildStrategy.docker)
                .build());
            final WebServerEventCollector collector = createMockServer(dockerConfig, true, 50,
                    false, false);

            new OpenshiftBuildService(jKubeServiceHub).build(image);

            assertTrue(mockServer.getOpenShiftMockServer().getRequestCount() > 8);
            collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed");
            assertEquals("{\"apiVersion\":\"build.openshift.io/v1\",\"kind\":\"BuildConfig\",\"metadata\":{\"name\":\"myapp\"},\"spec\":{\"output\":{\"to\":{\"kind\":\"ImageStreamTag\",\"name\":\"myapp:latest\"}},\"source\":{\"type\":\"Binary\"},\"strategy\":{\"dockerStrategy\":{\"from\":{\"kind\":\"DockerImage\",\"name\":\"myapp\"},\"noCache\":false},\"type\":\"Docker\"}}}", collector.getBodies().get(1));
            collector.assertEventsNotRecorded("patch-build-config");
        });
    }

    @Test
    public void testDockerBuildFromExt() throws Exception {
        retryInMockServer(() -> {
            final BuildServiceConfig dockerConfig = withBuildServiceConfig(BuildServiceConfig.builder()
                .buildDirectory(baseDir)
                .buildRecreateMode(BuildRecreateMode.none)
                .s2iBuildNameSuffix("-docker")
                .jKubeBuildStrategy(JKubeBuildStrategy.docker)
                .build());
            final WebServerEventCollector collector = createMockServer(dockerConfig, true, 50,
                    false, false);

            OpenshiftBuildService service = new OpenshiftBuildService(jKubeServiceHub);
            Map<String,String> fromExt = ImmutableMap.of("name", "app:1.2-1",
                    "kind", "ImageStreamTag",
                    "namespace", "my-project");
            ImageConfiguration fromExtImage = ImageConfiguration.builder()
                    .name(projectName)
                    .build(BuildConfiguration.builder()
                            .fromExt(fromExt)
                            .nocache(Boolean.TRUE)
                            .build()
                    ).build();

            service.build(fromExtImage);

            assertTrue(mockServer.getOpenShiftMockServer().getRequestCount() > 8);
            collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed");
            assertEquals("{\"apiVersion\":\"build.openshift.io/v1\",\"kind\":\"BuildConfig\",\"metadata\":{\"name\":\"myapp-docker\"},\"spec\":{\"output\":{\"to\":{\"kind\":\"ImageStreamTag\",\"name\":\"myapp:latest\"}},\"source\":{\"type\":\"Binary\"},\"strategy\":{\"dockerStrategy\":{\"from\":{\"kind\":\"ImageStreamTag\",\"name\":\"app:1.2-1\",\"namespace\":\"my-project\"},\"noCache\":true},\"type\":\"Docker\"}}}", collector.getBodies().get(1));
            collector.assertEventsNotRecorded("patch-build-config");
        });
    }

    @Test
    public void testSuccessfulBuildSecret() throws Exception {
        retryInMockServer(() -> {
            final BuildServiceConfig config = withBuildServiceConfig(defaultConfigSecret.build());
            final WebServerEventCollector collector = createMockServer(config, true, 50, false, false);

            new OpenshiftBuildService(jKubeServiceHub).build(image);

            // we should find a better way to assert that a certain call has been made
            assertTrue(mockServer.getOpenShiftMockServer().getRequestCount() > 8);
            collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed");
            collector.assertEventsNotRecorded("patch-build-config");
        });
    }

    @Test
    public void testFailedBuild() {
        withBuildServiceConfig(defaultConfig.build());
        final OpenshiftBuildService openshiftBuildService = new OpenshiftBuildService(jKubeServiceHub);

        final JKubeServiceException result = assertThrows(JKubeServiceException.class, () ->
            openshiftBuildService.build(image));

        assertThat(result).hasMessageContaining("Unable to build the image using the OpenShift build service");
    }

    @Test
    public void testSuccessfulSecondBuild() throws Exception {
        retryInMockServer(() -> {
            final BuildServiceConfig config = withBuildServiceConfig(defaultConfig.build());
            final WebServerEventCollector collector = createMockServer(config, true, 50, true, true);

            new OpenshiftBuildService(jKubeServiceHub).build(image);

            collector.assertEventsRecordedInOrder("build-config-check", "patch-build-config", "pushed");
            collector.assertEventsNotRecorded("new-build-config");
        });
    }

    @Test
    public void testSuccessfulBuildWithResourceConfig() throws Exception {
        retryInMockServer(() -> {
            final Map<String, String> limitsMap = new HashMap<>();
            limitsMap.put("cpu", "100m");
            limitsMap.put("memory", "256Mi");
            final BuildServiceConfig config = withBuildServiceConfig(defaultConfig
                .resourceConfig(ResourceConfig.builder()
                    .openshiftBuildConfig(OpenshiftBuildConfig.builder().limits(limitsMap).build())
                    .build()
                ).build());
            final WebServerEventCollector collector = createMockServer(config, true, 50, false, false);

            new OpenshiftBuildService(jKubeServiceHub).build(image);

            collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed");
            collector.assertEventsNotRecorded("patch-build-config");
            assertThat(Serialization.unmarshal(collector.getBodies().get(1), BuildConfig.class))
                .hasFieldOrPropertyWithValue("metadata.name", "myapp-s2i-suffix2")
                .extracting(BuildConfig::getSpec)
                .hasFieldOrPropertyWithValue("output.to.kind", "ImageStreamTag")
                .hasFieldOrPropertyWithValue("output.to.name", "myapp:latest")
                .hasFieldOrPropertyWithValue("output.to.namespace", null)
                .hasFieldOrPropertyWithValue("resources.limits.memory", Quantity.parse("256Mi"))
                .hasFieldOrPropertyWithValue("resources.limits.cpu", Quantity.parse("100m"))
                .hasFieldOrPropertyWithValue("source.type", "Binary")
                .hasFieldOrPropertyWithValue("strategy.type", "Source")
                .hasFieldOrPropertyWithValue("strategy.sourceStrategy.from.kind", "DockerImage")
                .hasFieldOrPropertyWithValue("strategy.sourceStrategy.from.name", "myapp");
            assertTrue(containsRequest("imagestreams"));
        });
    }

    @Test
    public void testSuccessfulDockerImageOutputBuild() throws Exception {
        retryInMockServer(() -> {
            final BuildServiceConfig config = withBuildServiceConfig(dockerImageConfig.build());
            final WebServerEventCollector collector = createMockServer(config, true, 50, false, false);

            new OpenshiftBuildService(jKubeServiceHub).build(image);

            // we should add a better way to assert that a certain call has been made
            assertTrue(mockServer.getOpenShiftMockServer().getRequestCount() > 7);
            collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed");
            assertEquals("{\"apiVersion\":\"build.openshift.io/v1\",\"kind\":\"BuildConfig\",\"metadata\":{\"name\":\"myapp-s2i-suffix2\"},\"spec\":{\"output\":{\"to\":{\"kind\":\"DockerImage\",\"name\":\"myapp:latest\"}},\"source\":{\"type\":\"Binary\"},\"strategy\":{\"sourceStrategy\":{\"forcePull\":false,\"from\":{\"kind\":\"DockerImage\",\"name\":\"myapp\"}},\"type\":\"Source\"}}}", collector.getBodies().get(1));
            collector.assertEventsNotRecorded("patch-build-config");
            assertFalse(containsRequest("imagestreams"));
        });
    }

    @Test
    public void testSuccessfulDockerImageOutputBuildSecret() throws Exception {
        retryInMockServer(() -> {
            final BuildServiceConfig config = withBuildServiceConfig(dockerImageConfigSecret.build());
            final WebServerEventCollector collector = createMockServer(config, true, 50, false, false);

            new OpenshiftBuildService(jKubeServiceHub).build(image);

            // we should find a better way to assert that a certain call has been made
            assertTrue(mockServer.getOpenShiftMockServer().getRequestCount() > 7);
            collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed");
            assertEquals("{\"apiVersion\":\"build.openshift.io/v1\",\"kind\":\"BuildConfig\",\"metadata\":{\"name\":\"myapp-s2i-suffix2\"},\"spec\":{\"output\":{\"pushSecret\":{\"name\":\"pushsecret-fabric8\"},\"to\":{\"kind\":\"DockerImage\",\"name\":\"myapp:latest\"}},\"source\":{\"type\":\"Binary\"},\"strategy\":{\"sourceStrategy\":{\"forcePull\":false,\"from\":{\"kind\":\"DockerImage\",\"name\":\"myapp\"}},\"type\":\"Source\"}}}", collector.getBodies().get(1));
            collector.assertEventsNotRecorded("patch-build-config");
            assertFalse(containsRequest("imagestreams"));
        });
    }

    @FunctionalInterface
    private interface MockServerRetryable {
        void run() throws JKubeServiceException, IOException;
    }

    private void retryInMockServer(MockServerRetryable retryable) throws Exception {
        Throwable rootCause = null;
        int nTries = 0;
        boolean bTestComplete = false;
        do {
            try {
                nTries++;
                retryable.run();
                bTestComplete = true;
            } catch (JKubeServiceException exception) {
                rootCause = getRootCause(exception);
                logger.warn("A problem encountered while running test {}, retrying..", exception.getMessage());
            }
        } while (nTries < MAX_TIMEOUT_RETRIES && !bTestComplete);
        if (!bTestComplete && rootCause != null) {
            throw new Exception("Test did not complete", rootCause);
        }
    }

    protected WebServerEventCollector createMockServer(
        BuildServiceConfig config, boolean success, long buildDelay, boolean buildConfigExists, boolean imageStreamExists) {
        return createMockServer(projectName, config, success, buildDelay, buildConfigExists, imageStreamExists);
    }

    protected WebServerEventCollector createMockServer(String resourceName,
        BuildServiceConfig config, boolean success, long buildDelay, boolean buildConfigExists, boolean imageStreamExists) {
        WebServerEventCollector collector = new WebServerEventCollector();


        final String s2iBuildNameSuffix = Optional
                .ofNullable(config.getS2iBuildNameSuffix())
                .orElseGet(() -> config.getJKubeBuildStrategy() == JKubeBuildStrategy.s2i ?
                        "-s2i" : "");

        final String buildOutputKind = Optional.ofNullable(config.getBuildOutputKind()).orElse("ImageStreamTag");

        BuildConfig bc = new BuildConfigBuilder()
                .withNewMetadata()
                .withName(resourceName + s2iBuildNameSuffix)
                .endMetadata()
                .withNewSpec()
                .withNewOutput()
                    .withNewTo()
                    .withKind(buildOutputKind)
                    .endTo()
                    .endOutput()
                .endSpec()
                .build();

        BuildConfig bcSecret = null;
        if (config.getOpenshiftPullSecret() != null) {
            bcSecret = new BuildConfigBuilder()
                    .withNewMetadata()
                    .withName(resourceName + s2iBuildNameSuffix + "pullSecret")
                    .endMetadata()
                    .withNewSpec()
                    .withStrategy(new BuildStrategyBuilder().withType("Docker")
                            .withNewDockerStrategy()
                            .withNewPullSecret(config.getOpenshiftPullSecret())
                            .endDockerStrategy().build())
                    .withNewOutput()
                        .withNewPushSecret().withName(config.getOpenshiftPushSecret()).endPushSecret()
                        .withNewTo().withKind(buildOutputKind).endTo()
                        .endOutput()
                    .endSpec()
                    .build();
        }

        ImageStream imageStream = new ImageStreamBuilder()
                .withNewMetadata()
                .withName(resourceName)
                .endMetadata()
                .withStatus(new ImageStreamStatusBuilder()
                        .addNewTagLike(new NamedTagEventListBuilder()
                                .addNewItem()
                                .withImage("abcdef0123456789")
                                .endItem()
                                .build())
                        .endTag()
                        .build())
                .build();

        KubernetesList builds = new KubernetesListBuilder().withItems(
                new BuildBuilder()
                        .withNewMetadata()
                        .withName(resourceName)
                        .endMetadata()
                        .build())
                .withNewMetadata().withResourceVersion("1").endMetadata()
                .build();

        String buildStatus = success ? "Complete" : "Fail";
        Build build = new BuildBuilder()
                .withNewMetadata().withResourceVersion("2").endMetadata()
                .withNewStatus().withPhase(buildStatus).endStatus()
                .build();

        if (!buildConfigExists) {
            mockServer.expect().get().withPath("/apis/build.openshift.io/v1/namespaces/test/buildconfigs/" + resourceName + s2iBuildNameSuffix).andReply(collector.record("build-config-check").andReturn
                    (404, "")).once();
            mockServer.expect().get().withPath("/apis/build.openshift.io/v1/namespaces/test/buildconfigs/" + resourceName + s2iBuildNameSuffix + "pullSecret").andReply(collector.record("build-config-check").andReturn
                    (404, "")).once();
            mockServer.expect().post().withPath("/apis/build.openshift.io/v1/namespaces/test/buildconfigs").andReply(collector.record("new-build-config").andReturn(201, bc)).once();
            if (bcSecret != null) {
                mockServer.expect().post().withPath("/apis/build.openshift.io/v1/namespaces/test/buildconfigs").andReply(collector.record("new-build-config").andReturn(201, bcSecret)).once();
            }
        } else {
            mockServer.expect().patch().withPath("/apis/build.openshift.io/v1/namespaces/test/buildconfigs/" + resourceName + s2iBuildNameSuffix).andReply(collector.record("patch-build-config").andReturn
                    (200, bc)).once();
            if (bcSecret != null) {
                mockServer.expect().patch().withPath("/apis/build.openshift.io/v1/namespaces/test/buildconfigs/" + resourceName + s2iBuildNameSuffix + "pullSecret").andReply(collector.record("patch-build-config").andReturn
                        (200, bcSecret)).once();
            }
        }
        mockServer.expect().get().withPath("/apis/build.openshift.io/v1/namespaces/test/buildconfigs/" + resourceName + s2iBuildNameSuffix).andReply(collector.record("build-config-check").andReturn(200,
                bc)).always();
        if (bcSecret != null) {
            mockServer.expect().get().withPath("/apis/build.openshift.io/v1/namespaces/test/buildconfigs/" + resourceName + s2iBuildNameSuffix + "pullSecret").andReply(collector.record("build-config-check").andReturn(200,
                    bcSecret)).always();
        }


        if (!imageStreamExists) {
            mockServer.expect().get().withPath("/apis/image.openshift.io/v1/namespaces/test/imagestreams/" + resourceName).andReturn(404, "").once();
        }
        mockServer.expect().get().withPath("/apis/image.openshift.io/v1/namespaces/test/imagestreams/" + resourceName).andReturn(200, imageStream).always();

        mockServer.expect().post().withPath("/apis/image.openshift.io/v1/namespaces/test/imagestreams").andReturn(201, imageStream).once();

        mockServer.expect().post().withPath("/apis/build.openshift.io/v1/namespaces/test/buildconfigs/" + resourceName + s2iBuildNameSuffix + "/instantiatebinary?name="+ resourceName + s2iBuildNameSuffix +"&namespace=test")
                .andReply(
                        collector.record("pushed")
                                .andReturn(201, imageStream))
                .once();

        mockServer.expect().get().withPath("/apis/build.openshift.io/v1/namespaces/test/builds").andReply(collector.record("check-build").andReturn(200, builds)).always();
        mockServer.expect().get().withPath("/apis/build.openshift.io/v1/namespaces/test/builds?labelSelector=openshift.io/build-config.name%3D" + resourceName + s2iBuildNameSuffix).andReturn(200, builds)
                .always();

        mockServer.expect().withPath("/apis/build.openshift.io/v1/namespaces/test/builds/" + resourceName).andReturn(200, build).always();
        mockServer.expect().withPath("/apis/build.openshift.io/v1/namespaces/test/builds?fieldSelector=metadata.name%3D" + resourceName + "&watch=true")
                .andUpgradeToWebSocket().open()
                .waitFor(buildDelay)
                .andEmit(new WatchEvent(build, "MODIFIED"))
                .done().always();

        return collector;
    }

    private BuildServiceConfig withBuildServiceConfig(BuildServiceConfig buildServiceConfig) {
        // @formatter:off
        new Expectations() {{
            jKubeServiceHub.getBuildServiceConfig(); result = buildServiceConfig;
        }};
        // @formatter:on
        return buildServiceConfig;
    }

    /**
     * Helper method to get root cause, pretty much like Apache's ExceptionUtils
     */
    private Throwable getRootCause(Throwable aThrowable) {
        Throwable cause, result = aThrowable;

        while((cause = result.getCause()) != null && cause != result) {
            result = cause;
        }
        return result;
    }

    private Boolean containsRequest(String path)   {
        try {
            OpenShiftMockServer mock = mockServer.getOpenShiftMockServer();
            int count = mock.getRequestCount();
            RecordedRequest request = null;
            while ( count-- > 0 ) {
                request = mock.takeRequest();
                if (request.getPath().contains(path)) {
                    return true;
                }
            }
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
        return false;
    }
}
