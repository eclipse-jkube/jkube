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
package org.eclipse.jkube.kit.common.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jkube.kit.common.KitLogger;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.GenericKubernetesResourceBuilder;
import io.fabric8.kubernetes.api.model.HTTPHeader;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.apps.DaemonSetBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.ServiceResource;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.fabric8.openshift.api.model.Template;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked"})
class KubernetesHelperTest {

    private KitLogger logger;

    @BeforeEach
    public void setUp() throws Exception {
        logger = new KitLogger.SilentLogger();
    }

    @Test
    void testListResourceFragments() {
        // Given
        File localResourceDir = new File(getClass().getResource("/util/fragments").getPath());

        // When & Then
        assertLocalFragments(KubernetesHelper.listResourceFragments(null, logger, localResourceDir), 2);
    }

    @Test
    void testResourceFragmentsWithRemotes() {
        // Given
        List<String> remoteStrList = getRemoteFragments();
        File localResourceDir = new File(getClass().getResource("/util/fragments").getPath());

        // When
        File[] fragments = KubernetesHelper.listResourceFragments(remoteStrList, logger, localResourceDir);

        // Then
        assertLocalFragments(fragments, 4);
        assertThat(Arrays.stream(fragments).anyMatch( f -> f.getName().equals("deployment.yaml"))).isTrue();
        assertThat(Arrays.stream(fragments).anyMatch( f -> f.getName().equals("sa.yml"))).isTrue();
    }

    @Test
    void testGetResourceFragmentFromSourceWithSomeResourceDirAndNullRemotes() {
        // Given
        File localResourceDir = new File(getClass().getResource("/util/fragments").getPath());

        // When
        File fragmentFile = KubernetesHelper.getResourceFragmentFromSource(localResourceDir, Collections.emptyList(), "service.yml", logger);

        // Then
        assertThat(fragmentFile).isNotNull()
                .exists()
                .hasName("service.yml");
    }

    @Test
    void testGetResourceFragmentWithNullResourceDirAndNullRemotes() {
        assertThat(KubernetesHelper.getResourceFragmentFromSource(null, null, "service.yml", logger)).isNull();
    }

    @Test
    void testGetResourceFragmentFromSourceWithNullResourceDirAndSomeRemotes() {
        // Given
        List<String> remotes = getRemoteFragments();

        // When
        File fragmentFile = KubernetesHelper.getResourceFragmentFromSource(null, remotes, "deployment.yaml", logger);

        // Then
        assertThat(fragmentFile).isNotNull()
                .exists()
                .hasName("deployment.yaml");
    }

    @Test
    void testGetResourceFragmentFromSourceWithSomeResourceDirAndSomeRemotes() {
        // Given
        File localResourceDir = new File(getClass().getResource("/util/fragments").getPath());
        List<String> remotes = getRemoteFragments();

        // When
        File fragmentFile = KubernetesHelper.getResourceFragmentFromSource(localResourceDir, remotes, "sa.yml", logger);

        // Then
        assertThat(fragmentFile).isNotNull()
                .exists()
                .hasName("sa.yml");
    }

    @Test
    void testGetQuantityFromString() {
        // Given
        Map<String, String> limitsAsStr = new HashMap<>();
        limitsAsStr.put("cpu", "200m");
        limitsAsStr.put("memory", "1Gi");

        // When
        Map<String, Quantity> limitAsQuantity = KubernetesHelper.getQuantityFromString(limitsAsStr);

        // Then
        assertThat(limitAsQuantity).isNotNull()
                .hasSize(2)
                .contains(
                        entry("cpu", new Quantity("200m")),
                        entry("memory", new Quantity("1Gi")));
    }

    @Test
    void testGetEnvVar() {
        // Given
        List<EnvVar> envVarList = prepareEnvVarList();

        // When
        String value1 = KubernetesHelper.getEnvVar(envVarList, "env1", "defaultValue");
        String value2 = KubernetesHelper.getEnvVar(envVarList, "JAVA_OPTIONS", "defaultValue");
        String value3 = KubernetesHelper.getEnvVar(envVarList, "FOO", "defaultValue");
        String value4 = KubernetesHelper.getEnvVar(envVarList, "UNKNOWN", "defaultValue");

        // Then
        assertThat(value1).isEqualTo("value1");
        assertThat(value2).isEqualTo("-Dfoo=bar -Dxyz=abc");
        assertThat(value3).isEqualTo("BAR");
        assertThat(value4).isEqualTo("defaultValue");
    }

