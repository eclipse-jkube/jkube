package io.jshift.kit.config.service;

import io.fabric8.openshift.client.OpenShiftClient;
import io.jshift.kit.build.service.docker.ServiceHub;
import io.jshift.kit.common.KitLogger;
import io.jshift.kit.config.access.ClusterAccess;
import io.jshift.kit.config.resource.RuntimeMode;
import io.jshift.kit.config.service.kubernetes.DockerBuildService;
import io.jshift.kit.config.service.openshift.OpenshiftBuildService;
import mockit.Expectations;
import mockit.Mocked;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;

public class JshiftServiceHubTest {

    @Mocked
    private KitLogger logger;

    @Mocked
    private ClusterAccess clusterAccess;

    @Mocked
    private OpenShiftClient openShiftClient;

    @Mocked
    private ServiceHub dockerServiceHub;

    @Mocked
    private BuildService.BuildServiceConfig buildServiceConfig;

    @Mocked
    private MavenProject mavenProject;

    @Mocked
    private RepositorySystem repositorySystem;

    @Before
    public void init() throws Exception {
        new Expectations() {{
            clusterAccess.resolveRuntimeMode(RuntimeMode.kubernetes, withInstanceOf(KitLogger.class));
            result = RuntimeMode.kubernetes;
            minTimes = 0;

            clusterAccess.resolveRuntimeMode(RuntimeMode.openshift, withInstanceOf(KitLogger.class));
            result = RuntimeMode.openshift;
            minTimes = 0;

            clusterAccess.resolveRuntimeMode(RuntimeMode.auto, withInstanceOf(KitLogger.class));
            result = RuntimeMode.kubernetes;
            minTimes = 0;

            clusterAccess.createKubernetesClient();
            result = openShiftClient;
            minTimes = 0;
        }};
    }

    @Test(expected = NullPointerException.class)
    public void testMissingClusterAccess() {
        new JshiftServiceHub.Builder()
                .log(logger)
                .build();
    }

    @Test(expected = NullPointerException.class)
    public void testMissingKitLogger() {
        new JshiftServiceHub.Builder()
                .clusterAccess(clusterAccess)
                .build();
    }

    @Test
    public void testBasicInit() {
        new JshiftServiceHub.Builder()
                .clusterAccess(clusterAccess)
                .log(logger)
                .platformMode(RuntimeMode.auto)
                .build();
    }

    @Test
    public void testObtainBuildService() {
        JshiftServiceHub hub = new JshiftServiceHub.Builder()
                .clusterAccess(clusterAccess)
                .log(logger)
                .platformMode(RuntimeMode.kubernetes)
                .dockerServiceHub(dockerServiceHub)
                .buildServiceConfig(buildServiceConfig)
                .build();

        BuildService buildService = hub.getBuildService();

        assertNotNull(buildService);
        assertTrue(buildService instanceof DockerBuildService);
    }

    @Test
    public void testObtainOpenshiftBuildService() {
        JshiftServiceHub hub = new JshiftServiceHub.Builder()
                .clusterAccess(clusterAccess)
                .log(logger)
                .platformMode(RuntimeMode.openshift)
                .dockerServiceHub(dockerServiceHub)
                .buildServiceConfig(buildServiceConfig)
                .build();

        BuildService buildService = hub.getBuildService();

        assertNotNull(buildService);
        assertTrue(buildService instanceof OpenshiftBuildService);
    }

    @Test
    public void testObtainArtifactResolverService() {
        JshiftServiceHub hub = new JshiftServiceHub.Builder()
                .clusterAccess(clusterAccess)
                .log(logger)
                .platformMode(RuntimeMode.kubernetes)
                .mavenProject(mavenProject)
                .repositorySystem(repositorySystem)
                .build();

        assertNotNull(hub.getArtifactResolverService());
    }
}
