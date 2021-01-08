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
import io.fabric8.kubernetes.api.model.WatchEvent;
import io.fabric8.openshift.api.model.Build;
import io.fabric8.openshift.api.model.BuildBuilder;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.BuildConfigBuilder;
import io.fabric8.openshift.api.model.BuildStrategyBuilder;
import io.fabric8.openshift.api.model.ImageStream;
import io.fabric8.openshift.api.model.ImageStreamBuilder;
import io.fabric8.openshift.api.model.ImageStreamStatusBuilder;
import io.fabric8.openshift.api.model.NamedTagEventListBuilder;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.server.mock.OpenShiftServer;
import org.eclipse.jkube.kit.build.api.assembly.JKubeBuildTarArchiver;
import org.eclipse.jkube.kit.config.image.build.JKubeConfiguration;
import org.eclipse.jkube.kit.build.api.assembly.ArchiverCustomizer;
import org.eclipse.jkube.kit.config.image.RegistryConfig;
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
import mockit.Verifications;
import org.apache.commons.io.IOUtils;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class OpenshiftBuildServiceTest {

    private static final Logger LOG = LoggerFactory.getLogger(OpenshiftBuildServiceTest.class);

    private static final int MAX_TIMEOUT_RETRIES = 5;

    @Rule
    public final OpenShiftServer mockServer = new OpenShiftServer(false);

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mocked
    private JKubeServiceHub jKubeServiceHub;

    @Mocked
    private JKubeBuildTarArchiver tarArchiver;

    private String baseDir;

    private String projectName = "myapp";

    private File imageStreamFile = new File(baseDir, projectName);

    private KitLogger logger;

    private ImageConfiguration image;

    private BuildServiceConfig.BuildServiceConfigBuilder defaultConfig;

    private BuildServiceConfig.BuildServiceConfigBuilder defaultConfigSecret;

    @Before
    public void init() throws Exception {
        logger = new KitLogger.StdoutLogger();
        baseDir = temporaryFolder.newFolder("openshift-build-service").getAbsolutePath();
        final File dockerFile = new File(baseDir, "Docker.tar");
        dockerFile.createNewFile();

        imageStreamFile.delete();

        new Expectations() {{
            jKubeServiceHub.getDockerServiceHub().getArchiveService().createDockerBuildArchive(
                withAny(ImageConfiguration.class.cast(null)),
                withAny(JKubeConfiguration.class.cast(null))
            );
            result = dockerFile;
            minTimes = 0;
            jKubeServiceHub.getDockerServiceHub().getArchiveService().createDockerBuildArchive(
                withAny(null),
                withAny(null),
                withAny(null)
            );
            result = dockerFile;
            minTimes = 0;
            jKubeServiceHub.getConfiguration().getProject();
            result = jKubeServiceHub.getConfiguration().getProject();
            minTimes = 0;
            jKubeServiceHub.getConfiguration().getRegistryConfig();
            result = RegistryConfig.builder().build();
            minTimes = 0;
        }};

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

        defaultConfigSecret = BuildServiceConfig.builder()
                .buildDirectory(baseDir)
                .buildRecreateMode(BuildRecreateMode.none)
                .s2iBuildNameSuffix("-s2i-suffix2")
                .openshiftPullSecret("pullsecret-fabric8")
                .jKubeBuildStrategy(JKubeBuildStrategy.s2i);
    }

    @Test
    public void testSuccessfulBuild() throws Exception {
        retryInMockServer(() -> {
            BuildServiceConfig config = defaultConfig.build();
            // @formatter:off
            new Expectations() {{
                jKubeServiceHub.getBuildServiceConfig(); result = config;
            }};
            // @formatter:on
            WebServerEventCollector collector = createMockServer(config, true, 50, false, false);

            DefaultOpenShiftClient client = (DefaultOpenShiftClient) mockServer.getOpenshiftClient();
            LOG.info("Current write timeout is : {}", client.getHttpClient().writeTimeoutMillis());
            LOG.info("Current read timeout is : {}", client.getHttpClient().readTimeoutMillis());
            LOG.info("Retry on failure : {}", client.getHttpClient().retryOnConnectionFailure());
            OpenshiftBuildService service = new OpenshiftBuildService(client, logger, jKubeServiceHub);
            service.build(image);

            // we should Foadd a better way to assert that a certain call has been made
            assertTrue(mockServer.getMockServer().getRequestCount() > 8);
            collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed");
            assertEquals("{\"apiVersion\":\"build.openshift.io/v1\",\"kind\":\"BuildConfig\",\"metadata\":{\"name\":\"myapp-s2i-suffix2\"},\"spec\":{\"output\":{\"to\":{\"kind\":\"ImageStreamTag\",\"name\":\"myapp:latest\"}},\"source\":{\"type\":\"Binary\"},\"strategy\":{\"sourceStrategy\":{\"forcePull\":false,\"from\":{\"kind\":\"DockerImage\",\"name\":\"myapp\"}},\"type\":\"Source\"}}}", collector.getBodies().get(1));
            collector.assertEventsNotRecorded("patch-build-config");
        });
    }

    @Test
    public void testSuccessfulBuildNoS2iSuffix() throws Exception {
        retryInMockServer(() -> {
            BuildServiceConfig config = defaultConfig
                    .s2iBuildNameSuffix(null)
                    .build();
            // @formatter:on
            new Expectations() {{
                jKubeServiceHub.getBuildServiceConfig(); result = config;
            }};
            WebServerEventCollector collector = createMockServer(
                config, true, 50, false, false);

            DefaultOpenShiftClient client = (DefaultOpenShiftClient) mockServer.getOpenshiftClient();
            LOG.info("Current write timeout is : {}", client.getHttpClient().writeTimeoutMillis());
            LOG.info("Current read timeout is : {}", client.getHttpClient().readTimeoutMillis());
            LOG.info("Retry on failure : {}", client.getHttpClient().retryOnConnectionFailure());
            OpenshiftBuildService service = new OpenshiftBuildService(client, logger, jKubeServiceHub);
            service.build(image);

            // we should Foadd a better way to assert that a certain call has been made
            assertTrue(mockServer.getMockServer().getRequestCount() > 8);
            collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed");
            assertEquals("{\"apiVersion\":\"build.openshift.io/v1\",\"kind\":\"BuildConfig\",\"metadata\":{\"name\":\"myapp-s2i\"},\"spec\":{\"output\":{\"to\":{\"kind\":\"ImageStreamTag\",\"name\":\"myapp:latest\"}},\"source\":{\"type\":\"Binary\"},\"strategy\":{\"sourceStrategy\":{\"forcePull\":false,\"from\":{\"kind\":\"DockerImage\",\"name\":\"myapp\"}},\"type\":\"Source\"}}}", collector.getBodies().get(1));
            collector.assertEventsNotRecorded("patch-build-config");
        });
    }

    @Test
    public void testDockerBuild() throws Exception {
        retryInMockServer(() -> {
            BuildServiceConfig dockerConfig = BuildServiceConfig.builder()
                    .buildDirectory(baseDir)
                    .buildRecreateMode(BuildRecreateMode.none)
                    .s2iBuildNameSuffix("-docker")
                    .jKubeBuildStrategy(JKubeBuildStrategy.docker).build();
            // @formatter:on
            new Expectations() {{
                jKubeServiceHub.getBuildServiceConfig(); result = dockerConfig;
            }};
            // @formatter:off
            WebServerEventCollector collector = createMockServer(dockerConfig, true, 50,
                    false, false);

            DefaultOpenShiftClient client = (DefaultOpenShiftClient) mockServer.getOpenshiftClient();
            OpenshiftBuildService service = new OpenshiftBuildService(client, logger, jKubeServiceHub);
            service.build(image);

            assertTrue(mockServer.getMockServer().getRequestCount() > 8);
            collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed");
            assertEquals("{\"apiVersion\":\"build.openshift.io/v1\",\"kind\":\"BuildConfig\",\"metadata\":{\"name\":\"myapp-docker\"},\"spec\":{\"output\":{\"to\":{\"kind\":\"ImageStreamTag\",\"name\":\"myapp:latest\"}},\"source\":{\"type\":\"Binary\"},\"strategy\":{\"dockerStrategy\":{\"from\":{\"kind\":\"DockerImage\",\"name\":\"myapp\"},\"noCache\":false},\"type\":\"Docker\"}}}", collector.getBodies().get(1));
            collector.assertEventsNotRecorded("patch-build-config");
        });
    }

    @Test
    public void testDockerBuildNoS2iSuffix() throws Exception {
        retryInMockServer(() -> {
            final BuildServiceConfig dockerConfig = BuildServiceConfig.builder()
                    .buildDirectory(baseDir)
                    .buildRecreateMode(BuildRecreateMode.none)
                    .jKubeBuildStrategy(JKubeBuildStrategy.docker)
                    .build();
            // @formatter:on
            new Expectations() {{
                jKubeServiceHub.getBuildServiceConfig(); result = dockerConfig;
            }};
            // @formatter:off
            WebServerEventCollector collector = createMockServer(dockerConfig, true, 50,
                    false, false);

            DefaultOpenShiftClient client = (DefaultOpenShiftClient) mockServer.getOpenshiftClient();
            OpenshiftBuildService service = new OpenshiftBuildService(client, logger, jKubeServiceHub);
            service.build(image);

            assertTrue(mockServer.getMockServer().getRequestCount() > 8);
            collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed");
            assertEquals("{\"apiVersion\":\"build.openshift.io/v1\",\"kind\":\"BuildConfig\",\"metadata\":{\"name\":\"myapp\"},\"spec\":{\"output\":{\"to\":{\"kind\":\"ImageStreamTag\",\"name\":\"myapp:latest\"}},\"source\":{\"type\":\"Binary\"},\"strategy\":{\"dockerStrategy\":{\"from\":{\"kind\":\"DockerImage\",\"name\":\"myapp\"},\"noCache\":false},\"type\":\"Docker\"}}}", collector.getBodies().get(1));
            collector.assertEventsNotRecorded("patch-build-config");
        });
    }

    @Test
    public void testDockerBuildFromExt() throws Exception {
        retryInMockServer(() -> {
            BuildServiceConfig dockerConfig = BuildServiceConfig.builder()
                    .buildDirectory(baseDir)
                    .buildRecreateMode(BuildRecreateMode.none)
                    .s2iBuildNameSuffix("-docker")
                    .jKubeBuildStrategy(JKubeBuildStrategy.docker)
                    .build();
            // @formatter:on
            new Expectations() {{
                jKubeServiceHub.getBuildServiceConfig(); result = dockerConfig;
            }};
            // @formatter:off
            WebServerEventCollector collector = createMockServer(dockerConfig, true, 50,
                    false, false);

            DefaultOpenShiftClient client = (DefaultOpenShiftClient) mockServer.getOpenshiftClient();
            OpenshiftBuildService service = new OpenshiftBuildService(client, logger, jKubeServiceHub);
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

            assertTrue(mockServer.getMockServer().getRequestCount() > 8);
            collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed");
            assertEquals("{\"apiVersion\":\"build.openshift.io/v1\",\"kind\":\"BuildConfig\",\"metadata\":{\"name\":\"myapp-docker\"},\"spec\":{\"output\":{\"to\":{\"kind\":\"ImageStreamTag\",\"name\":\"myapp:latest\"}},\"source\":{\"type\":\"Binary\"},\"strategy\":{\"dockerStrategy\":{\"from\":{\"kind\":\"ImageStreamTag\",\"name\":\"app:1.2-1\",\"namespace\":\"my-project\"},\"noCache\":true},\"type\":\"Docker\"}}}", collector.getBodies().get(1));
            collector.assertEventsNotRecorded("patch-build-config");
        });
    }

    @Test
    public void testSuccessfulBuildSecret() throws Exception {
        retryInMockServer(() -> {
            BuildServiceConfig config = defaultConfigSecret.build();
            // @formatter:on
            new Expectations() {{
                jKubeServiceHub.getBuildServiceConfig(); result = config;
            }};
            // @formatter:off
            WebServerEventCollector collector = createMockServer(config, true, 50, false, false);

            DefaultOpenShiftClient client = (DefaultOpenShiftClient) mockServer.getOpenshiftClient();
            LOG.info("Current write timeout is : {}", client.getHttpClient().writeTimeoutMillis());
            LOG.info("Current read timeout is : {}", client.getHttpClient().readTimeoutMillis());
            LOG.info("Retry on failure : {}", client.getHttpClient().retryOnConnectionFailure());
            OpenshiftBuildService service = new OpenshiftBuildService(client, logger, jKubeServiceHub);
            service.build(image);

            // we should Foadd a better way to assert that a certain call has been made
            assertTrue(mockServer.getMockServer().getRequestCount() > 8);
            collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed");
            collector.assertEventsNotRecorded("patch-build-config");
        });
    }

    @Test(expected = JKubeServiceException.class)
    public void testFailedBuild() throws Exception {
        BuildServiceConfig config = defaultConfig.build();
        // @formatter:on
        new Expectations() {{
            jKubeServiceHub.getBuildServiceConfig(); result = config;
        }};
        // @formatter:off

        OpenShiftClient client = mockServer.getOpenshiftClient();
        OpenshiftBuildService service = new OpenshiftBuildService(client, logger, jKubeServiceHub);
        service.build(image);
    }

    @Test(expected = JKubeServiceException.class)
    public void testFailedBuildSecret() throws Exception {
        BuildServiceConfig config = defaultConfigSecret.build();
        // @formatter:on
        new Expectations() {{
            jKubeServiceHub.getBuildServiceConfig(); result = config;
        }};
        // @formatter:off

        OpenShiftClient client = mockServer.getOpenshiftClient();
        OpenshiftBuildService service = new OpenshiftBuildService(client, logger, jKubeServiceHub);
        service.build(image);
    }

    @Test
    public void testSuccessfulSecondBuild() throws Exception {
        retryInMockServer(() -> {
            BuildServiceConfig config = defaultConfig.build();
            // @formatter:on
            new Expectations() {{
                jKubeServiceHub.getBuildServiceConfig(); result = config;
            }};
            // @formatter:off
            WebServerEventCollector collector = createMockServer(config, true, 50, true, true);

            OpenShiftClient client = mockServer.getOpenshiftClient();
            OpenshiftBuildService service = new OpenshiftBuildService(client, logger, jKubeServiceHub);
            service.build(image);

            assertTrue(mockServer.getMockServer().getRequestCount() > 8);
            collector.assertEventsRecordedInOrder("build-config-check", "patch-build-config", "pushed");
            collector.assertEventsNotRecorded("new-build-config");
        });
    }

    @Test
    public void checkTarPackage() throws Exception {
        retryInMockServer(() -> {
            BuildServiceConfig config = defaultConfig.build();
            // @formatter:on
            new Expectations() {{
                jKubeServiceHub.getBuildServiceConfig(); result = config;
            }};
            // @formatter:off

            OpenShiftClient client = mockServer.getOpenshiftClient();
            final OpenshiftBuildService service = new OpenshiftBuildService(client, logger, jKubeServiceHub);

            ImageConfiguration imageWithEnv = image.toBuilder()
                    .build(image.getBuildConfiguration().toBuilder()
                            .env(Collections.singletonMap("FOO", "BAR"))
                            .build()
                    ).build();

            service.createBuildArchive(imageWithEnv);

            final List<ArchiverCustomizer> customizer = new LinkedList<>();
            new Verifications() {{
                jKubeServiceHub.getDockerServiceHub().getArchiveService()
                    .createDockerBuildArchive(withInstanceOf(ImageConfiguration.class), withInstanceOf(JKubeConfiguration.class), withCapture(customizer));

                assertTrue(customizer.size() == 1);
            }};

            customizer.get(0).customize(tarArchiver);

            final List<File> file = new LinkedList<>();
            new Verifications() {{
                String path;
                tarArchiver.includeFile(withCapture(file), path = withCapture());

                assertEquals(".s2i/environment", path);
            }};

            assertEquals(1, file.size());
            List<String> lines;
            try (FileReader reader = new FileReader(file.get(0))) {
                lines = IOUtils.readLines(reader);
            }
            assertTrue(lines.contains("FOO=BAR"));
        });
    }

    @Test
    public void checkTarPackageSecret() throws Exception {
        retryInMockServer(() -> {
            BuildServiceConfig config = defaultConfigSecret.build();
            // @formatter:on
            new Expectations() {{
                jKubeServiceHub.getBuildServiceConfig(); result = config;
            }};
            // @formatter:off

            OpenShiftClient client = mockServer.getOpenshiftClient();
            final OpenshiftBuildService service = new OpenshiftBuildService(client, logger, jKubeServiceHub);

            ImageConfiguration imageWithEnv = image.toBuilder()
                    .build(image.getBuildConfiguration().toBuilder()
                            .env(Collections.singletonMap("FOO", "BAR"))
                            .build()
                    ).build();

            service.createBuildArchive(imageWithEnv);

            final List<ArchiverCustomizer> customizer = new LinkedList<>();
            new Verifications() {{
                jKubeServiceHub.getDockerServiceHub().getArchiveService()
                    .createDockerBuildArchive(withInstanceOf(ImageConfiguration.class), withInstanceOf(JKubeConfiguration.class), withCapture(customizer));

                assertEquals(1, customizer.size());
            }};

            customizer.get(0).customize(tarArchiver);

            final List<File> file = new LinkedList<>();
            new Verifications() {{
                String path;
                tarArchiver.includeFile(withCapture(file), path = withCapture());

                assertEquals(".s2i/environment", path);
            }};

            assertEquals(1, file.size());
            List<String> lines;
            try (FileReader reader = new FileReader(file.get(0))) {
                lines = IOUtils.readLines(reader);
            }
            assertTrue(lines.contains("FOO=BAR"));
        });
    }

    @Test
    public void testBuildConfigResourceConfig() throws Exception {
        retryInMockServer(() -> {
            Map<String, String> limitsMap = new HashMap<>();
            limitsMap.put("cpu", "100m");
            limitsMap.put("memory", "256Mi");

            BuildServiceConfig config = defaultConfig
                    .resourceConfig(ResourceConfig.builder()
                        .openshiftBuildConfig(OpenshiftBuildConfig.builder().limits(limitsMap).build())
                        .build()
                    ).build();
            // @formatter:on
            new Expectations() {{
                jKubeServiceHub.getBuildServiceConfig(); result = config;
            }};
            // @formatter:off

            OpenShiftClient client = mockServer.getOpenshiftClient();
            final OpenshiftBuildService service = new OpenshiftBuildService(client, logger, jKubeServiceHub);

            ImageConfiguration imageWithEnv = image.toBuilder()
                    .build(image.getBuildConfiguration().toBuilder()
                            .env(Collections.singletonMap("FOO", "BAR"))
                            .build()
                    ).build();

            KubernetesListBuilder builder = new KubernetesListBuilder();
            service.createBuildArchive(imageWithEnv);
            service.updateOrCreateBuildConfig(config, client, builder, imageWithEnv, null);
            BuildConfig buildConfig = (BuildConfig) builder.buildFirstItem();
            assertNotNull(buildConfig);
            assertNotNull(buildConfig.getSpec().getResources());
            assertEquals("256", buildConfig.getSpec().getResources().getLimits().get("memory").getAmount());
            assertEquals("Mi", buildConfig.getSpec().getResources().getLimits().get("memory").getFormat());
            assertEquals("100", buildConfig.getSpec().getResources().getLimits().get("cpu").getAmount());
            assertEquals("m", buildConfig.getSpec().getResources().getLimits().get("cpu").getFormat());
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
        WebServerEventCollector collector = new WebServerEventCollector();

        final String s2iBuildNameSuffix = Optional
                .ofNullable(config.getS2iBuildNameSuffix())
                .orElseGet(() -> config.getJKubeBuildStrategy() == JKubeBuildStrategy.s2i ?
                        "-s2i" : "");

        BuildConfig bc = new BuildConfigBuilder()
                .withNewMetadata()
                .withName(projectName + s2iBuildNameSuffix)
                .endMetadata()
                .withNewSpec()
                .endSpec()
                .build();

        BuildConfig bcSecret = null;
        if (config.getOpenshiftPullSecret() != null) {
            bcSecret = new BuildConfigBuilder()
                    .withNewMetadata()
                    .withName(projectName + s2iBuildNameSuffix + "pullSecret")
                    .endMetadata()
                    .withNewSpec()
                    .withStrategy(new BuildStrategyBuilder().withType("Docker")
                            .withNewDockerStrategy()
                            .withNewPullSecret(config.getOpenshiftPullSecret())
                            .endDockerStrategy().build())
                    .endSpec()
                    .build();
        }

        ImageStream imageStream = new ImageStreamBuilder()
                .withNewMetadata()
                .withName(projectName)
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
                        .withName(projectName)
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
            mockServer.expect().get().withPath("/apis/build.openshift.io/v1/namespaces/test/buildconfigs/" + projectName + s2iBuildNameSuffix).andReply(collector.record("build-config-check").andReturn
                    (404, "")).once();
            mockServer.expect().get().withPath("/apis/build.openshift.io/v1/namespaces/test/buildconfigs/" + projectName + s2iBuildNameSuffix + "pullSecret").andReply(collector.record("build-config-check").andReturn
                    (404, "")).once();
            mockServer.expect().post().withPath("/apis/build.openshift.io/v1/namespaces/test/buildconfigs").andReply(collector.record("new-build-config").andReturn(201, bc)).once();
            if (bcSecret != null) {
                mockServer.expect().post().withPath("/apis/build.openshift.io/v1/namespaces/test/buildconfigs").andReply(collector.record("new-build-config").andReturn(201, bcSecret)).once();
            }
        } else {
            mockServer.expect().patch().withPath("/apis/build.openshift.io/v1/namespaces/test/buildconfigs/" + projectName + s2iBuildNameSuffix).andReply(collector.record("patch-build-config").andReturn
                    (200, bc)).once();
            if (bcSecret != null) {
                mockServer.expect().patch().withPath("/apis/build.openshift.io/v1/namespaces/test/buildconfigs/" + projectName + s2iBuildNameSuffix + "pullSecret").andReply(collector.record("patch-build-config").andReturn
                        (200, bcSecret)).once();
            }
        }
        mockServer.expect().get().withPath("/apis/build.openshift.io/v1/namespaces/test/buildconfigs/" + projectName + s2iBuildNameSuffix).andReply(collector.record("build-config-check").andReturn(200,
                bc)).always();
        if (bcSecret != null) {
            mockServer.expect().get().withPath("/apis/build.openshift.io/v1/namespaces/test/buildconfigs/" + projectName + s2iBuildNameSuffix + "pullSecret").andReply(collector.record("build-config-check").andReturn(200,
                    bcSecret)).always();
        }


        if (!imageStreamExists) {
            mockServer.expect().get().withPath("/apis/image.openshift.io/v1/namespaces/test/imagestreams/" + projectName).andReturn(404, "").once();
        }
        mockServer.expect().get().withPath("/apis/image.openshift.io/v1/namespaces/test/imagestreams/" + projectName).andReturn(200, imageStream).always();

        mockServer.expect().post().withPath("/apis/image.openshift.io/v1/namespaces/test/imagestreams").andReturn(201, imageStream).once();

        mockServer.expect().post().withPath("/apis/build.openshift.io/v1/namespaces/test/buildconfigs/" + projectName + s2iBuildNameSuffix + "/instantiatebinary?name="+ projectName + s2iBuildNameSuffix +"&namespace=test")
                .andReply(
                        collector.record("pushed")
                                .andReturn(201, imageStream))
                .once();

        mockServer.expect().get().withPath("/apis/build.openshift.io/v1/namespaces/test/builds").andReply(collector.record("check-build").andReturn(200, builds)).always();
        mockServer.expect().get().withPath("/apis/build.openshift.io/v1/namespaces/test/builds?labelSelector=openshift.io/build-config.name%3D" + projectName + s2iBuildNameSuffix).andReturn(200, builds)
                .always();

        mockServer.expect().withPath("/apis/build.openshift.io/v1/namespaces/test/builds/" + projectName).andReturn(200, build).always();
        mockServer.expect().withPath("/apis/build.openshift.io/v1/namespaces/test/builds?fieldSelector=metadata.name%3D" + projectName + "&watch=true")
                .andUpgradeToWebSocket().open()
                .waitFor(buildDelay)
                .andEmit(new WatchEvent(build, "MODIFIED"))
                .done().always();

        return collector;
    }

    /**
     * Helper method to get root cause, pretty much like Apache's ExceptionUtils
     *
     * @param aThrowable
     * @return
     */
    private Throwable getRootCause(Throwable aThrowable) {
        Throwable cause, result = aThrowable;

        while((cause = result.getCause()) != null && cause != result) {
            result = cause;
        }
        return result;
    }

}
