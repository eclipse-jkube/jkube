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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import io.fabric8.kubernetes.api.model.APIGroupListBuilder;
import io.fabric8.kubernetes.api.model.APIResource;
import io.fabric8.kubernetes.api.model.APIResourceBuilder;
import io.fabric8.kubernetes.api.model.APIResourceListBuilder;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.GenericKubernetesResourceBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionBuilder;
import io.fabric8.kubernetes.api.model.authorization.v1.SelfSubjectAccessReviewBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleBindingBuilder;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.openshift.client.OpenShiftClient;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.Serialization;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.access.ClusterConfiguration;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.openshift.WebServerEventCollector;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyBuilder;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.fabric8.openshift.api.model.Project;
import io.fabric8.openshift.api.model.ProjectBuilder;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThat;

@EnableKubernetesMockClient
class ApplyServiceTest {

    KubernetesMockServer mockServer;
    OpenShiftClient client;

   private ApplyService applyService;

    @BeforeEach
    void setUp() {
      final JKubeServiceHub serviceHub = JKubeServiceHub.builder()
        .log(new KitLogger.SilentLogger())
        .configuration(JKubeConfiguration.builder().build())
        .platformMode(RuntimeMode.KUBERNETES)
        .clusterAccess(new ClusterAccess(ClusterConfiguration.from(client.getConfiguration()).build()))
        .build();
      applyService = new ApplyService(serviceHub);
      applyService.setNamespace("default");
      // In OpenShift
      mockServer.expect()
        .get()
        .withPath("/apis")
        .andReturn(200, new APIGroupListBuilder()
          .addNewGroup().withName("build.openshift.io").withApiVersion("v1").endGroup()
          .build())
        .always();
    }

    @Test
    void applyEntities() {
        // Given
        final Set<HasMetadata> entities = new LinkedHashSet<>(Arrays.asList(
            new DeploymentBuilder().withNewMetadata().withName("d1").endMetadata().build(),
            new ServiceBuilder().withNewMetadata().withName("svc1").endMetadata().build(),
            new ConfigMapBuilder().withNewMetadata().withName("c1").endMetadata().build(),
            new PodBuilder().withNewMetadata().withName("p1").endMetadata().build(),
            new ReplicationControllerBuilder().withNewMetadata().withName("rc1").endMetadata().build(),
            new NamespaceBuilder().withNewMetadata().withName("ns1").endMetadata().build(),
            new NetworkPolicyBuilder().withNewMetadata().withName("npv1").endMetadata().build(),
            new io.fabric8.kubernetes.api.model.extensions.NetworkPolicyBuilder().withNewMetadata().withName("np-ext").endMetadata().build(),
            new SecretBuilder().withNewMetadata().withName("secret1").endMetadata().build(),
            new RoleBindingBuilder().withNewMetadata().withName("rb1").endMetadata().build(),
            new ServiceAccountBuilder().withNewMetadata().withName("sa1").endMetadata().build(),
            new PersistentVolumeClaimBuilder().withNewMetadata().withName("pvc1").endMetadata().build(),
            new CustomResourceDefinitionBuilder().withNewMetadata().withName("crd1").endMetadata().build()
        ));
        String fileName = "foo.yml";
        WebServerEventCollector collector = new WebServerEventCollector();
        mockServer.expect().post()
            .withPath("/apis/apps/v1/namespaces/default/deployments")
            .andReply(collector.record("new-deploy").andReturn(HTTP_CREATED, ""))
            .once();
        mockServer.expect().post()
                .withPath("/api/v1/namespaces/default/services")
                .andReply(collector.record("new-service").andReturn(HTTP_CREATED, ""))
                .once();
        mockServer.expect().post()
                .withPath("/api/v1/namespaces/default/configmaps")
                .andReply(collector.record("new-configmap").andReturn(HTTP_CREATED, ""))
                .once();
        mockServer.expect().post()
                .withPath("/api/v1/namespaces/default/pods")
                .andReply(collector.record("new-pod").andReturn(HTTP_CREATED, ""))
                .once();
        mockServer.expect().post()
                .withPath("/api/v1/namespaces/default/replicationcontrollers")
                .andReply(collector.record("new-rc").andReturn(HTTP_CREATED, ""))
                .once();
        mockServer.expect().post()
                .withPath("/api/v1/namespaces")
                .andReply(collector.record("new-ns").andReturn(HTTP_CREATED, ""))
                .once();
        mockServer.expect().post()
            .withPath("/apis/networking.k8s.io/v1/namespaces/default/networkpolicies")
            .andReply(collector.record("new-np-v1").andReturn(HTTP_CREATED, ""))
            .once();
        mockServer.expect().post()
            .withPath("/apis/extensions/v1beta1/namespaces/default/networkpolicies")
            .andReply(collector.record("new-np-extensions").andReturn(HTTP_CREATED, ""))
            .once();
        mockServer.expect().post()
            .withPath("/api/v1/namespaces/default/secrets")
            .andReply(collector.record("new-secret").andReturn(HTTP_CREATED, ""))
            .once();
        mockServer.expect().post()
            .withPath("/apis/rbac.authorization.k8s.io/v1/namespaces/default/rolebindings")
            .andReply(collector.record("new-rb").andReturn(HTTP_CREATED, ""))
            .once();
        mockServer.expect().post()
            .withPath("/api/v1/namespaces/default/serviceaccounts")
            .andReply(collector.record("new-sa").andReturn(HTTP_CREATED, ""))
            .once();
        mockServer.expect().post()
            .withPath("/api/v1/namespaces/default/persistentvolumeclaims")
            .andReply(collector.record("new-pvc").andReturn(HTTP_CREATED, ""))
            .once();
        mockServer.expect().post()
            .withPath("/apis/apiextensions.k8s.io/v1/customresourcedefinitions")
            .andReply(collector.record("new-crd").andReturn(HTTP_CREATED, ""))
            .once();

        // When
        applyService.applyEntities(fileName, entities);

        // Then
        collector.assertEventsRecordedInOrder(
          "new-ns", "new-secret", "new-sa", "new-service", "new-rb", "new-pvc", "new-configmap", "new-deploy", "new-pod", "new-rc");
        collector.assertEventsRecorded("new-np-v1", "new-np-extensions", "new-crd");
    }

