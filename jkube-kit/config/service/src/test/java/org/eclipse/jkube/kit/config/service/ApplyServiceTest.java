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

import io.fabric8.kubernetes.api.model.apiextensions.v1beta1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.v1beta1.CustomResourceDefinitionBuilder;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteBuilder;
import io.fabric8.openshift.client.server.mock.OpenShiftServer;
import mockit.Mocked;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.service.openshift.WebServerEventCollector;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.Assert.assertEquals;

public class ApplyServiceTest {

    @Mocked
    private KitLogger log;

    @Rule
    public final OpenShiftServer mockServer = new OpenShiftServer(false);

    private ApplyService applyService;

    @Before
    public void setUp() {
        applyService = new ApplyService(mockServer.getOpenshiftClient(), log);
        applyService.setNamespace("default");
    }

    @Test
    public void testCreateRoute() throws Exception {
        Route route = buildRoute();

        WebServerEventCollector<OpenShiftServer> collector = new WebServerEventCollector<>(mockServer);
        mockServer.expect().get()
                .withPath("/apis/route.openshift.io/v1/namespaces/default/routes/route")
                .andReply(collector.record("get-route").andReturn(HTTP_NOT_FOUND, ""))
                .always();
        mockServer.expect().post()
                .withPath("/apis/route.openshift.io/v1/namespaces/default/routes")
                .andReply(collector.record("new-route").andReturn(HTTP_CREATED, route))
                .once();

        applyService.apply(route, "route.yml");

        collector.assertEventsRecordedInOrder("get-route", "new-route");
    }

    @Test
    public void testUpdateRoute() throws Exception {
        Route oldRoute = buildRoute();
        Route newRoute = new RouteBuilder()
                .withNewMetadataLike(oldRoute.getMetadata())
                    .addToAnnotations("haproxy.router.openshift.io/balance", "roundrobin")
                .endMetadata()
                .withSpec(oldRoute.getSpec())
                .build();

        WebServerEventCollector<OpenShiftServer> collector = new WebServerEventCollector<>(mockServer);
        mockServer.expect().get()
                .withPath("/apis/route.openshift.io/v1/namespaces/default/routes/route")
                .andReply(collector.record("get-route").andReturn(HTTP_OK, oldRoute))
                .always();
        mockServer.expect().patch()
                .withPath("/apis/route.openshift.io/v1/namespaces/default/routes/route")
                .andReply(collector.record("patch-route")
                        .andReturn(HTTP_OK, new RouteBuilder()
                                .withMetadata(newRoute.getMetadata())
                                .withSpec(oldRoute.getSpec())
                                .build()))
                .once();

        applyService.apply(newRoute, "route.yml");

        collector.assertEventsRecordedInOrder("get-route", "patch-route");
    }

    @Test
    public void testCreateRouteInServiceOnlyMode() throws Exception {
        Route route = buildRoute();

        WebServerEventCollector<OpenShiftServer> collector = new WebServerEventCollector<>(mockServer);
        mockServer.expect().get()
                .withPath("/apis/route.openshift.io/v1/namespaces/default/routes/route")
                .andReply(collector.record("get-route").andReturn(HTTP_NOT_FOUND, ""))
                .always();

        applyService.setServicesOnlyMode(true);
        applyService.apply(route, "route.yml");

        collector.assertEventsNotRecorded("get-route");
        assertEquals(1, mockServer.getMockServer().getRequestCount());
    }

    @Test
    public void testCreateRouteNotAllowed() throws Exception {
        Route route = buildRoute();

        WebServerEventCollector<OpenShiftServer> collector = new WebServerEventCollector<>(mockServer);
        mockServer.expect().get()
                .withPath("/apis/route.openshift.io/v1/namespaces/default/routes/route")
                .andReply(collector.record("get-route").andReturn(HTTP_NOT_FOUND, ""))
                .always();

        applyService.setAllowCreate(false);
        applyService.apply(route, "route.yml");

        collector.assertEventsRecordedInOrder("get-route");
        assertEquals(2, mockServer.getMockServer().getRequestCount());
    }

