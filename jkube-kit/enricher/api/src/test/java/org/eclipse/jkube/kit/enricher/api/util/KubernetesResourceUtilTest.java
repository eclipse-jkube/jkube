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
package org.eclipse.jkube.kit.enricher.api.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil.FILENAME_TO_KIND_MAPPER;
import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil.KIND_TO_FILENAME_MAPPER;
import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil.getNameWithSuffix;
import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil.initializeKindFilenameMapper;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.resource.GroupArtifactVersion;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceVersioning;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicy;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author roland
 */
public class KubernetesResourceUtilTest {

    private static File jkubeDir;

    @BeforeClass
    public static void initPath() throws UnsupportedEncodingException {
        ClassLoader classLoader = KubernetesResourceUtil.class.getClassLoader();
        String filePath = URLDecoder.decode(Objects.requireNonNull(classLoader.getResource("jkube/simple-rc.yaml")).getFile(), "UTF-8");
        jkubeDir = new File(filePath).getParentFile();
    }

    @Before
    public void setUp() {
        FILENAME_TO_KIND_MAPPER.clear();
        KIND_TO_FILENAME_MAPPER.clear();
        initializeKindFilenameMapper();
    }

    @Test
    public void simple() throws IOException {
        for (String ext : new String[] { "yaml", "json" }) {
            HasMetadata ret = KubernetesResourceUtil.getResource(PlatformMode.kubernetes, KubernetesResourceUtil.DEFAULT_RESOURCE_VERSIONING, new File(jkubeDir, "simple-rc." + ext), "app");
            Assert.assertEquals(KubernetesResourceUtil.API_VERSION, ret.getApiVersion());
            assertEquals("ReplicationController", ret.getKind());
            assertEquals("simple", ret.getMetadata().getName());
        }
    }

    @Test
    public void withValue() throws IOException {
        HasMetadata ret = KubernetesResourceUtil.getResource(PlatformMode.kubernetes, KubernetesResourceUtil.DEFAULT_RESOURCE_VERSIONING, new File(jkubeDir, "named-svc.yaml"), "app");
        Assert.assertEquals(KubernetesResourceUtil.API_VERSION, ret.getApiVersion());
        assertEquals("Service", ret.getKind());
        assertEquals("pong", ret.getMetadata().getName());
    }

    @Test
    public void invalidType() throws IOException {
        try {
            KubernetesResourceUtil.getResource(PlatformMode.kubernetes, KubernetesResourceUtil.DEFAULT_RESOURCE_VERSIONING, new File(jkubeDir, "simple-bla.yaml"), "app");
            fail();
        } catch (IllegalArgumentException exp) {
            assertTrue(exp.getMessage().contains("bla"));
            assertTrue(exp.getMessage().contains("svc"));
        }
    }

    @Test
    public void containsKind() throws Exception {
        HasMetadata ret = KubernetesResourceUtil.getResource(PlatformMode.kubernetes, KubernetesResourceUtil.DEFAULT_RESOURCE_VERSIONING, new File(jkubeDir, "contains_kind.yml"), "app");
        assertEquals("ReplicationController", ret.getKind());
    }


    @Test
    public void job() throws Exception {
        HasMetadata ret = KubernetesResourceUtil.getResource(PlatformMode.kubernetes, KubernetesResourceUtil.DEFAULT_RESOURCE_VERSIONING, new File(jkubeDir, "job.yml"), "app");
        assertEquals("Job", ret.getKind());
        assertEquals(KubernetesResourceUtil.JOB_VERSION, ret.getApiVersion());
    }

    @Test
    public void containsNoKindAndNoTypeInFilename() throws Exception {
        try {
            KubernetesResourceUtil.getResource(PlatformMode.kubernetes, KubernetesResourceUtil.DEFAULT_RESOURCE_VERSIONING, new File(jkubeDir, "contains_no_kind.yml"), "app");
            fail();
        } catch (IllegalArgumentException exp) {
            assertTrue(exp.getMessage().contains("type"));
            assertTrue(exp.getMessage().toLowerCase().contains("kind"));
        }


    }

    @Test
    public void invalidPattern() throws IOException {
        try {
            KubernetesResourceUtil.getResource(PlatformMode.kubernetes, KubernetesResourceUtil.DEFAULT_RESOURCE_VERSIONING, new File(jkubeDir, "blubber.yaml"), "app");
            fail();
        } catch (FileNotFoundException exp) {
            assertTrue(exp.getMessage().contains("blubber"));
        }
    }

