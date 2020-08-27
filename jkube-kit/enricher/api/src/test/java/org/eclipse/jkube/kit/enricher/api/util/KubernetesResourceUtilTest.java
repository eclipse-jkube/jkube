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

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.resource.GroupArtifactVersion;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceVersioning;
import mockit.Mocked;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author roland
 */
public class KubernetesResourceUtilTest {

    private static File jkubeDir;

    @Mocked
    JavaProject project;

    @BeforeClass
    public static void initPath() throws UnsupportedEncodingException {
        ClassLoader classLoader = KubernetesResourceUtil.class.getClassLoader();
        String filePath = URLDecoder.decode(Objects.requireNonNull(classLoader.getResource("jkube/simple-rc.yaml")).getFile(), "UTF-8");
        jkubeDir = new File(filePath).getParentFile();
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

