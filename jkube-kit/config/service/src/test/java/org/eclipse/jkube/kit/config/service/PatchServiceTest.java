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

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import org.eclipse.jkube.kit.common.util.UserConfigurationCompare;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

@EnableKubernetesMockClient
class PatchServiceTest {

    KubernetesMockServer mockServer;
    OpenShiftClient client;

    private PatchService patchService;

    @BeforeEach
    void setUp() {
        patchService = new PatchService(client);
    }

    @Test
    void resourcePatching() {
        ReplicationController old = new ReplicationControllerBuilder()
                .withNewMetadata().withName("resource").endMetadata()
                .withNewSpec()
                .withSelector(Collections.singletonMap("app", "MyApp"))
                .endSpec()
                .build();
        ReplicationController newResource = new ReplicationControllerBuilder()
                .withNewMetadata().withName("resource").addToAnnotations(Collections.singletonMap("app", "org.eclipse.jkube")).endMetadata()
                .withSpec(old.getSpec())
                .build();

        mockServer.expect().get().withPath("/api/v1/namespaces/test/replicationcontrollers/resource")
          .andReturn(200, old).always();
        mockServer.expect().patch().withPath("/api/v1/namespaces/test/replicationcontrollers/resource")
          .andReturn(200, new ReplicationControllerBuilder(newResource).build()).once();

        ReplicationController patchedService = patchService.compareAndPatchEntity("test", old, newResource);

        assertThat(UserConfigurationCompare.configEqual(patchedService.getMetadata(), newResource.getMetadata())).isTrue();
    }

    @Test
    void routePatching() {
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
        assertThat(UserConfigurationCompare.configEqual(patchedRoute, newRoute)).isTrue();
    }

    @Test
    void invalidPatcherKind() {
        ConfigMap oldResource = new ConfigMapBuilder()
                .withNewMetadata().withName("configmap1").endMetadata()
                .addToData(Collections.singletonMap("foo", "bar"))
                .build();
        ConfigMap newResource = new ConfigMapBuilder()
                .withNewMetadata().withName("configmap1").endMetadata()
                .addToData(Collections.singletonMap("FOO", "BAR"))
                .build();
        assertThatIllegalArgumentException()
            .isThrownBy(() -> patchService.compareAndPatchEntity("test", newResource, oldResource))
            .withMessageContaining("Internal: No patcher for ConfigMap found");
    }

}