    @Test
    public void noNameInFile() throws IOException {
        HasMetadata ret = KubernetesResourceUtil.getResource(PlatformMode.kubernetes, KubernetesResourceUtil.DEFAULT_RESOURCE_VERSIONING, new File(jkubeDir, "rc.yml"), "app");
        assertEquals("flipper",ret.getMetadata().getName());
    }

    @Test
    public void noNameInFileAndNotInMetadata() throws IOException {
        HasMetadata ret = KubernetesResourceUtil.getResource(PlatformMode.kubernetes, KubernetesResourceUtil.DEFAULT_RESOURCE_VERSIONING, new File(jkubeDir, "svc.yml"), "app");
        assertEquals("Service",ret.getKind());
        assertEquals("app", ret.getMetadata().getName());
    }

    @Test
    public void invalidExtension() throws IOException {
        try {
            KubernetesResourceUtil.getResource(PlatformMode.kubernetes, KubernetesResourceUtil.DEFAULT_RESOURCE_VERSIONING, new File(jkubeDir, "simple-rc.txt"), "app");
            fail();
        } catch (IllegalArgumentException exp) {
            assertTrue(exp.getMessage().contains("txt"));
            assertTrue(exp.getMessage().contains("json"));
            assertTrue(exp.getMessage().contains("yml"));
        }
    }

    @Test
    public void containerName() {

        ImageConfiguration imageConfiguration = ImageConfiguration.builder()
                .name("dummy-image")
                .registry("example.com/someregistry")
                .name("test")
                .build();
        String containerName = KubernetesResourceUtil.extractContainerName(new GroupArtifactVersion("io.fabric8-test-", "fabric8-maven-plugin-dummy", "0"), imageConfiguration);
        assertTrue(containerName.matches(KubernetesResourceUtil.CONTAINER_NAME_REGEX));
    }

    @Test
    public void readWholeDir() throws IOException {
        ResourceVersioning v = new ResourceVersioning()
                .withCoreVersion("v2")
                .withExtensionsVersion("extensions/v2");

        KubernetesListBuilder builder =
            KubernetesResourceUtil.readResourceFragmentsFrom(PlatformMode.kubernetes, v, "pong", new File(jkubeDir, "read-dir").listFiles());
        KubernetesList list = builder.build();
        assertEquals(2,list.getItems().size());
        for (HasMetadata item : list.getItems() ) {
            assertTrue("Service".equals(item.getKind()) || "ReplicationController".equals(item.getKind()));
            assertEquals("pong",item.getMetadata().getName());
            assertEquals("v2",item.getApiVersion());
        }
    }

    @Test
    public void testMergePodSpecWithFragmentWhenFragmentHasContainerNameWithSidecarDisabled() {
        // Given
        PodSpecBuilder fragment = new PodSpecBuilder()
                .addNewContainer()
                .withArgs("/usr/local/s2i/run")
                .withName("demo")
                .addNewEnv()
                .withName("JAVA_APP_DIR")
                .withValue("/deployments/ROOT.war ")
                .endEnv()
                .endContainer();
        PodSpec defaultPodSpec = getDefaultGeneratedPodSpec();

        // When
        String defaultContainerName = KubernetesResourceUtil.mergePodSpec(fragment, defaultPodSpec, "default-name", false);

        // Then
        assertNotNull(defaultContainerName);
        assertEquals("demo", defaultContainerName);
    }

    @Test
    public void testMergePodSpecWithFragmentWhenFragmentNullContainerNameWithSidecarDisabled() {
        // Given
        Map<String, Quantity> requests = new HashMap<>();
        requests.put("cpu", new Quantity("0.2"));
        requests.put("memory", new Quantity("256Mi"));

        Map<String, Quantity> limits = new HashMap<>();
        limits.put("cpu", new Quantity("1.0"));
        limits.put("memory", new Quantity("256Mi"));

        PodSpecBuilder fragment = new PodSpecBuilder()
                .addNewContainer()
                .withNewResources()
                .withRequests(requests)
                .withLimits(limits)
                .endResources()
                .addNewEnv()
                .withName("SPRING_APPLICATION_JSON")
                .withValue("{\"server\":{\"undertow\":{\"io-threads\":1, \"worker-threads\":2 }}}")
                .endEnv()
                .endContainer();
        PodSpec defaultPodSpec = getDefaultGeneratedPodSpec();

        // When
        String defaultContainerName = KubernetesResourceUtil.mergePodSpec(fragment, defaultPodSpec, "default-name", false);

        // Then
        assertNotNull(defaultContainerName);
        assertEquals("spring-boot", defaultContainerName);
    }

