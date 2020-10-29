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
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.openshift.api.model.Project;
import io.fabric8.openshift.api.model.ProjectBuilder;
import io.fabric8.openshift.api.model.ProjectListBuilder;
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

import java.util.HashSet;
import java.util.Set;

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
    public void testApplyNamespaceOrProjectIfPresent() {
        // Given
        Set<HasMetadata> entities = new HashSet<>();
        Namespace namespace = new NamespaceBuilder().withNewMetadata().withName("ns1").endMetadata().build();
        Project project = new ProjectBuilder().withNewMetadata().withName("p1").endMetadata().build();
        entities.add(namespace);
        entities.add(project);
        WebServerEventCollector collector = new WebServerEventCollector();
        mockServer.expect().post()
                .withPath("/api/v1/namespaces")
                .andReply(collector.record("new-namespace").andReturn(HTTP_CREATED, namespace))
                .once();
        mockServer.expect().get()
                .withPath("/apis/project.openshift.io/v1/projects")
                .andReply(collector.record("get-project").andReturn(HTTP_OK, new ProjectListBuilder().build()))
                .once();
        mockServer.expect().post()
                .withPath("/apis/project.openshift.io/v1/projectrequests")
                .andReply(collector.record("new-projectrequest").andReturn(HTTP_CREATED, project))
                .once();

        // When
        applyService.applyNamespaceOrProjectIfPresent(entities);

        // Then
        collector.assertEventsRecordedInOrder( "get-project", "new-projectrequest", "new-namespace");
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
        applyService.applyEntities(fileName, entities, log, 5, null, null, null);

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
    public void testProcessCustomEntities() throws Exception {
        // Given
        List<String> crdNames = new ArrayList<>();
        crdNames.add("virtualservices.networking.istio.io");
        crdNames.add("gateways.networking.istio.io");
        File crFragmentDir = new File(getClass().getResource("/gateway-cr.yml").getFile()).getParentFile();
        WebServerEventCollector collector = new WebServerEventCollector();
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
        WebServerEventCollector collector = new WebServerEventCollector();
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
        WebServerEventCollector collector = new WebServerEventCollector();
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