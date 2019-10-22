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
package io.jkube.kit.config.service.openshift;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.server.mock.OpenShiftMockServer;
import io.jkube.kit.common.KitLogger;
import io.jkube.kit.common.util.UserConfigurationCompare;
import io.jkube.kit.config.service.PatchService;
import mockit.Mocked;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class PatchServiceTest {
    @Mocked
    KitLogger log;

    OpenShiftMockServer mockServer = new OpenShiftMockServer(false);

    @Test
    public void testResourcePatching() {
        Service oldService = new ServiceBuilder()
                .withNewMetadata().withName("service1").endMetadata()
                .withNewSpec()
                .withClusterIP("192.168.1.3")
                .withSelector(Collections.singletonMap("app", "MyApp"))
                .addNewPort()
                .withProtocol("TCP")
                .withTargetPort(new IntOrString("9376"))
                .withPort(80)
                .endPort()
                .endSpec()
                .build();
        Service newService = new ServiceBuilder()
                .withNewMetadata().withName("service1").addToAnnotations(Collections.singletonMap("app", "io.jkube")).endMetadata()
                .withSpec(oldService.getSpec())
                .build();

        mockServer.expect().get().withPath("/api/v1/namespaces/test/services/service1").andReturn(200, oldService).always();
        mockServer.expect().patch().withPath("/api/v1/namespaces/test/services/service1").andReturn(200, new ServiceBuilder().withMetadata(newService.getMetadata()).withSpec(oldService.getSpec()).build()).once();

        OpenShiftClient client = mockServer.createOpenShiftClient();
        PatchService patchService = new PatchService(client, log);

        Service patchedService = patchService.compareAndPatchEntity("test", newService, oldService);

        assertTrue(UserConfigurationCompare.configEqual(patchedService.getMetadata(), newService.getMetadata()));
    }

    @Test
    public void testSecretPatching() {
        Secret oldSecret = new SecretBuilder()
                .withNewMetadata().withName("secret").endMetadata()
                .addToData("test", "dGVzdA==")
                .build();
        Secret newSecret = new SecretBuilder()
                .withNewMetadata().withName("secret").endMetadata()
                .addToStringData("test", "test")
                .build();
        WebServerEventCollector<OpenShiftMockServer> collector = new WebServerEventCollector<>(mockServer);
        mockServer.expect().get().withPath("/api/v1/namespaces/test/secrets/secret")
                .andReply(collector.record("get-secret").andReturn(200, oldSecret)).always();
        mockServer.expect().patch().withPath("/api/v1/namespaces/test/secrets/secret")
                .andReply(collector.record("patch-secret")
                        .andReturn(200, new SecretBuilder().withMetadata(newSecret.getMetadata())
                                .addToStringData(oldSecret.getData()).build())).once();

        OpenShiftClient client = mockServer.createOpenShiftClient();

        PatchService patchService = new PatchService(client, log);

        patchService.compareAndPatchEntity("test", newSecret, oldSecret);
        collector.assertEventsRecordedInOrder("get-secret", "get-secret", "patch-secret");
        assertEquals("[{\"op\":\"remove\",\"path\":\"/data\"},{\"op\":\"add\",\"path\":\"/stringData\",\"value\":{\"test\":\"test\"}}]", collector.getBodies().get(2));

    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidPatcherKind() {
        ConfigMap oldResource = new ConfigMapBuilder()
                .withNewMetadata().withName("configmap1").endMetadata()
                .addToData(Collections.singletonMap("foo", "bar"))
                .build();
        ConfigMap newResource = new ConfigMapBuilder()
                .withNewMetadata().withName("configmap1").endMetadata()
                .addToData(Collections.singletonMap("FOO", "BAR"))
                .build();

        OpenShiftClient client = mockServer.createOpenShiftClient();
        PatchService patchService = new PatchService(client, log);

        patchService.compareAndPatchEntity("test", newResource, oldResource);
    }
}
