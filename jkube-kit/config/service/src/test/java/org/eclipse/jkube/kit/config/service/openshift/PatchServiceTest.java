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

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteBuilder;
import io.fabric8.openshift.client.server.mock.OpenShiftServer;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.UserConfigurationCompare;
import org.eclipse.jkube.kit.config.service.PatchService;
import mockit.Mocked;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class PatchServiceTest {
    @Mocked
    private KitLogger log;

    @Rule
    public final OpenShiftServer mockServer = new OpenShiftServer(false);

    private PatchService patchService;

    @Before
    public void setUp() {
        patchService = new PatchService(mockServer.getOpenshiftClient(), log);
    }

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
                .withNewMetadata().withName("service1").addToAnnotations(Collections.singletonMap("app", "org.eclipse.jkube")).endMetadata()
                .withSpec(oldService.getSpec())
                .build();

        mockServer.expect().get().withPath("/api/v1/namespaces/test/services/service1").andReturn(200, oldService).always();
        mockServer.expect().patch().withPath("/api/v1/namespaces/test/services/service1").andReturn(200, new ServiceBuilder().withMetadata(newService.getMetadata()).withSpec(oldService.getSpec()).build()).once();

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
        WebServerEventCollector collector = new WebServerEventCollector();
        mockServer.expect().get().withPath("/api/v1/namespaces/test/secrets/secret")
                .andReply(collector.record("get-secret").andReturn(200, oldSecret)).always();
        mockServer.expect().patch().withPath("/api/v1/namespaces/test/secrets/secret")
                .andReply(collector.record("patch-secret")
                        .andReturn(200, new SecretBuilder().withMetadata(newSecret.getMetadata())
                                .addToStringData(oldSecret.getData()).build())).once();

        patchService.compareAndPatchEntity("test", newSecret, oldSecret);
        collector.assertEventsRecordedInOrder("get-secret", "get-secret", "patch-secret");
        assertEquals("[{\"op\":\"remove\",\"path\":\"/data\"},{\"op\":\"add\",\"path\":\"/stringData\",\"value\":{\"test\":\"test\"}}]", collector.getBodies().get(3));

    }

    @Test
    public void testRoutePatching() {
        Route oldRoute = new RouteBuilder()
                .withNewMetadata()
                    .withName("route")
                .endMetadata()
                .withNewSpec()
                    .withHost("www.example.com")
                    .withNewTo()
                        .withKind("Service")
                        .withName("frontend")
                    .endTo()
                .endSpec()
                .build();
        Route newRoute = new RouteBuilder()
                .withNewMetadata()
                    .withName("route")
                    .addToAnnotations("haproxy.router.openshift.io/balance", "roundrobin")
                .endMetadata()
                .withSpec(oldRoute.getSpec())
                .build();

        mockServer.expect().get()
                .withPath("/apis/route.openshift.io/v1/namespaces/test/routes/route")
                .andReturn(200, oldRoute)
                .always();
        mockServer.expect().patch()
                .withPath("/apis/route.openshift.io/v1/namespaces/test/routes/route")
                .andReturn(200, new RouteBuilder()
                        .withMetadata(newRoute.getMetadata())
                        .withSpec(oldRoute.getSpec())
                        .build())
                .once();

        Route patchedRoute = patchService.compareAndPatchEntity("test", newRoute, oldRoute);
        assertTrue(UserConfigurationCompare.configEqual(patchedRoute, newRoute));
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

        patchService.compareAndPatchEntity("test", newResource, oldResource);
    }

}