    @Test
    void createRoute() {
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
    void updateRoute() {
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
    void createRouteInServiceOnlyMode() {
        Route route = buildRoute();

        WebServerEventCollector collector = new WebServerEventCollector();
        mockServer.expect().get()
                .withPath("/apis/route.openshift.io/v1/namespaces/default/routes/route")
                .andReply(collector.record("get-route").andReturn(HTTP_NOT_FOUND, ""))
                .always();

        applyService.setServicesOnlyMode(true);
        applyService.apply(route, "route.yml");

        collector.assertEventsNotRecorded("get-route");
        assertThat(mockServer.getRequestCount()).isEqualTo(1);
    }

    @Test
    void createRouteNotAllowed() {
        Route route = buildRoute();

        WebServerEventCollector collector = new WebServerEventCollector();
        mockServer.expect().get()
                .withPath("/apis/route.openshift.io/v1/namespaces/default/routes/route")
                .andReply(collector.record("get-route").andReturn(HTTP_NOT_FOUND, ""))
                .always();

        applyService.setAllowCreate(false);
        applyService.apply(route, "route.yml");

        collector.assertEventsRecordedInOrder("get-route");
        assertThat(mockServer.getRequestCount()).isEqualTo(3);
    }

    @Test
    void applyGenericKubernetesResource() throws Exception {
        // Given
        File gatewayFragment = new File(getClass().getResource("/gateway-cr.yml").getFile());
        File virtualServiceFragment = new File(getClass().getResource("/virtualservice-cr.yml").getFile());
        GenericKubernetesResource gateway = Serialization.unmarshal(gatewayFragment, GenericKubernetesResource.class);
        GenericKubernetesResource virtualService = Serialization.unmarshal(virtualServiceFragment, GenericKubernetesResource.class);
        WebServerEventCollector collector = new WebServerEventCollector();
        mockServer.expect().get()
                .withPath("/apis/networking.istio.io/v1alpha3")
                .andReply(collector.record("get-resources").andReturn(HTTP_OK, new APIResourceListBuilder()
                    .addToResources(virtualServiceResource(), gatewayResource()).build()))
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
        applyService.applyGenericKubernetesResource(gateway, gatewayFragment.getName());
        applyService.applyGenericKubernetesResource(virtualService, virtualServiceFragment.getName());

        // Then
        collector.assertEventsRecordedInOrder("get-resources", "post-cr-gateway", "get-resources", "post-cr-virtualservice");
        assertThat(mockServer.getRequestCount()).isEqualTo(6);
    }

    @Test
    void processCustomEntitiesReplaceCustomResources() throws Exception {
        // Given
        File gatewayFragment = new File(getClass().getResource("/gateway-cr.yml").getFile());
        File virtualServiceFragment = new File(getClass().getResource("/virtualservice-cr.yml").getFile());
        GenericKubernetesResource gateway = Serialization.unmarshal(gatewayFragment, GenericKubernetesResource.class);
        GenericKubernetesResource virtualService = Serialization.unmarshal(virtualServiceFragment, GenericKubernetesResource.class);
        WebServerEventCollector collector = new WebServerEventCollector();
        mockServer.expect().get()
                 .withPath("/apis/networking.istio.io/v1alpha3")
                 .andReply(collector.record("get-resources").andReturn(HTTP_OK, new APIResourceListBuilder()
                     .addToResources(virtualServiceResource(), gatewayResource()).build()))
                 .times(2);
        mockServer.expect().get()
                .withPath("/apis/networking.istio.io/v1alpha3/namespaces/default/virtualservices/reviews-route")
                .andReply(collector.record("get-cr-virtualservice").andReturn(HTTP_OK, "{\"metadata\":{\"resourceVersion\":\"1001\"}}"))
                .once();
        mockServer.expect().post()
                .withPath("/apis/networking.istio.io/v1alpha3/namespaces/default/virtualservices")
                .andReply(collector.record("post-cr-virtualservice").andReturn(HTTP_OK, "{}"))
                .once();
        mockServer.expect().get()
                .withPath("/apis/networking.istio.io/v1alpha3/namespaces/default/gateways/mygateway-https")
                .andReply(collector.record("get-cr-gateway").andReturn(HTTP_OK, "{\"metadata\":{\"resourceVersion\":\"1002\"}}"))
                .once();
        mockServer.expect().post()
                .withPath("/apis/networking.istio.io/v1alpha3/namespaces/default/gateways")
                .andReply(collector.record("post-cr-gateway").andReturn(HTTP_CONFLICT, "{}"))
                .once();
        mockServer.expect().put()
            .withPath("/apis/networking.istio.io/v1alpha3/namespaces/default/gateways/mygateway-https")
            .andReply(collector.record("put-cr-gateway").andReturn(HTTP_OK, "{\"metadata\":{\"resourceVersion\":\"1002\"}}"))
            .once();

        // When
        applyService.applyGenericKubernetesResource(gateway, gatewayFragment.getName());
        applyService.applyGenericKubernetesResource(virtualService, virtualServiceFragment.getName());

        // Then
        collector.assertEventsRecordedInOrder("get-cr-gateway", "post-cr-gateway", "put-cr-gateway",
             "get-cr-virtualservice", "post-cr-virtualservice");
        assertThat(mockServer.getRequestCount()).isEqualTo(7);
    }

    @Test
    void processCustomEntitiesRecreateModeTrue() throws Exception {
        // Given
        File gatewayFragment = new File(getClass().getResource("/gateway-cr.yml").getFile());
        File virtualServiceFragment = new File(getClass().getResource("/virtualservice-cr.yml").getFile());
        GenericKubernetesResource gateway = Serialization.unmarshal(gatewayFragment, GenericKubernetesResource.class);
        GenericKubernetesResource virtualService = Serialization.unmarshal(virtualServiceFragment, GenericKubernetesResource.class);
        WebServerEventCollector collector = new WebServerEventCollector();
        mockServer.expect().get()
            .withPath("/apis/networking.istio.io/v1alpha3")
            .andReply(collector.record("get-resources").andReturn(HTTP_OK, new APIResourceListBuilder()
                .addToResources(virtualServiceResource(), gatewayResource()).build()))
            .times(2);
        mockServer.expect().get()
            .withPath("/apis/networking.istio.io/v1alpha3/namespaces/default/gateways?fieldSelector=metadata.name%3Dmygateway-https")
            .andReturn(HTTP_OK, new GenericKubernetesResourceBuilder()
                .withApiVersion("networking.istio.io/v1alpha3")
                .withKind("Gateway")
                .withNewMetadata().withName("mygateway-https").endMetadata()
                .build())
            .once();
        mockServer.expect().get()
            .withPath("/apis/networking.istio.io/v1alpha3/namespaces/default/virtualservices?fieldSelector=metadata.name%3Dreviews-route")
            .andReturn(HTTP_OK, new GenericKubernetesResourceBuilder()
                .withApiVersion("networking.istio.io/v1alpha3")
                .withKind("VirtualService")
                .withNewMetadata().withName("reviews-route").endMetadata()
                .build())
            .once();
        mockServer.expect().delete()
            .withPath("/apis/networking.istio.io/v1alpha3/namespaces/default/virtualservices/reviews-route")
            .andReply(collector.record("delete-cr-virtualservice")
            .andReturn(HTTP_OK, "{\"kind\":\"Status\",\"apiVersion\":\"v1\",\"status\":\"Success\"}"))
            .once();
        mockServer.expect().delete()
            .withPath("/apis/networking.istio.io/v1alpha3/namespaces/default/gateways/mygateway-https")
            .andReply(collector.record("delete-cr-gateway")
            .andReturn(HTTP_OK, "{\"kind\":\"Status\",\"apiVersion\":\"v1\",\"status\":\"Success\"}"))
            .once();
        mockServer.expect().post()
            .withPath("/apis/networking.istio.io/v1alpha3/namespaces/default/virtualservices")
            .andReply(collector.record("post-cr-virtualservice")
            .andReturn(HTTP_OK, "{\"kind\":\"VirtualService\",\"apiVersion\":\"networking.istio.io/v1alpha3\"}"))
            .once();
        mockServer.expect().post()
            .withPath("/apis/networking.istio.io/v1alpha3/namespaces/default/gateways")
            .andReply(collector.record("post-cr-gateway")
            .andReturn(HTTP_OK, "{\"kind\":\"Gateway\",\"apiVersion\":\"networking.istio.io/v1alpha3\"}"))
            .once();
        applyService.setRecreateMode(true);

        // When
        applyService.applyGenericKubernetesResource(gateway, gatewayFragment.getName());
        applyService.applyGenericKubernetesResource(virtualService, virtualServiceFragment.getName());

        // Then
        collector.assertEventsRecordedInOrder( "delete-cr-gateway", "post-cr-gateway", "delete-cr-virtualservice", "post-cr-virtualservice");
        assertThat(mockServer.getRequestCount()).isEqualTo(10);
        applyService.setRecreateMode(false);
    }

    @Test
    void getK8sListWithNamespaceFirstWithNamespace() {
        // Given
        List<HasMetadata> k8sList = new ArrayList<>();
        k8sList.add(new PodBuilder().withNewMetadata().withNamespace("p1").endMetadata().build());
        k8sList.add(new ConfigMapBuilder().withNewMetadata().withName("c1").endMetadata().build());
        k8sList.add(new NamespaceBuilder().withNewMetadata().withName("n1").endMetadata().build());
        k8sList.add(new DeploymentBuilder().withNewMetadata().withName("d1").endMetadata().build());

        // When
        List<HasMetadata> result = ApplyService.getK8sListWithNamespaceFirst(k8sList);

        // Then
        assertThat(result).isNotNull()
            .hasSameSizeAs(k8sList)
            .first()
            .isInstanceOf(Namespace.class);
    }

    @Test
    void getK8sListWithNamespaceFirstWithProject() {
        // Given
        List<HasMetadata> k8sList = new ArrayList<>();
        k8sList.add(new PodBuilder().withNewMetadata().withNamespace("p1").endMetadata().build());
        k8sList.add(new ConfigMapBuilder().withNewMetadata().withName("c1").endMetadata().build());
        k8sList.add(new DeploymentConfigBuilder().withNewMetadata().withName("d1").endMetadata().build());
        k8sList.add(new ProjectBuilder().withNewMetadata().withName("project1").endMetadata().build());

        // When
        List<HasMetadata> result = ApplyService.getK8sListWithNamespaceFirst(k8sList);

        // Then
        assertThat(result).isNotNull()
            .hasSameSizeAs(k8sList)
            .first()
            .isInstanceOf(Project.class);
    }

    @Test
    void applyToMultipleNamespaceNoNamespaceConfigured() {
        // Given
        ConfigMap configMap = new ConfigMapBuilder().withNewMetadata().withName("cm1").withNamespace("ns1").endMetadata().build();
        Ingress ingress = new IngressBuilder().withNewMetadata().withName("ing1").withNamespace("ns2").endMetadata().build();
        ServiceAccount serviceAccount = new ServiceAccountBuilder().withNewMetadata().withName("sa1").endMetadata().build();

        List<HasMetadata> entities = new ArrayList<>();
        entities.add(configMap);
        entities.add(serviceAccount);
        entities.add(ingress);
        WebServerEventCollector collector = new WebServerEventCollector();
        mockServer.expect().post().withPath("/api/v1/namespaces/ns1/configmaps")
                .andReply(collector.record("configmap-ns1-create").andReturn(HTTP_OK, configMap))
                .once();
        mockServer.expect().post().withPath("/apis/networking.k8s.io/v1/namespaces/ns2/ingresses")
                .andReply(collector.record("ingress-ns2-create").andReturn(HTTP_OK, ingress))
                .once();
        mockServer.expect().post().withPath("/apis/authorization.k8s.io/v1/selfsubjectaccessreviews")
            .andReturn(HTTP_OK, new SelfSubjectAccessReviewBuilder().withNewStatus().withAllowed(false).endStatus().build())
            .always();
        mockServer.expect().post().withPath("/api/v1/namespaces/default/serviceaccounts")
                .andReply(collector.record("serviceaccount-default-create").andReturn(HTTP_OK, serviceAccount))
                .once();
        String configuredNamespace = applyService.getNamespace();
        applyService.setNamespace(null);
        applyService.setFallbackNamespace("default");

        // When
        applyService.applyEntities(null, entities);

        // Then
        collector.assertEventsRecordedInOrder("serviceaccount-default-create", "configmap-ns1-create", "ingress-ns2-create");
        assertThat(mockServer.getRequestCount()).isEqualTo(10);
        applyService.setFallbackNamespace(null);
        applyService.setNamespace(configuredNamespace);
    }

    @Test
    void applyToMultipleNamespacesOverriddenWhenNamespaceConfigured() {
        // Given
        ConfigMap configMap = new ConfigMapBuilder().withNewMetadata().withName("cm1").withNamespace("default").endMetadata().build();
        Ingress ingress = new IngressBuilder().withNewMetadata().withName("ing1").withNamespace("default").endMetadata().build();
        ServiceAccount serviceAccount = new ServiceAccountBuilder().withNewMetadata().withName("default").endMetadata().build();

        List<HasMetadata> entities = new ArrayList<>();
        entities.add(configMap);
        entities.add(serviceAccount);
        entities.add(ingress);
        WebServerEventCollector collector = new WebServerEventCollector();
        mockServer.expect().post().withPath("/api/v1/namespaces/default/configmaps")
                .andReply(collector.record("configmap-default-ns-create").andReturn(HTTP_OK, configMap))
                .once();
        mockServer.expect().post().withPath("/apis/networking.k8s.io/v1/namespaces/default/ingresses")
                .andReply(collector.record("ingress-default-ns-create").andReturn(HTTP_OK, ingress))
                .once();
        mockServer.expect().post().withPath("/apis/authorization.k8s.io/v1/selfsubjectaccessreviews")
                .andReturn(HTTP_OK, new SelfSubjectAccessReviewBuilder().withNewStatus().withAllowed(false).endStatus().build())
                .always();
        mockServer.expect().post().withPath("/api/v1/namespaces/default/serviceaccounts")
                .andReply(collector.record("serviceaccount-default-ns-create").andReturn(HTTP_OK, serviceAccount))
                .once();

        // When
        applyService.applyEntities(null, entities);

        // Then
        collector.assertEventsRecordedInOrder("serviceaccount-default-ns-create", "configmap-default-ns-create", "ingress-default-ns-create");
        assertThat(mockServer.getRequestCount()).isEqualTo(10);
        applyService.setFallbackNamespace(null);
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

    private static APIResource virtualServiceResource() {
        return new APIResourceBuilder()
            .withName("gateways")
            .withNamespaced(true)
            .withGroup("networking.istio.io")
            .withKind("Gateway")
            .withSingularName("gateway")
            .build();
    }

    private static APIResource gatewayResource() {
        return new APIResourceBuilder()
            .withName("virtualservices")
            .withNamespaced(true)
            .withGroup("networking.istio.io")
            .withKind("VirtualService")
            .withSingularName("virtualservice")
            .build();
    }
}
