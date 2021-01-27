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

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1beta1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.v1beta1.CustomResourceDefinitionBuilder;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1beta1.CustomResourceDefinitionListBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.fabric8.openshift.api.model.Project;
import io.fabric8.openshift.api.model.ProjectBuilder;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteBuilder;
import io.fabric8.openshift.client.server.mock.OpenShiftServer;
import mockit.Mocked;
import org.eclipse.jkube.kit.common.GenericCustomResource;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.service.openshift.WebServerEventCollector;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ApplyServiceTest {

    @Mocked
    private KitLogger log;

    @Rule
    public final OpenShiftServer mockServer = new OpenShiftServer(false);

    private ApplyService applyService;

    private final CustomResourceDefinition virtualServiceCRD = new CustomResourceDefinitionBuilder()
            .withNewMetadata().withName("networking.istio.io.virtualservices").endMetadata()
            .withNewSpec()
            .withGroup("networking.istio.io")
            .withScope("Namespaced")
            .withVersion("v1alpha3")
            .withNewNames()
            .withKind("VirtualService")
            .withPlural("virtualservices")
            .endNames()
            .endSpec()
            .build();
    private final CustomResourceDefinition gatewayCRD = new CustomResourceDefinitionBuilder()
            .withNewMetadata().withName("networking.istio.io.gateway").endMetadata()
            .withNewSpec()
            .withGroup("networking.istio.io")
            .withScope("Namespaced")
            .withVersion("v1alpha3")
            .withNewNames()
            .withKind("Gateway")
            .withPlural("gateways")
            .endNames()
            .endSpec()
            .build();

    @Before
    public void setUp() {
        applyService = new ApplyService(mockServer.getOpenshiftClient(), log);
        applyService.setNamespace("default");
    }

    @Test
    public void testApplyEntities() throws Exception {
        // Given
        Set<HasMetadata> entities = new HashSet<>();
        String fileName = "foo.yml";
        Deployment deployment = new DeploymentBuilder().withNewMetadata().withName("d1").endMetadata().build();
        Service service = new ServiceBuilder().withNewMetadata().withName("svc1").endMetadata().build();
        ConfigMap configMap = new ConfigMapBuilder().withNewMetadata().withName("c1").endMetadata().build();
        Pod pod = new PodBuilder().withNewMetadata().withName("p1").endMetadata().build();
        ReplicationController rc = new ReplicationControllerBuilder().withNewMetadata().withName("rc1").endMetadata().build();
        entities.add(deployment);
        entities.add(service);
        entities.add(configMap);
        entities.add(pod);
        entities.add(rc);
        WebServerEventCollector collector = new WebServerEventCollector();
        mockServer.expect().post()
                .withPath("/api/v1/namespaces/default/services")
                .andReply(collector.record("new-service").andReturn(HTTP_CREATED, service))
                .once();
        mockServer.expect().post()
                .withPath("/api/v1/namespaces/default/configmaps")
                .andReply(collector.record("new-configmap").andReturn(HTTP_CREATED, configMap))
                .once();
        mockServer.expect().post()
                .withPath("/apis/apps/v1/namespaces/default/deployments")
                .andReply(collector.record("new-deploy").andReturn(HTTP_CREATED, deployment))
                .once();
        mockServer.expect().post()
                .withPath("/api/v1/namespaces/default/pods")
                .andReply(collector.record("new-pod").andReturn(HTTP_CREATED, pod))
                .once();
        mockServer.expect().post()
                .withPath("/api/v1/namespaces/default/replicationcontrollers")
                .andReply(collector.record("new-rc").andReturn(HTTP_CREATED, rc))
                .once();

        // When
        applyService.applyEntities(fileName, entities, log, 5);

        // Then
        collector.assertEventsRecordedInOrder("new-rc", "new-configmap", "new-service", "new-deploy", "new-pod");
    }

    @Test
    public void testCreateRoute() {
        Route route = buildRoute();

        WebServerEventCollector collector = new WebServerEventCollector();
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
    public void testUpdateRoute() {
        Route oldRoute = buildRoute();
        Route newRoute = new RouteBuilder()
                .withNewMetadataLike(oldRoute.getMetadata())
                    .addToAnnotations("haproxy.router.openshift.io/balance", "roundrobin")
                .endMetadata()
                .withSpec(oldRoute.getSpec())
                .build();

        WebServerEventCollector collector = new WebServerEventCollector();
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
    public void testCreateRouteInServiceOnlyMode() {
        Route route = buildRoute();

        WebServerEventCollector collector = new WebServerEventCollector();
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
    public void testCreateRouteNotAllowed() {
        Route route = buildRoute();

        WebServerEventCollector collector = new WebServerEventCollector();
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
    public void testApplyGenericCustomResource() throws Exception {
        // Given
        File gatewayFragment = new File(getClass().getResource("/gateway-cr.yml").getFile());
        File virtualServiceFragment = new File(getClass().getResource("/virtualservice-cr.yml").getFile());
        GenericCustomResource gateway = Serialization.yamlMapper().readValue(gatewayFragment, GenericCustomResource.class);
        GenericCustomResource virtualService = Serialization.yamlMapper().readValue(virtualServiceFragment, GenericCustomResource.class);
        WebServerEventCollector collector = new WebServerEventCollector();
        mockServer.expect().get()
                .withPath("/apis/apiextensions.k8s.io/v1beta1/customresourcedefinitions")
                .andReply(collector.record("get-crds").andReturn(HTTP_OK, new CustomResourceDefinitionListBuilder().withItems(virtualServiceCRD, gatewayCRD).build()))
                .times(2);
        mockServer.expect().post()
                .withPath("/apis/networking.istio.io/v1alpha3/namespaces/default/virtualservices")
                .andReply(collector.record("post-cr-virtualservice").andReturn(HTTP_OK, "{}"))
                .once();
        mockServer.expect().post()
                .withPath("/apis/networking.istio.io/v1alpha3/namespaces/default/gateways")
                .andReply(collector.record("post-cr-gateway").andReturn(HTTP_OK, "{}"))
                .once();

        // When
        applyService.applyGenericCustomResource(gateway, gatewayFragment.getName());
        applyService.applyGenericCustomResource(virtualService, virtualServiceFragment.getName());

        // Then
        collector.assertEventsRecordedInOrder("get-crds", "post-cr-gateway", "get-crds", "post-cr-virtualservice");
        assertEquals(7, mockServer.getMockServer().getRequestCount());
    }

    @Test
    public void testProcessCustomEntitiesReplaceCustomResources() throws Exception {
        // Given
        File gatewayFragment = new File(getClass().getResource("/gateway-cr.yml").getFile());
        File virtualServiceFragment = new File(getClass().getResource("/virtualservice-cr.yml").getFile());
        GenericCustomResource gateway = Serialization.yamlMapper().readValue(gatewayFragment, GenericCustomResource.class);
        GenericCustomResource virtualService = Serialization.yamlMapper().readValue(virtualServiceFragment, GenericCustomResource.class);
        WebServerEventCollector collector = new WebServerEventCollector();
        mockServer.expect().get()
                .withPath("/apis/apiextensions.k8s.io/v1beta1/customresourcedefinitions")
                .andReply(collector.record("get-crds").andReturn(HTTP_OK, new CustomResourceDefinitionListBuilder().withItems(virtualServiceCRD, gatewayCRD).build()))
                .times(2);
        mockServer.expect().get()
                .withPath("/apis/networking.istio.io/v1alpha3/namespaces/default/virtualservices/reviews-route")
                .andReply(collector.record("get-cr-virtualservice").andReturn(HTTP_OK, "{\"metadata\":{\"resourceVersion\":\"1001\"}}"))
                .times(2);
        mockServer.expect().put()
                .withPath("/apis/networking.istio.io/v1alpha3/namespaces/default/virtualservices/reviews-route")
                .andReply(collector.record("put-cr-virtualservice").andReturn(HTTP_OK, "{}"))
                .once();
        mockServer.expect().get()
                .withPath("/apis/networking.istio.io/v1alpha3/namespaces/default/gateways/mygateway-https")
                .andReply(collector.record("get-cr-gateway").andReturn(HTTP_OK, "{\"metadata\":{\"resourceVersion\":\"1002\"}}"))
                .times(2);
        mockServer.expect().put()
                .withPath("/apis/networking.istio.io/v1alpha3/namespaces/default/gateways/mygateway-https")
                .andReply(collector.record("put-cr-gateway").andReturn(HTTP_OK, "{}"))
                .once();

        // When
        applyService.applyGenericCustomResource(gateway, gatewayFragment.getName());
        applyService.applyGenericCustomResource(virtualService, virtualServiceFragment.getName());

        // Then
        collector.assertEventsRecordedInOrder("get-crds", "get-cr-gateway", "get-cr-gateway", "put-cr-gateway", "get-crds", "get-cr-virtualservice", "get-cr-virtualservice", "put-cr-virtualservice");
        assertEquals(9, mockServer.getMockServer().getRequestCount());
    }

    @Test
    public void testProcessCustomEntitiesRecreateModeTrue() throws Exception {
        // Given
        File gatewayFragment = new File(getClass().getResource("/gateway-cr.yml").getFile());
        File virtualServiceFragment = new File(getClass().getResource("/virtualservice-cr.yml").getFile());
        GenericCustomResource gateway = Serialization.yamlMapper().readValue(gatewayFragment, GenericCustomResource.class);
        GenericCustomResource virtualService = Serialization.yamlMapper().readValue(virtualServiceFragment, GenericCustomResource.class);
        WebServerEventCollector collector = new WebServerEventCollector();
        mockServer.expect().get()
                .withPath("/apis/apiextensions.k8s.io/v1beta1/customresourcedefinitions")
                .andReply(collector.record("get-crds").andReturn(HTTP_OK, new CustomResourceDefinitionListBuilder().withItems(virtualServiceCRD, gatewayCRD).build()))
                .times(2);
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
        applyService.applyGenericCustomResource(gateway, gatewayFragment.getName());
        applyService.applyGenericCustomResource(virtualService, virtualServiceFragment.getName());

        // Then
        collector.assertEventsRecordedInOrder("get-crds", "delete-cr-gateway", "post-cr-gateway", "get-crds", "delete-cr-virtualservice", "post-cr-virtualservice");
        assertEquals(7, mockServer.getMockServer().getRequestCount());
        applyService.setRecreateMode(false);
    }

    @Test
    public void testGetK8sListWithNamespaceFirstWithNamespace() {
        // Given
        List<HasMetadata> k8sList = new ArrayList<>();
        k8sList.add(new PodBuilder().withNewMetadata().withNamespace("p1").endMetadata().build());
        k8sList.add(new ConfigMapBuilder().withNewMetadata().withName("c1").endMetadata().build());
        k8sList.add(new NamespaceBuilder().withNewMetadata().withName("n1").endMetadata().build());
        k8sList.add(new DeploymentBuilder().withNewMetadata().withName("d1").endMetadata().build());

        // When
        List<HasMetadata> result = ApplyService.getK8sListWithNamespaceFirst(k8sList);

        // Then
        assertNotNull(result);
        assertEquals(k8sList.size(), result.size());
        assertTrue(result.get(0) instanceof Namespace);
    }

    @Test
    public void testGetK8sListWithNamespaceFirstWithProject() {
        // Given
        List<HasMetadata> k8sList = new ArrayList<>();
        k8sList.add(new PodBuilder().withNewMetadata().withNamespace("p1").endMetadata().build());
        k8sList.add(new ConfigMapBuilder().withNewMetadata().withName("c1").endMetadata().build());
        k8sList.add(new DeploymentConfigBuilder().withNewMetadata().withName("d1").endMetadata().build());
        k8sList.add(new ProjectBuilder().withNewMetadata().withName("project1").endMetadata().build());

        // When
        List<HasMetadata> result = ApplyService.getK8sListWithNamespaceFirst(k8sList);

        // Then
        assertNotNull(result);
        assertEquals(k8sList.size(), result.size());
        assertTrue(result.get(0) instanceof Project);
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