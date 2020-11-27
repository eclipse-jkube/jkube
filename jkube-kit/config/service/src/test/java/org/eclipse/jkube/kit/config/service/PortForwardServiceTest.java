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

import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.PodListBuilder;
import io.fabric8.kubernetes.api.model.WatchEvent;
import io.fabric8.kubernetes.client.LocalPortForward;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.server.mock.OpenShiftServer;
import org.eclipse.jkube.kit.common.KitLogger;
import mockit.Mocked;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.Closeable;
import java.util.Collections;

public class PortForwardServiceTest {

    @Mocked
    private KitLogger logger;

    @Mocked
    private Process process;

    @Mocked
    private ClientToolsService clientToolsService;

    @Rule
    public final OpenShiftServer mockServer = new OpenShiftServer(false);

    @Before
    public void init() throws Exception {
        new Expectations() {{
            process.destroy();
        }};
    }

    @Test
    public void testSimpleScenario() throws Exception {
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

        mockServer.expect().get().withPath("/api/v1/namespaces/test/pods?labelSelector=mykey%3Dmyvalue").andReturn(200, pods1).always();
        mockServer.expect().get().withPath("/api/v1/namespaces/test/pods").andReturn(200, pods1).always();
        mockServer.expect().get().withPath("/api/v1/namespaces/test/pods?labelSelector=mykey%3Dmyvalue&watch=true")
                .andUpgradeToWebSocket().open()
                .waitFor(1000)
                .andEmit(new WatchEvent(pod1, "MODIFIED"))
                .done().always();

        mockServer.expect().get().withPath("/api/v1/namespaces/test/pods?resourceVersion=1&watch=true")
                .andUpgradeToWebSocket().open()
                .waitFor(1000)
                .andEmit(new WatchEvent(pod1, "MODIFIED"))
                .done().always();

        OpenShiftClient client = mockServer.getOpenshiftClient();
        PortForwardService service = new PortForwardService(client, logger) {
            @Override
            public LocalPortForward forwardPortAsync(String pod, String namespace, int remotePort, int localPort) {
                return client.pods().inNamespace(namespace).withName(pod).portForward(localPort, remotePort);
            }
        };

        try (Closeable c = service.forwardPortAsync(new LabelSelectorBuilder().withMatchLabels(Collections.singletonMap("mykey", "myvalue")).build(), 8080, 9000)) {
            Thread.sleep(3000);
        }
    }

}