    @Test
    public void testMergePodSpecDefaultContainerNameWhenWhenFragmentNullSidecarEnabled() {
        // Given
        PodSpecBuilder fragment = new PodSpecBuilder()
                .addNewContainer()
                .withName("sidecar1")
                .withImage("busybox")
                .endContainer()
                .addNewContainer()
                .withName("sidecar2")
                .withImage("busybox")
                .endContainer();
        PodSpec defaultPodSpec = getDefaultGeneratedPodSpec();

        // When
        String defaultContainerName = KubernetesResourceUtil.mergePodSpec(fragment, defaultPodSpec, "default-name", true);

        // Then
        assertNotNull(defaultContainerName);
        assertEquals("spring-boot", defaultContainerName);
    }

    @Test
    public void testGetResourceShouldLoadNetworkV1Ingress() throws IOException {
        // Given
        File resourceFragment = new File(jkubeDir, "network-v1-ingress.yml");

        // When
        HasMetadata result = KubernetesResourceUtil.getResource(PlatformMode.kubernetes, KubernetesResourceUtil.DEFAULT_RESOURCE_VERSIONING, resourceFragment, "app");

        // Then
        assertNotNull(result);
        assertEquals("networking.k8s.io/v1", result.getApiVersion());
        assertEquals("Ingress", result.getKind());
        assertEquals("my-ingress", result.getMetadata().getName());
    }

    @Test
    public void getResourceWithNetworkPolicyShouldLoadV1NetworkPolicy() throws Exception {
      // Given
      final File resource = new File(jkubeDir, "networking-v1-np.yml");
      // When
      final HasMetadata result = KubernetesResourceUtil.getResource(PlatformMode.kubernetes,
          KubernetesResourceUtil.DEFAULT_RESOURCE_VERSIONING, resource, "app");
      // Then
      assertThat(result)
          .isInstanceOf(NetworkPolicy.class)
      .hasFieldOrPropertyWithValue("kind", "NetworkPolicy")
      .hasFieldOrPropertyWithValue("spec.podSelector.matchLabels.role", "db");
    }

    @Test
    public void testIsExposedService() {
        assertTrue(KubernetesResourceUtil.isExposedService(new ObjectMetaBuilder().addToLabels("expose", "true").build()));
        assertTrue(KubernetesResourceUtil.isExposedService(new ObjectMetaBuilder().addToLabels("jkube.io/exposeUrl", "true").build()));
    }

    @Test
    public void getNameWithSuffix_withKnownMapping_shouldReturnKnownMapping() {
        assertEquals("name-pod", getNameWithSuffix("name", "Pod"));
    }

    @Test
    public void getNameWithSuffix_withUnknownMapping_shouldReturnCR() {
        assertEquals("name-cr", getNameWithSuffix("name", "VeryCustomKind"));
    }

    @Test
    public void updateKindFilenameMappings_whenAddsCronTabMapping_updatesKindToFileNameMapper() {
        // Given
        Map<String, List<String>> mappings = new HashMap<>();
        mappings.put("CronTab", Collections.singletonList("cr"));

        // When
        KubernetesResourceUtil.updateKindFilenameMapper(mappings);

        // Then
        assertThat(KIND_TO_FILENAME_MAPPER).containsKey("CronTab");
        assertThat(FILENAME_TO_KIND_MAPPER).containsKey("cr");
    }

    private PodSpec getDefaultGeneratedPodSpec() {
        return new PodSpecBuilder()
                .addNewContainer()
                .withName("spring-boot")
                .withImage("spring-boot-test:latest")
                .addNewEnv()
                .withName("KUBERNETES_NAMESPACE")
                .withNewValueFrom()
                .withNewFieldRef()
                .withFieldPath("metadata.namespace")
                .endFieldRef()
                .endValueFrom()
                .endEnv()
                .withImagePullPolicy("IfNotPresent")
                .addNewPort().withContainerPort(8080).withProtocol("TCP").endPort()
                .addNewPort().withContainerPort(9779).withProtocol("TCP").endPort()
                .addNewPort().withContainerPort(8778).withProtocol("TCP").endPort()
                .endContainer()
                .build();
    }
}

