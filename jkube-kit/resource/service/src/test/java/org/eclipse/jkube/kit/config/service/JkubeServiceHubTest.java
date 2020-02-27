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
package org.eclipse.jkube.kit.config.service;

import io.fabric8.openshift.client.OpenShiftClient;
import org.eclipse.jkube.kit.build.service.docker.ServiceHub;
import org.eclipse.jkube.kit.common.JkubeProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.kubernetes.DockerBuildService;
import org.eclipse.jkube.kit.config.service.openshift.OpenshiftBuildService;
import mockit.Expectations;
import mockit.Mocked;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;

public class JkubeServiceHubTest {

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
    private JkubeProject jkubeProject;

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
        new JkubeServiceHub.Builder()
                .log(logger)
                .build();
    }

    @Test(expected = NullPointerException.class)
    public void testMissingKitLogger() {
        new JkubeServiceHub.Builder()
                .clusterAccess(clusterAccess)
                .build();
    }

    @Test
    public void testBasicInit() {
        new JkubeServiceHub.Builder()
                .clusterAccess(clusterAccess)
                .log(logger)
                .platformMode(RuntimeMode.auto)
                .build();
    }

    @Test
    public void testObtainBuildService() {
        JkubeServiceHub hub = new JkubeServiceHub.Builder()
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
        JkubeServiceHub hub = new JkubeServiceHub.Builder()
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
        JkubeServiceHub hub = new JkubeServiceHub.Builder()
                .clusterAccess(clusterAccess)
                .log(logger)
                .platformMode(RuntimeMode.kubernetes)
                .jkubeProject(jkubeProject)
                .build();

        assertNotNull(hub.getArtifactResolverService());
    }
}
