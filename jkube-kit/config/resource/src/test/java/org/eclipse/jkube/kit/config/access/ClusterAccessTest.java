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
package org.eclipse.jkube.kit.config.access;

import io.fabric8.kubernetes.api.model.APIGroupListBuilder;
import io.fabric8.kubernetes.api.model.RootPaths;
import io.fabric8.kubernetes.client.Client;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.server.mock.OpenShiftMockServer;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import mockit.Mocked;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ClusterAccessTest {

    @Mocked
    private KitLogger logger;

    private RuntimeMode mode;

    private List<String> paths = new ArrayList<>() ;

    OpenShiftMockServer mockServer = new OpenShiftMockServer(false);
    OpenShiftClient client = mockServer.createOpenShiftClient();

    @Test
    public void openshiftRuntimeModeTest() throws Exception {

        paths.add("/oapi");
        paths.add("/oapi/v1");

        RootPaths rootpaths = new RootPaths();

        rootpaths.setPaths(paths);

        mockServer.expect().get().withPath("/" ).andReturn(200, rootpaths).always();
        mockServer.expect().withPath("/apis").andReturn(200, new APIGroupListBuilder()
                .addNewGroup()
                .withApiVersion("v1")
                .withName("autoscaling.k8s.io")
                .endGroup()
                .addNewGroup()
                .withApiVersion("v1")
                .withName("security.openshift.io")
                .endGroup()
                .build()).always();

        ClusterAccess clusterAccess = new ClusterAccess(null, client);

        mode = clusterAccess.resolveRuntimeMode(RuntimeMode.openshift, logger);
        assertEquals(RuntimeMode.openshift, mode);

        mode = clusterAccess.resolveRuntimeMode(RuntimeMode.DEFAULT, logger);
        assertEquals(RuntimeMode.openshift, mode);

        mode = clusterAccess.resolveRuntimeMode(null, logger);
        assertEquals(RuntimeMode.openshift, mode);
    }

    @Test
    public void kubernetesRuntimeModeTest() throws Exception {

        RootPaths rootpaths = new RootPaths();

        rootpaths.setPaths(paths);

        mockServer.expect().get().withPath("/" ).andReturn(200, rootpaths).always();

        ClusterAccess clusterAccess = new ClusterAccess(null, client);

        mode = clusterAccess.resolveRuntimeMode(RuntimeMode.kubernetes, logger);
        assertEquals(RuntimeMode.kubernetes, mode);

        mode = clusterAccess.resolveRuntimeMode(RuntimeMode.DEFAULT, logger);
        assertEquals(RuntimeMode.kubernetes, mode);

        mode = clusterAccess.resolveRuntimeMode(null, logger);
        assertEquals(RuntimeMode.kubernetes, mode);
    }

    @Test
    @Ignore("Ignored as long as the kubernetes client not update with the fix https://github.com/fabric8io/kubernetes-client/pull/1209")
    public void createClientTestOpenshift() throws Exception {

        paths.add("/oapi");
        paths.add("/oapi/v1");

        RootPaths rootpaths = new RootPaths();

        rootpaths.setPaths(paths);

        mockServer.expect().get().withPath("/" ).andReturn(200, rootpaths).always();

        ClusterAccess clusterAccess = new ClusterAccess(null, client);

        Client outputClient = clusterAccess.createDefaultClient(logger);
        assertTrue(outputClient instanceof OpenShiftClient);

    }

    @Test
    @Ignore("Ignored as long as the kubernetes client not update with the fix https://github.com/fabric8io/kubernetes-client/pull/1209")
    public void createClientTestKubernetes() throws Exception {

        RootPaths rootpaths = new RootPaths();

        rootpaths.setPaths(paths);

        mockServer.expect().get().withPath("/" ).andReturn(200, rootpaths).always();

        ClusterAccess clusterAccess = new ClusterAccess(null, client);

        Client outputClient = clusterAccess.createDefaultClient(logger);
        assertTrue(outputClient instanceof KubernetesClient);  }

}
