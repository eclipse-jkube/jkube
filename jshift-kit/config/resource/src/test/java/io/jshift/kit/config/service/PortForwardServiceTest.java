/**
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.jshift.kit.config.service;

import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.PodListBuilder;
import io.fabric8.kubernetes.api.model.WatchEvent;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.server.mock.OpenShiftMockServer;
import io.jshift.kit.common.KitLogger;
import io.jshift.kit.common.util.ProcessUtil;
import mockit.Expectations;
import mockit.Mocked;
import org.junit.Before;
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

    @Before
    public void init() throws Exception {
        new Expectations() {{
            process.destroy();
        }};
    }

    @Test
    public void testSimpleScenario() throws Exception {
        // Cannot test more complex scenarios due to errors in mockwebserver
        OpenShiftMockServer mockServer = new OpenShiftMockServer(false);

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

        OpenShiftClient client = mockServer.createOpenShiftClient();
        PortForwardService service = new PortForwardService(client, logger) {
            @Override
            public ProcessUtil.ProcessExecutionContext forwardPortAsync(KitLogger externalProcessLogger, String pod, int remotePort, int localPort) throws JshiftServiceException {
                return new ProcessUtil.ProcessExecutionContext(process, Collections.<Thread>emptyList(), logger);
            }
        };

        try (Closeable c = service.forwardPortAsync(logger, new LabelSelectorBuilder().withMatchLabels(Collections.singletonMap("mykey", "myvalue")).build(), 8080, 9000)) {
            Thread.sleep(3000);
        }
    }

}
