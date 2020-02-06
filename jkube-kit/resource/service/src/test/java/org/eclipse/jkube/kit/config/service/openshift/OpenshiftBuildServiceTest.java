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
import io.fabric8.openshift.client.server.mock.OpenShiftMockServer;
import org.eclipse.jkube.kit.build.maven.MavenBuildContext;
import org.eclipse.jkube.kit.build.maven.assembly.ArchiverCustomizer;
import org.eclipse.jkube.kit.build.maven.config.MavenBuildConfiguration;
import org.eclipse.jkube.kit.build.service.docker.ArchiveService;
import org.eclipse.jkube.kit.build.service.docker.ImageConfiguration;
import org.eclipse.jkube.kit.build.service.docker.RegistryService;
import org.eclipse.jkube.kit.build.service.docker.ServiceHub;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.build.OpenShiftBuildStrategy;
import org.eclipse.jkube.kit.config.resource.BuildRecreateMode;
import org.eclipse.jkube.kit.config.service.BuildService;
import org.eclipse.jkube.kit.config.service.JkubeServiceException;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import org.apache.commons.io.IOUtils;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.tar.TarArchiver;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class OpenshiftBuildServiceTest {

    private static final Logger LOG = LoggerFactory.getLogger(OpenshiftBuildServiceTest.class);

    private static final int MAX_TIMEOUT_RETRIES = 5;

    private String baseDir = "target/test-files/openshift-build-service";

    private String projectName = "myapp";

    private File imageStreamFile = new File(baseDir, projectName);

    @Mocked
    private ServiceHub dockerServiceHub;

    @Mocked
    private ArchiveService archiveService;

    @Mocked
    private TarArchiver tarArchiver;

    @Mocked
    private KitLogger logger;

    @Mocked
    private MavenBuildContext dockerMojoParameters;

    @Mocked
    private MavenProject project;

    private ImageConfiguration image;

    private BuildService.BuildServiceConfig.Builder defaultConfig;

    private BuildService.BuildServiceConfig.Builder defaultConfigSecret;

    @Before
    public void init() throws Exception {
        final File dockerFile = new File(baseDir, "Docker.tar");
        dockerFile.getParentFile().mkdirs();
        dockerFile.createNewFile();

        imageStreamFile.delete();

        new Expectations() {{
            dockerServiceHub.getArchiveService();
            result = archiveService;

            archiveService.createDockerBuildArchive(withAny(ImageConfiguration.class.cast(null)), withAny(MavenBuildContext.class.cast(null)));
            result = dockerFile;
            minTimes = 0;

            project.getArtifact();
            result = "myapp";
            minTimes = 0;

            dockerMojoParameters.getProject();
            result = project;
            minTimes = 0;
        }};

        image = new ImageConfiguration.Builder()
                .name(projectName)
                .buildConfig(new MavenBuildConfiguration.Builder()
                        .from(projectName)
                        .build()
                ).build();
        final org.eclipse.jkube.kit.build.service.docker.BuildService.BuildContext context = new org.eclipse.jkube.kit.build.service.docker.BuildService.BuildContext.Builder()
                .registryConfig( new RegistryService.RegistryConfig.Builder().build())
                .build();


        defaultConfig = new BuildService.BuildServiceConfig.Builder()
                .buildDirectory(baseDir)
                .buildRecreateMode(BuildRecreateMode.none)
                .dockerBuildContext(context)
                .s2iBuildNameSuffix("-s2i-suffix2")
                .openshiftBuildStrategy(OpenShiftBuildStrategy.s2i)
                .dockerMavenBuildContext(dockerMojoParameters);

        defaultConfigSecret = new BuildService.BuildServiceConfig.Builder()
                .buildDirectory(baseDir)
                .buildRecreateMode(BuildRecreateMode.none)
                .dockerBuildContext(context)
                .s2iBuildNameSuffix("-s2i-suffix2")
                .openshiftPullSecret("pullsecret-fabric8")
                .openshiftBuildStrategy(OpenShiftBuildStrategy.s2i)
                .dockerMavenBuildContext(dockerMojoParameters);
    }

    @Test
    public void testSuccessfulBuild() throws Exception {
        retryInMockServer(() -> {
            BuildService.BuildServiceConfig config = defaultConfig.build();
            WebServerEventCollector<OpenShiftMockServer> collector = createMockServer(config, true, 50, false, false);
            OpenShiftMockServer mockServer = collector.getMockServer();

            DefaultOpenShiftClient client = (DefaultOpenShiftClient) mockServer.createOpenShiftClient();
            LOG.info("Current write timeout is : {}", client.getHttpClient().writeTimeoutMillis());
            LOG.info("Current read timeout is : {}", client.getHttpClient().readTimeoutMillis());
            LOG.info("Retry on failure : {}", client.getHttpClient().retryOnConnectionFailure());
            OpenshiftBuildService service = new OpenshiftBuildService(client, logger, dockerServiceHub, config);
            service.build(image);

            // we should Foadd a better way to assert that a certain call has been made
            assertTrue(mockServer.getRequestCount() > 8);
            collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed");
            assertEquals("{\"apiVersion\":\"build.openshift.io/v1\",\"kind\":\"BuildConfig\",\"metadata\":{\"name\":\"myapp-s2i-suffix2\"},\"spec\":{\"output\":{\"to\":{\"kind\":\"ImageStreamTag\",\"name\":\"myapp:latest\"}},\"source\":{\"type\":\"Binary\"},\"strategy\":{\"sourceStrategy\":{\"forcePull\":false,\"from\":{\"kind\":\"DockerImage\",\"name\":\"myapp\"}},\"type\":\"Source\"},\"triggers\":[]}}", collector.getBodies().get(1));
            collector.assertEventsNotRecorded("patch-build-config");
        });
    }

    @Test
    public void testSuccessfulBuildNoS2iSuffix() throws Exception {
        retryInMockServer(() -> {
            BuildService.BuildServiceConfig config = defaultConfig
                    .s2iBuildNameSuffix(null)
                    .build();
            WebServerEventCollector<OpenShiftMockServer> collector = createMockServer(config, true, 50, false, false);
            OpenShiftMockServer mockServer = collector.getMockServer();

            DefaultOpenShiftClient client = (DefaultOpenShiftClient) mockServer.createOpenShiftClient();
            LOG.info("Current write timeout is : {}", client.getHttpClient().writeTimeoutMillis());
            LOG.info("Current read timeout is : {}", client.getHttpClient().readTimeoutMillis());
            LOG.info("Retry on failure : {}", client.getHttpClient().retryOnConnectionFailure());
            OpenshiftBuildService service = new OpenshiftBuildService(client, logger, dockerServiceHub, config);
            service.build(image);

            // we should Foadd a better way to assert that a certain call has been made
            assertTrue(mockServer.getRequestCount() > 8);
            collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed");
            assertEquals("{\"apiVersion\":\"build.openshift.io/v1\",\"kind\":\"BuildConfig\",\"metadata\":{\"name\":\"myapp-s2i\"},\"spec\":{\"output\":{\"to\":{\"kind\":\"ImageStreamTag\",\"name\":\"myapp:latest\"}},\"source\":{\"type\":\"Binary\"},\"strategy\":{\"sourceStrategy\":{\"forcePull\":false,\"from\":{\"kind\":\"DockerImage\",\"name\":\"myapp\"}},\"type\":\"Source\"},\"triggers\":[]}}", collector.getBodies().get(1));
            collector.assertEventsNotRecorded("patch-build-config");
        });
    }

    @Test
    public void testDockerBuild() throws Exception {
        retryInMockServer(() -> {
            final org.eclipse.jkube.kit.build.service.docker.BuildService.BuildContext context
                    = new org.eclipse.jkube.kit.build.service.docker.BuildService.BuildContext.Builder()
                    .registryConfig(new RegistryService.RegistryConfig.Builder().build())
                    .build();

            BuildService.BuildServiceConfig.Builder dockerConfig = new BuildService.BuildServiceConfig.Builder()
                    .buildDirectory(baseDir)
                    .dockerBuildContext(context)
                    .buildRecreateMode(BuildRecreateMode.none)
                    .s2iBuildNameSuffix("-docker")
                    .openshiftBuildStrategy(OpenShiftBuildStrategy.docker)
                    .dockerMavenBuildContext(dockerMojoParameters);

            BuildService.BuildServiceConfig config = dockerConfig.build();
            WebServerEventCollector<OpenShiftMockServer> collector = createMockServer(config, true, 50,
                    false, false);
            OpenShiftMockServer mockServer = collector.getMockServer();

            DefaultOpenShiftClient client = (DefaultOpenShiftClient) mockServer.createOpenShiftClient();
            OpenshiftBuildService service = new OpenshiftBuildService(client, logger, dockerServiceHub, config);
            service.build(image);

            assertTrue(mockServer.getRequestCount() > 8);
            collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed");
            assertEquals("{\"apiVersion\":\"build.openshift.io/v1\",\"kind\":\"BuildConfig\",\"metadata\":{\"name\":\"myapp-docker\"},\"spec\":{\"output\":{\"to\":{\"kind\":\"ImageStreamTag\",\"name\":\"myapp:latest\"}},\"source\":{\"type\":\"Binary\"},\"strategy\":{\"dockerStrategy\":{\"from\":{\"kind\":\"DockerImage\",\"name\":\"myapp\"}},\"type\":\"Docker\"},\"triggers\":[]}}", collector.getBodies().get(1));
            collector.assertEventsNotRecorded("patch-build-config");
        });
    }

    @Test
    public void testDockerBuildNoS2iSuffix() throws Exception {
        retryInMockServer(() -> {
            final org.eclipse.jkube.kit.build.service.docker.BuildService.BuildContext context = new org.eclipse.jkube.kit.build.service.docker.BuildService.BuildContext.Builder()
                    .registryConfig(new RegistryService.RegistryConfig.Builder().build())
                    .build();
            final BuildService.BuildServiceConfig.Builder dockerConfig = new BuildService.BuildServiceConfig.Builder()
                    .buildDirectory(baseDir)
                    .dockerBuildContext(context)
                    .buildRecreateMode(BuildRecreateMode.none)
                    .openshiftBuildStrategy(OpenShiftBuildStrategy.docker)
                    .dockerMavenBuildContext(dockerMojoParameters);
            final BuildService.BuildServiceConfig config = dockerConfig.build();
            WebServerEventCollector<OpenShiftMockServer> collector = createMockServer(config, true, 50,
                    false, false);
            OpenShiftMockServer mockServer = collector.getMockServer();

            DefaultOpenShiftClient client = (DefaultOpenShiftClient) mockServer.createOpenShiftClient();
            OpenshiftBuildService service = new OpenshiftBuildService(client, logger, dockerServiceHub, config);
            service.build(image);

            assertTrue(mockServer.getRequestCount() > 8);
            collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed");
            assertEquals("{\"apiVersion\":\"build.openshift.io/v1\",\"kind\":\"BuildConfig\",\"metadata\":{\"name\":\"myapp\"},\"spec\":{\"output\":{\"to\":{\"kind\":\"ImageStreamTag\",\"name\":\"myapp:latest\"}},\"source\":{\"type\":\"Binary\"},\"strategy\":{\"dockerStrategy\":{\"from\":{\"kind\":\"DockerImage\",\"name\":\"myapp\"}},\"type\":\"Docker\"},\"triggers\":[]}}", collector.getBodies().get(1));
            collector.assertEventsNotRecorded("patch-build-config");
        });
    }

    @Test
    public void testDockerBuildFromExt() throws Exception {
        retryInMockServer(() -> {
            final org.eclipse.jkube.kit.build.service.docker.BuildService.BuildContext context
                    = new org.eclipse.jkube.kit.build.service.docker.BuildService.BuildContext.Builder()
                    .registryConfig(new RegistryService.RegistryConfig.Builder().build())
                    .build();

            BuildService.BuildServiceConfig.Builder dockerConfig = new BuildService.BuildServiceConfig.Builder()
                    .buildDirectory(baseDir)
                    .dockerBuildContext(context)
                    .buildRecreateMode(BuildRecreateMode.none)
                    .s2iBuildNameSuffix("-docker")
                    .openshiftBuildStrategy(OpenShiftBuildStrategy.docker)
                    .dockerMavenBuildContext(dockerMojoParameters);

            BuildService.BuildServiceConfig config = dockerConfig.build();
            WebServerEventCollector<OpenShiftMockServer> collector = createMockServer(config, true, 50,
                    false, false);
            OpenShiftMockServer mockServer = collector.getMockServer();

            DefaultOpenShiftClient client = (DefaultOpenShiftClient) mockServer.createOpenShiftClient();
            OpenshiftBuildService service = new OpenshiftBuildService(client, logger, dockerServiceHub, config);
            Map<String,String> fromExt = ImmutableMap.of("name", "app:1.2-1",
                    "kind", "ImageStreamTag",
                    "namespace", "my-project");
            ImageConfiguration fromExtImage = new ImageConfiguration.Builder()
                    .name(projectName)
                    .buildConfig(new MavenBuildConfiguration.Builder()
                            .fromExt(fromExt)
                            .nocache(Boolean.TRUE)
                            .build()
                    ).build();

            service.build(fromExtImage);

            assertTrue(mockServer.getRequestCount() > 8);
            collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed");
            assertEquals("{\"apiVersion\":\"build.openshift.io/v1\",\"kind\":\"BuildConfig\",\"metadata\":{\"name\":\"myapp-docker\"},\"spec\":{\"output\":{\"to\":{\"kind\":\"ImageStreamTag\",\"name\":\"myapp:latest\"}},\"source\":{\"type\":\"Binary\"},\"strategy\":{\"dockerStrategy\":{\"from\":{\"kind\":\"ImageStreamTag\",\"name\":\"app:1.2-1\",\"namespace\":\"my-project\"},\"noCache\":true},\"type\":\"Docker\"},\"triggers\":[]}}", collector.getBodies().get(1));
            collector.assertEventsNotRecorded("patch-build-config");
        });
    }

    @Test
    public void testSuccessfulBuildSecret() throws Exception {
        retryInMockServer(() -> {
            BuildService.BuildServiceConfig config = defaultConfigSecret.build();
            WebServerEventCollector<OpenShiftMockServer> collector = createMockServer(config, true, 50, false, false);
            OpenShiftMockServer mockServer = collector.getMockServer();

            DefaultOpenShiftClient client = (DefaultOpenShiftClient) mockServer.createOpenShiftClient();
            LOG.info("Current write timeout is : {}", client.getHttpClient().writeTimeoutMillis());
            LOG.info("Current read timeout is : {}", client.getHttpClient().readTimeoutMillis());
            LOG.info("Retry on failure : {}", client.getHttpClient().retryOnConnectionFailure());
            OpenshiftBuildService service = new OpenshiftBuildService(client, logger, dockerServiceHub, config);
            service.build(image);

            // we should Foadd a better way to assert that a certain call has been made
            assertTrue(mockServer.getRequestCount() > 8);
            collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed");
            collector.assertEventsNotRecorded("patch-build-config");
        });
    }

    @Test(expected = JkubeServiceException.class)
    public void testFailedBuild() throws Exception {
        BuildService.BuildServiceConfig config = defaultConfig.build();
        WebServerEventCollector<OpenShiftMockServer> collector = createMockServer(config, false, 50, false, false);
        OpenShiftMockServer mockServer = collector.getMockServer();

        OpenShiftClient client = mockServer.createOpenShiftClient();
        OpenshiftBuildService service = new OpenshiftBuildService(client, logger, dockerServiceHub, config);
        service.build(image);
    }

    @Test(expected = JkubeServiceException.class)
    public void testFailedBuildSecret() throws Exception {
        BuildService.BuildServiceConfig config = defaultConfigSecret.build();
        WebServerEventCollector<OpenShiftMockServer> collector = createMockServer(config, false, 50, false, false);
        OpenShiftMockServer mockServer = collector.getMockServer();

        OpenShiftClient client = mockServer.createOpenShiftClient();
        OpenshiftBuildService service = new OpenshiftBuildService(client, logger, dockerServiceHub, config);
        service.build(image);
    }

    @Test
    public void testSuccessfulSecondBuild() throws Exception {
        retryInMockServer(() -> {
            BuildService.BuildServiceConfig config = defaultConfig.build();
            WebServerEventCollector<OpenShiftMockServer> collector = createMockServer(config, true, 50, true, true);
            OpenShiftMockServer mockServer = collector.getMockServer();

            OpenShiftClient client = mockServer.createOpenShiftClient();
            OpenshiftBuildService service = new OpenshiftBuildService(client, logger, dockerServiceHub, config);
            service.build(image);

            assertTrue(mockServer.getRequestCount() > 8);
            collector.assertEventsRecordedInOrder("build-config-check", "patch-build-config", "pushed");
            collector.assertEventsNotRecorded("new-build-config");
        });
    }

    @Test
    public void checkTarPackage() throws Exception {
        retryInMockServer(() -> {
            BuildService.BuildServiceConfig config = defaultConfig.build();
            WebServerEventCollector<OpenShiftMockServer> collector = createMockServer(config, true, 50, true, true);
            OpenShiftMockServer mockServer = collector.getMockServer();

            OpenShiftClient client = mockServer.createOpenShiftClient();
            final OpenshiftBuildService service = new OpenshiftBuildService(client, logger, dockerServiceHub, config);

            ImageConfiguration imageWithEnv = new ImageConfiguration.Builder(image)
                    .buildConfig(new MavenBuildConfiguration.Builder(image.getBuildConfiguration())
                            .env(Collections.singletonMap("FOO", "BAR"))
                            .build()
                    ).build();

            service.createBuildArchive(imageWithEnv);

            final List<ArchiverCustomizer> customizer = new LinkedList<>();
            new Verifications() {{
                archiveService.createDockerBuildArchive(withInstanceOf(ImageConfiguration.class), withInstanceOf(MavenBuildContext.class), withCapture(customizer));

                assertTrue(customizer.size() == 1);
            }};

            customizer.get(0).customize(tarArchiver);

            final List<File> file = new LinkedList<>();
            new Verifications() {{
                String path;
                tarArchiver.addFile(withCapture(file), path = withCapture());

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
            BuildService.BuildServiceConfig config = defaultConfigSecret.build();
            WebServerEventCollector<OpenShiftMockServer> collector = createMockServer(config, true, 50, true, true);
            OpenShiftMockServer mockServer = collector.getMockServer();

            OpenShiftClient client = mockServer.createOpenShiftClient();
            final OpenshiftBuildService service = new OpenshiftBuildService(client, logger, dockerServiceHub, config);

            ImageConfiguration imageWithEnv = new ImageConfiguration.Builder(image)
                    .buildConfig(new MavenBuildConfiguration.Builder(image.getBuildConfiguration())
                            .env(Collections.singletonMap("FOO", "BAR"))
                            .build()
                    ).build();

            service.createBuildArchive(imageWithEnv);

            final List<ArchiverCustomizer> customizer = new LinkedList<>();
            new Verifications() {{
                archiveService.createDockerBuildArchive(withInstanceOf(ImageConfiguration.class), withInstanceOf(MavenBuildContext.class), withCapture(customizer));

                assertTrue(customizer.size() == 1);
            }};

            customizer.get(0).customize(tarArchiver);

            final List<File> file = new LinkedList<>();
            new Verifications() {{
                String path;
                tarArchiver.addFile(withCapture(file), path = withCapture());

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

    @FunctionalInterface
    private interface MockServerRetryable {
        void run() throws JkubeServiceException, IOException;
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
            } catch (JkubeServiceException exception) {
                rootCause = getRootCause(exception);
                logger.warn("A problem encountered while running test {}, retrying..", exception.getMessage());
            }
        } while (nTries < MAX_TIMEOUT_RETRIES && !bTestComplete);
        if (!bTestComplete && rootCause != null) {
            throw new Exception("Test did not complete", rootCause);
        }
    }

    protected WebServerEventCollector<OpenShiftMockServer> createMockServer(BuildService.BuildServiceConfig config, boolean success, long buildDelay, boolean buildConfigExists, boolean
            imageStreamExists) {
        OpenShiftMockServer mockServer = new OpenShiftMockServer(false);
        WebServerEventCollector<OpenShiftMockServer> collector = new WebServerEventCollector<>(mockServer);

        final String s2iBuildNameSuffix = Optional
                .ofNullable(config.getS2iBuildNameSuffix())
                .orElseGet(() -> config.getOpenshiftBuildStrategy() == OpenShiftBuildStrategy.s2i ?
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

        mockServer.expect().post().withPath("/apis/build.openshift.io/v1/namespaces/test/buildconfigs/" + projectName + s2iBuildNameSuffix + "/instantiatebinary?commit=")
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