    @Test
    void testSetEnvVar() {
        // Given
        List<EnvVar> envVarList = prepareEnvVarList();

        // When
        boolean statusCode1 = KubernetesHelper.setEnvVar(envVarList, "FOO", "NEW_BAR");
        boolean statusCode2 = KubernetesHelper.setEnvVar(envVarList, "UNKNOWN_KEY", "UNKNOWN_VALUE");

        // Then
        assertThat(statusCode1).isTrue();
        assertThat(statusCode2).isTrue();
        assertThat(KubernetesHelper.getEnvVar(envVarList, "FOO", "defaultValue")).isEqualTo("NEW_BAR");
        assertThat(KubernetesHelper.getEnvVar(envVarList, "UNKNOWN_KEY", "defaultValue")).isEqualTo("UNKNOWN_VALUE");
    }

    @Test
    void testRemoveEnvVar() {
        // Given
        List<EnvVar> envVarList = prepareEnvVarList();

        // When
        boolean statusCode1 = KubernetesHelper.removeEnvVar(envVarList, "FOO");

        // Then
        assertThat(statusCode1).isTrue();
        assertThat(KubernetesHelper.getEnvVar(envVarList, "FOO", "defaultValue")).isEqualTo("defaultValue");
    }

    @Test
    void testIsExposedServiceReturnsTrue() {
        // Given
        Service service = new ServiceBuilder()
                .withNewMetadata()
                .addToLabels("expose", "true")
                .withName("svc1")
                .endMetadata()
                .build();

        // When
        boolean result = KubernetesHelper.isExposeService(service);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void testIsExposedServiceReturnsFalse() {
        // Given
        Service service = new ServiceBuilder().withNewMetadata().withName("svc1").endMetadata().build();

        // When
        boolean result = KubernetesHelper.isExposeService(service);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void testGetAnnotationValue() {
        // Given
        Service svc = new ServiceBuilder()
                .withNewMetadata()
                .withName("svc1")
                .addToAnnotations("expose", "true")
                .addToAnnotations("exposeUrl", "http://12.4.1.4:8223/test")
                .endMetadata().build();
        Service svc2 = null;

        // When
        String result1 = KubernetesHelper.getAnnotationValue(svc, "expose");
        String result2 = KubernetesHelper.getAnnotationValue(svc, "exposeUrl");
        String result3 = KubernetesHelper.getAnnotationValue(svc, "iDontExist");
        String result4 = KubernetesHelper.getAnnotationValue(svc2, "expose");

        // Then
        assertThat(result1).isEqualTo("true");
        assertThat(result2).isEqualTo("http://12.4.1.4:8223/test");
        assertThat(result3).isNull();
        assertThat(result4).isNull();
    }

    @Test
    void testConvertToEnvVarList() {
        // Given
        Map<String, String> envVarAsStringMap = new HashMap<>();
        envVarAsStringMap.put("env1", "value1");
        envVarAsStringMap.put("JAVA_OPTIONS", "-Dfoo=bar -Dxyz=abc");
        envVarAsStringMap.put("FOO", "BAR");

        // When
        List<EnvVar> envVarList = KubernetesHelper.convertToEnvVarList(envVarAsStringMap);

        // Then
        assertThat(envVarList).isNotNull().hasSize(3);
        assertThat(KubernetesHelper.getEnvVar(envVarList, "env1", "defaultValue")).isEqualTo("value1");
        assertThat(KubernetesHelper.getEnvVar(envVarList, "JAVA_OPTIONS", "defaultValue")).isEqualTo("-Dfoo=bar -Dxyz=abc");
        assertThat(KubernetesHelper.getEnvVar(envVarList, "FOO", "defaultValue")).isEqualTo("BAR");

    }

    @Test
    void testGetServiceExposeUrlReturnsUrlFromAnnotation() throws InterruptedException {
        // Given
        Service svc = new ServiceBuilder().withNewMetadata().withName("svc1").endMetadata().build();
        Set<HasMetadata> entities = new HashSet<>();
        entities.add(svc);
        final NamespacedKubernetesClient kubernetesClient = mock(NamespacedKubernetesClient.class);
        final MixedOperation<Service,ServiceList, ServiceResource<Service>> svcMixedOp = mock(MixedOperation.class);
        when(kubernetesClient.services()).thenReturn(svcMixedOp);
        final ServiceResource<Service> svcResource = mock(ServiceResource.class);
        when(svcMixedOp.withName("svc1")).thenReturn(svcResource);
        when(svcResource.get()).thenReturn(new ServiceBuilder()
            .withNewMetadata()
            .withName("svc1")
            .addToAnnotations("exposeUrl", "https://example.com")
            .endMetadata()
            .build());
        // When
        String result = KubernetesHelper.getServiceExposeUrl(kubernetesClient, entities, 3, "exposeUrl");
        // Then
        assertThat(result).isEqualTo("https://example.com");
    }

    @Test
    void testGetServiceExposeUrlReturnsNull() throws InterruptedException {
        // Given
        Service svc = new ServiceBuilder().withNewMetadata().withName("svc1").endMetadata().build();
        Set<HasMetadata> entities = new HashSet<>();
        entities.add(svc);
        final NamespacedKubernetesClient kubernetesClient = mock(NamespacedKubernetesClient.class);
        final MixedOperation<Service,ServiceList, ServiceResource<Service>> svcMixedOp = mock(MixedOperation.class);
        when(kubernetesClient.services()).thenReturn(svcMixedOp);
        final ServiceResource<Service> svcResource = mock(ServiceResource.class);
        when(svcMixedOp.withName("svc1")).thenReturn(svcResource);
        when(svcResource.get()).thenReturn(new ServiceBuilder()
                .withNewMetadata()
                .withName("svc1")
                .endMetadata()
                .build());
        // When
        String result = KubernetesHelper.getServiceExposeUrl(kubernetesClient, entities, 1, "exposeUrl");
        // Then
        assertThat(result).isNull();
        verify(kubernetesClient, times(1)).services();
        verify(svcResource,times(1)).get();
    }

    @Test
    void testGetFullyQualifiedApiGroupWithKind() {
        // Given
        GenericKubernetesResource cr1 = new GenericKubernetesResourceBuilder()
            .withApiVersion("networking.istio.io/v1alpha3")
            .withKind("VirtualService")
            .build();
        GenericKubernetesResource cr2 = new GenericKubernetesResourceBuilder()
            .withApiVersion("networking.istio.io/v1alpha3")
            .withKind("Gateway")
            .build();

        // When
        String result1 = KubernetesHelper.getFullyQualifiedApiGroupWithKind(cr1);
        String result2 = KubernetesHelper.getFullyQualifiedApiGroupWithKind(cr2);

        // Then
        assertThat(result1).isEqualTo("networking.istio.io/v1alpha3#VirtualService");
        assertThat(result2).isEqualTo("networking.istio.io/v1alpha3#Gateway");
    }

    @Test
    void testContainsPort() {
        // Given
        List<ContainerPort> ports = new ArrayList<>();
        ports.add(new ContainerPortBuilder().withName("p1").withContainerPort(8001).build());
        ports.add(new ContainerPortBuilder().withName("p2").withContainerPort(8002).build());

        // When
        boolean result1 = KubernetesHelper.containsPort(ports, "8001");
        boolean result2 = KubernetesHelper.containsPort(ports, "8002");

        // Then
        assertThat(result1).isTrue();
        assertThat(result2).isTrue();
    }

    @Test
    void testAddPort() {
        // When
        ContainerPort result = KubernetesHelper.addPort("8001", "p1", logger);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("p1");
        assertThat(result.getContainerPort().intValue()).isEqualTo(8001);
    }

    @Test
    void testAddPortNullPortNumber() {
        assertThat(KubernetesHelper.addPort("", "", logger)).isNull();
    }

    @Test
    void testAddPortWithInvalidPortNumber() {
        assertThat(KubernetesHelper.addPort("90invalid", "", logger)).isNull();
    }

    @Test
    void testIsControllerResource() {
        assertThat(KubernetesHelper.isControllerResource(new DeploymentBuilder().build())).isTrue();
        assertThat(KubernetesHelper.isControllerResource(new StatefulSetBuilder().build())).isTrue();
        assertThat(KubernetesHelper.isControllerResource(new ReplicationControllerBuilder().build())).isTrue();
        assertThat(KubernetesHelper.isControllerResource(new ReplicaSetBuilder().build())).isTrue();
        assertThat(KubernetesHelper.isControllerResource(new DeploymentConfigBuilder().build())).isTrue();
        assertThat(KubernetesHelper.isControllerResource(new DaemonSetBuilder().build())).isTrue();
        assertThat(KubernetesHelper.isControllerResource(new ConfigMapBuilder().build())).isFalse();
    }

    @Test
    void loadResourcesWithNestedTemplateAndDuplicateResources() throws IOException {
        // Given
        final File manifest = new File(KubernetesHelperTest.class.getResource(
            "/util/kubernetes-helper/list-with-duplicates-and-template.yml").getFile());
        // When
        final List<HasMetadata> result = KubernetesHelper.loadResources(manifest);
        // Then
        assertThat(result)
            .hasSize(3)
            .hasOnlyElementsOfTypes(Namespace.class, GenericKubernetesResource.class, Template.class)
            .extracting("metadata.name")
            .containsExactly("should-be-first", "custom-resource", "template-example");
    }

    @Test
    void loadResourcesWithDuplicateAndSameNameCustomResources() throws IOException {
        // Given
        final File manifest = new File(KubernetesHelperTest.class.getResource(
            "/util/kubernetes-helper/list-with-duplicates-and-same-name-custom-resource.yml").getFile());
        // When
        final List<HasMetadata> result = KubernetesHelper.loadResources(manifest);
        // Then
        assertThat(result)
            .hasSize(3)
            .hasOnlyElementsOfTypes(Namespace.class, GenericKubernetesResource.class)
            .extracting("metadata.name")
            .containsExactly("should-be-first", "custom-resource", "custom-resource");
    }

    @Test
    void testConvertMapToHTTPHeaderList() {
        // Given
        Map<String, String> headerAsMap = new HashMap<>();
        headerAsMap.put("Accept", "application/json");
        headerAsMap.put("User-Agent", "MyUserAgent");

        // When
        List<HTTPHeader> httpHeaders = KubernetesHelper.convertMapToHTTPHeaderList(headerAsMap);

        // Then
        assertThat(httpHeaders).isNotNull().hasSize(2)
                .satisfies(h -> assertThat(h).element(0)
                     .hasFieldOrPropertyWithValue("name", "Accept")
                     .hasFieldOrPropertyWithValue("value", "application/json"))
                .satisfies(h -> assertThat(h).element(1)
                     .hasFieldOrPropertyWithValue("name", "User-Agent")
                     .hasFieldOrPropertyWithValue("value", "MyUserAgent"));
    }

    @Test
    void extractPodLabelSelector_withJobWithSelector_shouldReturnSelector() {
        // Given
        final KubernetesList list = new KubernetesListBuilder()
            .addToItems(new JobBuilder()
                .withNewSpec()
                .withNewSelector().addToMatchLabels("selector", "label").endSelector()
                .withNewTemplate().withNewMetadata().addToLabels("template", "label").endMetadata().endTemplate()
                .endSpec()
                .build())
            .build();
        // When
        final LabelSelector result = KubernetesHelper.extractPodLabelSelector(list.getItems());
        // Then
        assertThat(result.getMatchLabels())
            .hasSize(1)
            .containsEntry("selector", "label");
    }

    @Test
    void extractPodLabelSelector_withJobWithNoSelector_shouldReturnTemplateLabels() {
        // Given
        final KubernetesList list = new KubernetesListBuilder()
            .addToItems(new JobBuilder()
                .withNewSpec()
                .withNewTemplate().withNewMetadata().addToLabels("template", "label").endMetadata().endTemplate()
                .endSpec()
                .build())
            .build();
        // When
        final LabelSelector result = KubernetesHelper.extractPodLabelSelector(list.getItems());
        // Then
        assertThat(result.getMatchLabels())
            .hasSize(1)
            .containsEntry("template", "label");
    }

    private void assertLocalFragments(File[] fragments, int expectedSize) {
        assertThat(fragments).hasSize(expectedSize);
        assertThat(Arrays.stream(fragments).anyMatch( f -> f.getName().equals("deployment.yml"))).isTrue();
        assertThat(Arrays.stream(fragments).anyMatch( f -> f.getName().equals("service.yml"))).isTrue();
    }

    private List<EnvVar> prepareEnvVarList() {
        List<EnvVar> envVarList = new ArrayList<>();
        envVarList.add(new EnvVarBuilder().withName("env1").withValue("value1").build());
        envVarList.add(new EnvVarBuilder().withName("JAVA_OPTIONS").withValue("-Dfoo=bar -Dxyz=abc").build());
        envVarList.add(new EnvVarBuilder().withName("FOO").withValue("BAR").build());

        return envVarList;
    }

    private List<String> getRemoteFragments() {
        List<String> remoteStrList = new ArrayList<>();

        remoteStrList.add("https://gist.githubusercontent.com/lordofthejars/ac2823cec7831697d09444bbaa76cd50/raw/e4b43f1b6494766dfc635b5959af7730c1a58a93/deployment.yaml");
        remoteStrList.add("https://gist.githubusercontent.com/rohanKanojia/c4ac4ae5533f0bf0dd77d13c905face7/raw/8a7de1e27c1f437c1ccbd186ed247efd967953ee/sa.yml");
        return remoteStrList;
    }
}
