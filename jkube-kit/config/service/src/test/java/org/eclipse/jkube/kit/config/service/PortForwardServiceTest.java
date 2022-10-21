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

import java.io.Closeable;
import java.util.Collections;

import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.openshift.client.OpenShiftClient;
import org.eclipse.jkube.kit.common.KitLogger;

import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.PodListBuilder;
import io.fabric8.kubernetes.api.model.WatchEvent;
import io.fabric8.kubernetes.client.LocalPortForward;
import org.eclipse.jkube.kit.config.service.portforward.PortForwardTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedConstruction;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@EnableKubernetesMockClient
class PortForwardServiceTest {

    KubernetesMockServer mockServer;
    OpenShiftClient openShiftClient;

    @Mock
    private KitLogger logger;
    private MockedConstruction<PortForwardTask> portForwardTaskMockedConstruction;

    @BeforeEach
    public void setUp() throws Exception{
        logger = mock(KitLogger.class);
    }

    @Test
    void simpleScenario() throws Exception {
        // Cannot test more complex scenarios due to errors in mockwebserver

        Pod pod1 = new PodBuilder()
                .withNewMetadata()
                .withName("mypod")
                .addToLabels("mykey", "myvalue")
                .withResourceVersion("1")
                .endMetadata()
                .withNewStatus()
                .withPhase("run")
                .endStatus()
                .build();

        PodList pods1 = new PodListBuilder()
                .withItems(pod1)
                .withNewMetadata()
                .withResourceVersion("1")
                .endMetadata()
                .build();

        mockServer.expect().get().withPath("/api/v1/namespaces/ns1/pods?labelSelector=mykey%3Dmyvalue").andReturn(200, pods1).always();
        mockServer.expect().get().withPath("/api/v1/namespaces/ns1/pods").andReturn(200, pods1).always();
        mockServer.expect().get().withPath("/api/v1/namespaces/ns1/pods?labelSelector=mykey%3Dmyvalue&allowWatchBookmarks=true&watch=true")
                .andUpgradeToWebSocket().open()
                .waitFor(1000)
                .andEmit(new WatchEvent(pod1, "MODIFIED"))
                .done().always();

        mockServer.expect().get().withPath("/api/v1/namespaces/ns1/pods?resourceVersion=1&allowWatchBookmarks=true&watch=true")
                .andUpgradeToWebSocket().open()
                .waitFor(1000)
                .andEmit(new WatchEvent(pod1, "MODIFIED"))
                .done().always();

        final NamespacedKubernetesClient client = openShiftClient
            .adapt(NamespacedKubernetesClient.class).inNamespace("ns1");
        PortForwardService service = new PortForwardService(logger);

        try (Closeable c = service.forwardPortAsync(client, new LabelSelectorBuilder().withMatchLabels(Collections.singletonMap("mykey", "myvalue")).build(), 8080, 9000)) {
            Thread.sleep(3000);
        }
    }

    @Test
    void startPortForward() {
        NamespacedKubernetesClient kubernetesClient = mock(NamespacedKubernetesClient.class);
        KitLogger logger = mock(KitLogger.class);
        LocalPortForward lpf = mock(LocalPortForward.class);
        PortForwardTask pft = mock(PortForwardTask.class);
        // When
        new PortForwardService(logger)
            .startPortForward(kubernetesClient, "pod", 5005, 1337);
        // Then
        portForwardTaskMockedConstruction = mockConstruction(PortForwardTask.class,(mock, ctx) ->
                verify(mock,times(1)).run());
    }
}
