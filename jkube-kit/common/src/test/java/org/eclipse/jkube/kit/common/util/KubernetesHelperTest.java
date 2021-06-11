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

import io.fabric8.kubernetes.api.model.HTTPHeader;
import org.assertj.core.api.AssertionsForClassTypes;
import org.eclipse.jkube.kit.common.GenericCustomResource;
import org.eclipse.jkube.kit.common.KitLogger;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1beta1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.v1beta1.CustomResourceDefinitionBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1beta1.CustomResourceDefinitionList;
import io.fabric8.kubernetes.api.model.apiextensions.v1beta1.CustomResourceDefinitionListBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1beta1.CustomResourceDefinitionNamesBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1beta1.CustomResourceDefinitionSpecBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1beta1.CustomResourceDefinitionVersionBuilder;
import io.fabric8.kubernetes.api.model.apps.DaemonSetBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.fabric8.openshift.api.model.Template;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class KubernetesHelperTest {

    @Mocked
    private KitLogger logger;

    @Test
    public void testListResourceFragments() {
        // Given
        File localResourceDir = new File(getClass().getResource("/util/fragments").getPath());

        // When & Then
        assertLocalFragments(KubernetesHelper.listResourceFragments(localResourceDir, null, logger), 2);
    }

    @Test
    public void testResourceFragmentsWithRemotes() {
        // Given
        List<String> remoteStrList = getRemoteFragments();
        File localResourceDir = new File(getClass().getResource("/util/fragments").getPath());

        // When
        File[] fragments = KubernetesHelper.listResourceFragments(localResourceDir, remoteStrList, logger);

        // Then
        assertLocalFragments(fragments, 4);
        assertTrue(Arrays.stream(fragments).anyMatch( f -> f.getName().equals("deployment.yaml")));
        assertTrue(Arrays.stream(fragments).anyMatch( f -> f.getName().equals("sa.yml")));
    }

    @Test
    public void testGetResourceFragmentFromSourceWithSomeResourceDirAndNullRemotes() {
        // Given
        File localResourceDir = new File(getClass().getResource("/util/fragments").getPath());

        // When
        File fragmentFile = KubernetesHelper.getResourceFragmentFromSource(localResourceDir, Collections.emptyList(), "service.yml", logger);

        // Then
        assertNotNull(fragmentFile);
        assertTrue(fragmentFile.exists());
        assertEquals("service.yml", fragmentFile.getName());
    }

    @Test
    public void testGetResourceFragmentWithNullResourceDirAndNullRemotes() {
        assertNull(KubernetesHelper.getResourceFragmentFromSource(null, null, "service.yml", logger));
    }

    @Test
    public void testGetResourceFragmentFromSourceWithNullResourceDirAndSomeRemotes() {
        // Given
        List<String> remotes = getRemoteFragments();

        // When
        File fragmentFile = KubernetesHelper.getResourceFragmentFromSource(null, remotes, "deployment.yaml", logger);

        // Then
        assertNotNull(fragmentFile);
        assertTrue(fragmentFile.exists());
        assertEquals("deployment.yaml", fragmentFile.getName());
    }

    @Test
    public void testGetResourceFragmentFromSourceWithSomeResourceDirAndSomeRemotes() {
        // Given
        File localResourceDir = new File(getClass().getResource("/util/fragments").getPath());
        List<String> remotes = getRemoteFragments();

        // When
        File fragmentFile = KubernetesHelper.getResourceFragmentFromSource(localResourceDir, remotes, "sa.yml", logger);

        // Then
        assertNotNull(fragmentFile);
        assertTrue(fragmentFile.exists());
        assertEquals("sa.yml", fragmentFile.getName());
    }

    @Test
    public void testGetQuantityFromString() {
        // Given
        Map<String, String> limitsAsStr = new HashMap<>();
        limitsAsStr.put("cpu", "200m");
        limitsAsStr.put("memory", "1Gi");

        // When
        Map<String, Quantity> limitAsQuantity = KubernetesHelper.getQuantityFromString(limitsAsStr);

        // Then
        assertNotNull(limitAsQuantity);
        assertEquals(2, limitAsQuantity.size());
        assertEquals(new Quantity("200m"), limitAsQuantity.get("cpu"));
        assertEquals(new Quantity("1Gi"), limitAsQuantity.get("memory"));
    }

    @Test
    public void testGetEnvVar() {
        // Given
        List<EnvVar> envVarList = prepareEnvVarList();

        // When
        String value1 = KubernetesHelper.getEnvVar(envVarList, "env1", "defaultValue");
        String value2 = KubernetesHelper.getEnvVar(envVarList, "JAVA_OPTIONS", "defaultValue");
        String value3 = KubernetesHelper.getEnvVar(envVarList, "FOO", "defaultValue");
        String value4 = KubernetesHelper.getEnvVar(envVarList, "UNKNOWN", "defaultValue");

        // Then
        assertEquals("value1", value1);
        assertEquals("-Dfoo=bar -Dxyz=abc", value2);
        assertEquals("BAR", value3);
        assertEquals("defaultValue", value4);
    }

    @Test
    public void testSetEnvVar() {
        // Given
        List<EnvVar> envVarList = prepareEnvVarList();

        // When
        boolean statusCode1 = KubernetesHelper.setEnvVar(envVarList, "FOO", "NEW_BAR");
        boolean statusCode2 = KubernetesHelper.setEnvVar(envVarList, "UNKNOWN_KEY", "UNKNOWN_VALUE");

        // Then
        assertTrue(statusCode1);
        assertEquals("NEW_BAR", KubernetesHelper.getEnvVar(envVarList, "FOO", "defaultValue"));
        assertTrue(statusCode2);
        assertEquals("UNKNOWN_VALUE", KubernetesHelper.getEnvVar(envVarList, "UNKNOWN_KEY", "defaultValue"));
    }

    @Test
    public void testRemoveEnvVar() {
        // Given
        List<EnvVar> envVarList = prepareEnvVarList();

        // When
        boolean statusCode1 = KubernetesHelper.removeEnvVar(envVarList, "FOO");

        // Then
        assertTrue(statusCode1);
        assertEquals("defaultValue", KubernetesHelper.getEnvVar(envVarList, "FOO", "defaultValue"));
    }

    @Test
    public void testIsExposedServiceReturnsTrue() {
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
        assertTrue(result);
    }

    @Test
    public void testIsExposedServiceReturnsFalse() {
        // Given
        Service service = new ServiceBuilder().withNewMetadata().withName("svc1").endMetadata().build();

        // When
        boolean result = KubernetesHelper.isExposeService(service);

        // Then
        assertFalse(result);
    }

    @Test
    public void testGetAnnotationValue() {
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
        assertEquals("true", result1);
        assertEquals("http://12.4.1.4:8223/test", result2);
        assertNull(result3);
        assertNull(result4);
    }

    @Test
    public void testConvertToEnvVarList() {
        // Given
        Map<String, String> envVarAsStringMap = new HashMap<>();
        envVarAsStringMap.put("env1", "value1");
        envVarAsStringMap.put("JAVA_OPTIONS", "-Dfoo=bar -Dxyz=abc");
        envVarAsStringMap.put("FOO", "BAR");

        // When
        List<EnvVar> envVarList = KubernetesHelper.convertToEnvVarList(envVarAsStringMap);

        // Then
        assertNotNull(envVarList);
        assertEquals(3, envVarList.size());
        assertEquals("value1", KubernetesHelper.getEnvVar(envVarList, "env1", "defaultValue"));
        assertEquals("-Dfoo=bar -Dxyz=abc", KubernetesHelper.getEnvVar(envVarList, "JAVA_OPTIONS", "defaultValue"));
        assertEquals("BAR", KubernetesHelper.getEnvVar(envVarList, "FOO", "defaultValue"));

    }

    @Test
    public void testGetServiceExposeUrlReturnsUrlFromAnnotation(@Mocked KubernetesClient kubernetesClient, @Mocked Resource<Service> svcResource) throws InterruptedException {
        // Given
        Service svc = new ServiceBuilder().withNewMetadata().withName("svc1").endMetadata().build();
        Set<HasMetadata> entities = new HashSet<>();
        entities.add(svc);
        new Expectations() {{
            kubernetesClient.services().inNamespace(anyString).withName("svc1");
            result = svcResource;
            svcResource.get();
            result = new ServiceBuilder()
                    .withNewMetadata()
                    .withName("svc1")
                    .addToAnnotations("exposeUrl", "http://example.com")
                    .endMetadata()
                    .build();
        }};

        // When
        String result = KubernetesHelper.getServiceExposeUrl(kubernetesClient, entities, 3, "exposeUrl");

        // Then
        assertEquals("http://example.com", result);
        new Verifications() {{
            kubernetesClient.services().inNamespace(anyString).withName("svc1");
            times = 1;
            svcResource.get();
            times = 1;
        }};
    }

    @Test
    public void testGetServiceExposeUrlReturnsNull(@Mocked KubernetesClient kubernetesClient, @Mocked Resource<Service> svcResource) throws InterruptedException {
        // Given
        Service svc = new ServiceBuilder().withNewMetadata().withName("svc1").endMetadata().build();
        Set<HasMetadata> entities = new HashSet<>();
        entities.add(svc);
        new Expectations() {{
            kubernetesClient.services().inNamespace(anyString).withName("svc1");
            result = svcResource;
            svcResource.get();
            result = new ServiceBuilder()
                    .withNewMetadata()
                    .withName("svc1")
                    .endMetadata()
                    .build();
        }};

        // When
        String result = KubernetesHelper.getServiceExposeUrl(kubernetesClient, entities, 1, "exposeUrl");

        // Then
        assertNull(result);
        new Verifications() {{
            kubernetesClient.services().inNamespace(anyString).withName("svc1");
            times = 1;
            svcResource.get();
            times = 1;
        }};
    }

    @Test
    public void testGetFullyQualifiedApiGroupWithKind() {
        // Given
        CustomResourceDefinitionContext crd1 = new CustomResourceDefinitionContext.Builder()
                .withGroup("networking.istio.io")
                .withVersion("v1alpha3")
                .withKind("VirtualService")
                .build();
        CustomResourceDefinitionContext crd2 = new CustomResourceDefinitionContext.Builder()
                .withGroup("networking.istio.io")
                .withVersion("v1alpha3")
                .withKind("Gateway")
                .build();

        // When
        String result1 = KubernetesHelper.getFullyQualifiedApiGroupWithKind(crd1);
        String result2 = KubernetesHelper.getFullyQualifiedApiGroupWithKind(crd2);

        // Then
        assertEquals("networking.istio.io/v1alpha3#VirtualService", result1);
        assertEquals("networking.istio.io/v1alpha3#Gateway", result2);
    }

    @Test
    public void testContainsPort() {
        // Given
        List<ContainerPort> ports = new ArrayList<>();
        ports.add(new ContainerPortBuilder().withName("p1").withContainerPort(8001).build());
        ports.add(new ContainerPortBuilder().withName("p2").withContainerPort(8002).build());

        // When
        boolean result1 = KubernetesHelper.containsPort(ports, "8001");
        boolean result2 = KubernetesHelper.containsPort(ports, "8002");

        // Then
        assertTrue(result1);
        assertTrue(result2);
    }

    @Test
    public void testAddPort() {
        // When
        ContainerPort result = KubernetesHelper.addPort("8001", "p1", logger);

        // Then
        assertNotNull(result);
        assertEquals("p1", result.getName());
        assertEquals(8001, result.getContainerPort().intValue());
    }

    @Test
    public void testAddPortNullPortNumber() {
        assertNull(KubernetesHelper.addPort("", "", logger));
    }

    @Test
    public void testAddPortWithInvalidPortNumber() {
        assertNull(KubernetesHelper.addPort("90invalid", "", logger));
    }

    @Test
    public void testIsControllerResource() {
        assertTrue(KubernetesHelper.isControllerResource(new DeploymentBuilder().build()));
        assertTrue(KubernetesHelper.isControllerResource(new StatefulSetBuilder().build()));
        assertTrue(KubernetesHelper.isControllerResource(new ReplicationControllerBuilder().build()));
        assertTrue(KubernetesHelper.isControllerResource(new ReplicaSetBuilder().build()));
        assertTrue(KubernetesHelper.isControllerResource(new DeploymentConfigBuilder().build()));
        assertTrue(KubernetesHelper.isControllerResource(new DaemonSetBuilder().build()));
        assertFalse(KubernetesHelper.isControllerResource(new ConfigMapBuilder().build()));
    }

    @Test
    public void loadResourcesWithNestedTemplateAndDuplicateResources() throws IOException {
        // Given
        final File manifest = new File(KubernetesHelperTest.class.getResource(
            "/util/kubernetes-helper/list-with-duplicates-and-template.yml").getFile());
        // When
        final List<HasMetadata> result = KubernetesHelper.loadResources(manifest);
        // Then
        assertThat(result)
            .hasSize(3)
            .hasOnlyElementsOfTypes(Namespace.class, GenericCustomResource.class, Template.class)
            .extracting("metadata.name")
            .containsExactly("should-be-first", "custom-resource", "template-example");
    }

    @Test
    public void loadResourcesWithDuplicateAndSameNameCustomResources() throws IOException {
        // Given
        final File manifest = new File(KubernetesHelperTest.class.getResource(
            "/util/kubernetes-helper/list-with-duplicates-and-same-name-custom-resource.yml").getFile());
        // When
        final List<HasMetadata> result = KubernetesHelper.loadResources(manifest);
        // Then
        assertThat(result)
            .hasSize(3)
            .hasOnlyElementsOfTypes(Namespace.class, GenericCustomResource.class)
            .extracting("metadata.name")
            .containsExactly("should-be-first", "custom-resource", "custom-resource");
    }

    @Test
    public void testGetCrdContextReturnsValidCrdContext() {
        // Given
        final CustomResourceDefinitionList crdList = crdList();
        GenericCustomResource genericCustomResource = new GenericCustomResource();
        genericCustomResource.setApiVersion("jkube.eclipse.org/v1alpha1");
        genericCustomResource.setKind("JKubeCustomResource");
        // When
        CustomResourceDefinitionContext result = KubernetesHelper.getCrdContext(crdList, genericCustomResource);
        // Then
        assertThat(result)
            .hasFieldOrPropertyWithValue("group", "jkube.eclipse.org")
            .hasFieldOrPropertyWithValue("kind", "JKubeCustomResource")
            .hasFieldOrPropertyWithValue("version", "v1alpha1")
            .hasFieldOrPropertyWithValue("scope", "Namespaced")
            .hasFieldOrPropertyWithValue("plural", "jkubecustomresources");
    }

    @Test
    public void testGetCrdContextWithVersionInListReturnsValidCrdContext() {
        // Given
        final CustomResourceDefinitionList crdList = crdList();
        GenericCustomResource genericCustomResource = new GenericCustomResource();
        genericCustomResource.setApiVersion("jkube.eclipse.org/v1");
        genericCustomResource.setKind("JKubeCustomResource");
        // When
        CustomResourceDefinitionContext result = KubernetesHelper.getCrdContext(crdList, genericCustomResource);
        // Then
        assertThat(result)
            .hasFieldOrPropertyWithValue("group", "jkube.eclipse.org")
            .hasFieldOrPropertyWithValue("kind", "JKubeCustomResource")
            .hasFieldOrPropertyWithValue("version", "v1")
            .hasFieldOrPropertyWithValue("scope", "Namespaced")
            .hasFieldOrPropertyWithValue("plural", "jkubecustomresourcesinlist");
    }

    @Test
    public void testGetCrdContextReturnsNullCrdContext() {
        // Given
        final CustomResourceDefinitionList crdList = crdList();
        GenericCustomResource genericCustomResource = new GenericCustomResource();
        genericCustomResource.setApiVersion("jkube.eclipse.org/v1alpha1");
        genericCustomResource.setKind("Unknown");
        // When
        CustomResourceDefinitionContext crdContext = KubernetesHelper.getCrdContext(crdList, genericCustomResource);
        // Then
        assertThat(crdContext).isNull();
    }

    @Test
    public void testConvertMapToHTTPHeaderList() {
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

    private void assertLocalFragments(File[] fragments, int expectedSize) {
        assertEquals(expectedSize, fragments.length);
        assertTrue(Arrays.stream(fragments).anyMatch( f -> f.getName().equals("deployment.yml")));
        assertTrue(Arrays.stream(fragments).anyMatch( f -> f.getName().equals("service.yml")));
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

    private static CustomResourceDefinitionList crdList() {
        return new CustomResourceDefinitionListBuilder().addToItems(
            crd("jkubecustomresources", "jkube.eclipse.org", "v1beta1", "JKubeCustomResource"),
            crd("jkubecustomresources", "jkube.eclipse.org", "v1alpha1", "JKubeCustomResource"),
            crd("jkubecustomresources", "jkube.eclipse.org", "v2", "JKubeCustomResource"),
            crdVL("jkubecustomresourcesinlist", "jkube.eclipse.org", "v1", "JKubeCustomResource"),
            crd("jkubepods", "jkube.eclipse.org", "v2", "JKubePods"),
            crd("jkubepods", "jkube.eclipse.org", "v1alpha1", "JKubePods"),
            crdVL("jkubepods", "jkube.eclipse.org", "v1", "JKubePods"),
            crdVL("odds", "other.eclipse.org", "v1", "Odd")
        ).build();
    }

    private static CustomResourceDefinition crd(String plural, String group, String version, String kind) {
        return new CustomResourceDefinitionBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .withName(plural + group)
                .build())
            .withSpec(new CustomResourceDefinitionSpecBuilder()
                .withGroup(group)
                .withVersion(version)
                .withScope("Namespaced")
                .withNames(new CustomResourceDefinitionNamesBuilder()
                    .withKind(kind)
                    .withPlural(plural)
                    .build())
            .build())
        .build();
    }

    private static CustomResourceDefinition crdVL(String plural, String group, String version, String kind) {
        return new CustomResourceDefinitionBuilder(crd(plural, group, version, kind))
            .editSpec()
            .withVersion(null)
            .withVersions(new CustomResourceDefinitionVersionBuilder()
                .withName(version)
                .build())
            .endSpec()
            .build();
    }

}