    @Test
    public void testProcessCustomEntities() throws Exception {
        // Given
        List<String> crdNames = new ArrayList<>();
        crdNames.add("virtualservices.networking.istio.io");
        crdNames.add("gateways.networking.istio.io");
        File crFragmentDir = new File(getClass().getResource("/gateway-cr.yml").getFile()).getParentFile();
        WebServerEventCollector<OpenShiftServer> collector = new WebServerEventCollector<>(mockServer);
        mockServer.expect().get()
                .withPath("/apis/apiextensions.k8s.io/v1beta1/customresourcedefinitions/virtualservices.networking.istio.io")
                .andReply(collector.record("get-crd-virtualservice").andReturn(HTTP_OK, getVirtualServiceIstioCRD()))
                .always();
        mockServer.expect().get()
                .withPath("/apis/apiextensions.k8s.io/v1beta1/customresourcedefinitions/gateways.networking.istio.io")
                .andReply(collector.record("get-crd-gateway").andReturn(HTTP_OK, getGatewayIstioCRD()))
                .always();
        mockServer.expect().post()
                .withPath("/apis/networking.istio.io/v1alpha3/namespaces/default/virtualservices")
                .andReply(collector.record("post-cr-virtualservice").andReturn(HTTP_OK, "{}"))
                .once();
        mockServer.expect().post()
                .withPath("/apis/networking.istio.io/v1alpha3/namespaces/default/gateways")
                .andReply(collector.record("post-cr-gateway").andReturn(HTTP_OK, "{}"))
                .once();

        // When
        applyService.processCustomEntities(crdNames, crFragmentDir, null, Collections.emptyList());

        // Then
        collector.assertEventsRecordedInOrder("get-crd-virtualservice", "get-crd-gateway", "post-cr-virtualservice", "post-cr-gateway");
        assertEquals(7, mockServer.getMockServer().getRequestCount());
    }

    @Test
    public void testProcessCustomEntitiesReplaceCustomResources() throws Exception {
        // Given
        List<String> crdNames = new ArrayList<>();
        crdNames.add("virtualservices.networking.istio.io");
        crdNames.add("gateways.networking.istio.io");
        File crFragmentDir = new File(getClass().getResource("/gateway-cr.yml").getFile()).getParentFile();
        WebServerEventCollector<OpenShiftServer> collector = new WebServerEventCollector<>(mockServer);
        mockServer.expect().get()
                .withPath("/apis/apiextensions.k8s.io/v1beta1/customresourcedefinitions/virtualservices.networking.istio.io")
                .andReply(collector.record("get-crd-virtualservice").andReturn(HTTP_OK, getVirtualServiceIstioCRD()))
                .always();
        mockServer.expect().get()
                .withPath("/apis/apiextensions.k8s.io/v1beta1/customresourcedefinitions/gateways.networking.istio.io")
                .andReply(collector.record("get-crd-gateway").andReturn(HTTP_OK, getGatewayIstioCRD()))
                .always();
        mockServer.expect().get()
                .withPath("/apis/networking.istio.io/v1alpha3/namespaces/default/virtualservices/reviews-route")
                .andReply(collector.record("get-cr-virtualservice").andReturn(HTTP_OK, "{}"))
                .once();
        mockServer.expect().put()
                .withPath("/apis/networking.istio.io/v1alpha3/namespaces/default/virtualservices/reviews-route")
                .andReply(collector.record("put-cr-virtualservice").andReturn(HTTP_OK, "{}"))
                .once();
        mockServer.expect().get()
                .withPath("/apis/networking.istio.io/v1alpha3/namespaces/default/gateways/mygateway-https")
                .andReply(collector.record("get-cr-gateway").andReturn(HTTP_OK, "{}"))
                .once();
        mockServer.expect().put()
                .withPath("/apis/networking.istio.io/v1alpha3/namespaces/default/gateways/mygateway-https")
                .andReply(collector.record("put-cr-gateway").andReturn(HTTP_OK, "{}"))
                .once();

        // When
        applyService.processCustomEntities(crdNames, crFragmentDir, null, Collections.emptyList());

        // Then
        collector.assertEventsRecordedInOrder("get-crd-virtualservice", "get-crd-gateway", "get-cr-virtualservice", "put-cr-virtualservice", "get-cr-gateway", "put-cr-gateway");
        assertEquals(7, mockServer.getMockServer().getRequestCount());
    }

