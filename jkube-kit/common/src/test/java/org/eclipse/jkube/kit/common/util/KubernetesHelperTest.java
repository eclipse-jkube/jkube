/*
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
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.authorization.v1.SelfSubjectAccessReview;
import io.fabric8.kubernetes.api.model.authorization.v1.SelfSubjectAccessReviewBuilder;
import io.fabric8.kubernetes.api.model.runtime.RawExtension;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
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
import io.fabric8.kubernetes.api.model.apps.DaemonSetBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.fabric8.openshift.api.model.Template;
import org.eclipse.jkube.kit.common.TestHttpStaticServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.eclipse.jkube.kit.common.util.KubernetesHelper.hasAccessForAction;

@EnableKubernetesMockClient(crud = true)
class KubernetesHelperTest {

    private KitLogger logger;
    KubernetesMockServer mockServer;
    KubernetesClient mockClient;

    @BeforeEach
    public void setUp() {
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
    void resourceFragmentsWithRemotes() throws IOException {
        File remoteDirectory = new File(getClass().getResource("/remote-resources").getFile());
        try (TestHttpStaticServer http = new TestHttpStaticServer(remoteDirectory)) {
            // Given
            List<String> remoteStrList = getRemoteFragments(http.getPort());
            File localResourceDir = new File(getClass().getResource("/util/fragments").getPath());

            // When
            File[] fragments = KubernetesHelper.listResourceFragments(remoteStrList, logger, localResourceDir);

            // Then
            assertLocalFragments(fragments, 4);
            assertThat(Arrays.stream(fragments).anyMatch( f -> f.getName().equals("deployment.yaml"))).isTrue();
            assertThat(Arrays.stream(fragments).anyMatch( f -> f.getName().equals("sa.yml"))).isTrue();
        }
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
    void getResourceFragmentFromSourceWithNullResourceDirAndSomeRemotes() throws IOException {
        File remoteDirectory = new File(getClass().getResource("/remote-resources").getFile());
        try (TestHttpStaticServer http = new TestHttpStaticServer(remoteDirectory)) {
            // Given
            List<String> remotes = getRemoteFragments(http.getPort());

            // When
            File fragmentFile = KubernetesHelper.getResourceFragmentFromSource(null, remotes, "deployment.yaml", logger);

            // Then
            assertThat(fragmentFile).isNotNull()
                .exists()
                .hasName("deployment.yaml");
        }
    }

    @Test
    void getResourceFragmentFromSourceWithSomeResourceDirAndSomeRemotes() throws IOException {
        File remoteDirectory = new File(getClass().getResource("/remote-resources").getFile());
        try (TestHttpStaticServer http = new TestHttpStaticServer(remoteDirectory)) {
            // Given
            File localResourceDir = new File(getClass().getResource("/util/fragments").getPath());
            List<String> remotes = getRemoteFragments(http.getPort());

            // When
            File fragmentFile = KubernetesHelper.getResourceFragmentFromSource(localResourceDir, remotes, "sa.yml", logger);

            // Then
            assertThat(fragmentFile).isNotNull()
                .exists()
                .hasName("sa.yml");
        }
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

    @ParameterizedTest
    @MethodSource("controllerResources")
    void isControllerResource_withController_returnsTrue(HasMetadata resource) {
        assertThat(KubernetesHelper.isControllerResource(resource)).isTrue();
    }

    static Stream<Arguments> controllerResources() {
        return Stream.of(
            Arguments.of(new DeploymentBuilder().build()),
            Arguments.of(new StatefulSetBuilder().build()),
            Arguments.of(new ReplicationControllerBuilder().build()),
            Arguments.of(new ReplicaSetBuilder().build()),
            Arguments.of(new DeploymentConfigBuilder().build()),
            Arguments.of(new DaemonSetBuilder().build())
        );
    }

    @Test
    void isControllerResource_withNonController_returnsFalse() {
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

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void hasAccessForAction_whenApiServerReturnsAccessReviewWithStatus_thenReturnAllowed(boolean allowed) {
        // Given
        final AtomicReference<SelfSubjectAccessReview> requestedSSAR = new AtomicReference<>();
        mockServer.expect()
          .post()
          .withPath("/apis/authorization.k8s.io/v1/selfsubjectaccessreviews")
          .andReply(200, recordedRequest ->
            new SelfSubjectAccessReviewBuilder(requestedSSAR.updateAndGet(old ->
              Serialization.unmarshal(recordedRequest.getBody().inputStream(), SelfSubjectAccessReview.class)))
              .withNewStatus()
              .withAllowed(allowed)
              .endStatus()
              .build())
          .always();
        // When
        final boolean result = hasAccessForAction(mockClient,
          "test-ns", "example.com", "foos", "list");
        // Then
        assertThat(result).isEqualTo(allowed);
        assertThat(requestedSSAR.get())
          .hasFieldOrPropertyWithValue("spec.resourceAttributes.namespace", "test-ns")
          .hasFieldOrPropertyWithValue("spec.resourceAttributes.group", "example.com")
          .hasFieldOrPropertyWithValue("spec.resourceAttributes.resource", "foos")
          .hasFieldOrPropertyWithValue("spec.resourceAttributes.verb", "list");
    }

    @ParameterizedTest(name = "{index}: {0} returns {1}")
    @MethodSource("getKindTestCases")
    void getKind(KubernetesResource resource, String expectedKind) {
        assertThat(KubernetesHelper.getKind(resource)).isEqualTo(expectedKind);
    }

    static Stream<Arguments> getKindTestCases() {
        return Stream.of(
          Arguments.of(new Pod(), "Pod"),
          Arguments.of(new Template(), "Template"),
          Arguments.of(new KubernetesList(), "List"),
          Arguments.of(new RawExtension(), "RawExtension"),
          Arguments.of(null, null)
        );
    }

    @ParameterizedTest(name = "{index}: {0} returns {1}")
    @MethodSource("getCreationTimestampTestCases")
    void getCreationTimestamp(String timestamp, LocalDateTime expectedDate) {
        final ConfigMap cm = new ConfigMapBuilder().withNewMetadata().withCreationTimestamp(timestamp).endMetadata()
          .build();
        assertThat(KubernetesHelper.getCreationTimestamp(cm))
          .isEqualTo(expectedDate.atZone(ZoneOffset.UTC).toInstant());
    }

    static Stream<Arguments> getCreationTimestampTestCases() {
        return Stream.of(
          Arguments.of("1955-11-12T06:38:00Z", LocalDateTime.of(1955, 11, 12, 6, 38)),
          Arguments.of("1955-11-12T06:38:00+01", LocalDateTime.of(1955, 11, 12,  5, 38)),
          Arguments.of("1955-11-12T06:38:00.123456789Z", LocalDateTime.of(1955, 11, 12, 6, 38, 0, 123456789)),
          Arguments.of("1955-11-12T06:38:00.625Z", LocalDateTime.of(1955, 11, 12, 6, 38, 0, 625))
        );
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

    private List<String> getRemoteFragments(int port) {
        List<String> remoteStrList = new ArrayList<>();

        remoteStrList.add(String.format("http://localhost:%d/deployment.yaml", port));
        remoteStrList.add(String.format("http://localhost:%d/sa.yml", port));
        return remoteStrList;
    }
}