    @Test
    public void testProcessCustomEntitiesRecreateModeTrue() throws Exception {
        // Given
        List<String> crdNames = new ArrayList<>();
        crdNames.add("virtualservices.networking.istio.io");
        crdNames.add("gateways.networking.istio.io");
        File crFragmentDir = new File(getClass().getResource("/gateway-cr.yml").getFile()).getParentFile();
        WebServerEventCollector<OpenShiftServer> collector = new WebServerEventCollector<>(mockServer);
        mockServer.expect().get()
                .withPath("/apis/apiextensions.k8s.io/v1beta1/customresourcedefinitions/virtualservices.networking.istio.io")
                .andReply(collector.record("get-crd-virtualservice").andReturn(HTTP_OK, getVirtualServiceIstioCRD()))
                .always();
        mockServer.expect().get()
                .withPath("/apis/apiextensions.k8s.io/v1beta1/customresourcedefinitions/gateways.networking.istio.io")
                .andReply(collector.record("get-crd-gateway").andReturn(HTTP_OK, getGatewayIstioCRD()))
                .always();
        mockServer.expect().delete()
                .withPath("/apis/networking.istio.io/v1alpha3/namespaces/default/virtualservices/reviews-route")
                .andReply(collector.record("delete-cr-virtualservice").andReturn(HTTP_OK, "{}"))
                .once();
        mockServer.expect().delete()
                .withPath("/apis/networking.istio.io/v1alpha3/namespaces/default/gateways/mygateway-https")
                .andReply(collector.record("delete-cr-gateway").andReturn(HTTP_OK, "{}"))
                .once();
        mockServer.expect().post()
                .withPath("/apis/networking.istio.io/v1alpha3/namespaces/default/virtualservices")
                .andReply(collector.record("post-cr-virtualservice").andReturn(HTTP_OK, "{}"))
                .once();
        mockServer.expect().post()
                .withPath("/apis/networking.istio.io/v1alpha3/namespaces/default/gateways")
                .andReply(collector.record("post-cr-gateway").andReturn(HTTP_OK, "{}"))
                .once();
        applyService.setRecreateMode(true);

        // When
        applyService.processCustomEntities(crdNames, crFragmentDir, null, Collections.emptyList());
        applyService.setRecreateMode(false);

        // Then
        collector.assertEventsRecordedInOrder("get-crd-virtualservice", "get-crd-gateway",
                "delete-cr-virtualservice", "post-cr-virtualservice", "delete-cr-gateway", "post-cr-gateway");
        assertEquals(7, mockServer.getMockServer().getRequestCount());
    }

    @Test
    public void testProcessCustomEntitiesWithNullCustomResourceList() throws Exception {
        // Given
        File crFragmentDir = new File(getClass().getResource("/gateway-cr.yml").getFile()).getParentFile();

        // When
        applyService.processCustomEntities(null, crFragmentDir, null, Collections.emptyList());

        // Then
        assertEquals(1, mockServer.getMockServer().getRequestCount()); // This one is just /apis check
    }

    private Route buildRoute() {
        return new RouteBuilder()
                .withNewMetadata()
                    .withNamespace("default")
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
    }

    private CustomResourceDefinition getVirtualServiceIstioCRD() {
        return new CustomResourceDefinitionBuilder()
                .withNewMetadata().withName("virtualservices.networking.istio.io").endMetadata()
                .withNewSpec()
                .withGroup("networking.istio.io")
                .withVersion("v1alpha3")
                .withNewNames()
                .withKind("VirtualService")
                .withPlural("virtualservices")
                .endNames()
                .withScope("Namespaced")
                .endSpec()
                .build();
    }

    private CustomResourceDefinition getGatewayIstioCRD() {
        return new CustomResourceDefinitionBuilder()
                .withNewMetadata().withName("gateways.networking.istio.io").endMetadata()
                .withNewSpec()
                .withGroup("networking.istio.io")
                .withVersion("v1alpha3")
                .withNewNames()
                .withKind("Gateway")
                .withPlural("gateways")
                .endNames()
                .withScope("Namespaced")
                .endSpec()
                .build();
    }

}