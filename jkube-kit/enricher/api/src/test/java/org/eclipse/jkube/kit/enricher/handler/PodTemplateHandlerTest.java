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
package org.eclipse.jkube.kit.enricher.handler;

import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.resource.GroupArtifactVersion;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.resource.VolumeConfig;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

public class PodTemplateHandlerTest {

    private ProbeHandler probeHandler;
    private JavaProject project;
    private PodTemplateHandler podTemplateHandler;
    private List<String> mounts;
    private List<VolumeConfig> volumes1;
    private List<ImageConfiguration> images;

    @Before
    public void before() {
        mounts = new ArrayList<>();
        volumes1 = new ArrayList<>();
        images = new ArrayList<>();
        List<String> ports = new ArrayList<>();
        List<String> tags = new ArrayList<>();

        //volume config with name and multiple mount
        mounts.add("/path/system");
        mounts.add("/path/sys");

        ports.add("8080");
        ports.add("9090");

        tags.add("latest");
        tags.add("test");

        //container name with alias
        final BuildConfiguration buildImageConfiguration = BuildConfiguration.builder()
                .ports(ports).from("fabric8/maven:latest").cleanup("try")
                .tags(tags).compressionString("gzip").build();

        ImageConfiguration imageConfiguration = ImageConfiguration.builder()
                .name("test").alias("test-app").build(buildImageConfiguration)
                .registry("docker.io").build();

        images.add(imageConfiguration);
        probeHandler = mock(ProbeHandler.class, RETURNS_DEEP_STUBS);
        project = mock(JavaProject.class, RETURNS_DEEP_STUBS);
        ContainerHandler containerHandler = getContainerHandler();
        podTemplateHandler = new PodTemplateHandler(containerHandler);
    }

    @Test
    public void podWithoutVolumeTemplateHandlerTest() {
        //Pod without Volume Config
        ResourceConfig config = ResourceConfig.builder()
                .imagePullPolicy("IfNotPresent")
                .controllerName("testing")
                .serviceAccount("test-account")
                .replicas(5)
                .build();

        PodTemplateSpec podTemplateSpec = podTemplateHandler.getPodTemplate(config, null, images);

        //Assertion
        assertEquals("test-account", podTemplateSpec.getSpec().getServiceAccountName());
        assertTrue(podTemplateSpec.getSpec().getVolumes().isEmpty());
        assertNotNull(podTemplateSpec.getSpec().getContainers());
        assertEquals("test-app", podTemplateSpec.getSpec()
                .getContainers().get(0).getName());
        assertEquals("docker.io/test:latest", podTemplateSpec.getSpec()
                .getContainers().get(0).getImage());
        assertEquals("IfNotPresent", podTemplateSpec.getSpec()
                .getContainers().get(0).getImagePullPolicy());
    }

    @Test
    public void podWithEmotyVolumeTemplateHandlerTest(){
        //Pod with empty Volume Config and wihtout ServiceAccount
        ResourceConfig config = ResourceConfig.builder()
                .imagePullPolicy("IfNotPresent")
                .controllerName("testing")
                .replicas(5)
                .volumes(volumes1)
                .build();

        PodTemplateSpec podTemplateSpec = podTemplateHandler.getPodTemplate(config, null, images);

        //Assertion
        assertNull(podTemplateSpec.getSpec().getServiceAccountName());
        assertTrue(podTemplateSpec.getSpec().getVolumes().isEmpty());
        assertNotNull(podTemplateSpec.getSpec().getContainers());
    }

    @Test
    public void podWithVolumeTemplateHandlerTest(){
        //Config with Volume Config and ServiceAccount
        //valid type
        VolumeConfig volumeConfig1 = VolumeConfig.builder().name("test")
                .mounts(mounts).type("hostPath").path("/test/path").build();
        volumes1.clear();
        volumes1.add(volumeConfig1);

        ResourceConfig config = ResourceConfig.builder()
                .imagePullPolicy("IfNotPresent")
                .controllerName("testing")
                .serviceAccount("test-account")
                .replicas(5)
                .volumes(volumes1)
                .build();

        PodTemplateSpec podTemplateSpec = podTemplateHandler.getPodTemplate(config, null, images);

        //Assertion
        assertEquals("test-account",podTemplateSpec.getSpec().getServiceAccountName());
        assertFalse(podTemplateSpec.getSpec().getVolumes().isEmpty());
        assertEquals("test",podTemplateSpec.getSpec()
                .getVolumes().get(0).getName());
        assertEquals("/test/path",podTemplateSpec.getSpec()
                .getVolumes().get(0).getHostPath().getPath());
        assertNotNull(podTemplateSpec.getSpec().getContainers());
    }

    @Test
    public void podWithInvalidVolumeTypeTemplateHandlerTest(){
        //invalid type
        VolumeConfig volumeConfig1 = VolumeConfig.builder().name("test")
                .mounts(mounts).type("hoStPath").path("/test/path").build();
        volumes1.clear();
        volumes1.add(volumeConfig1);

        ResourceConfig config = ResourceConfig.builder()
                .imagePullPolicy("IfNotPresent")
                .controllerName("testing")
                .serviceAccount("test-account")
                .replicas(5)
                .volumes(volumes1)
                .build();

        PodTemplateSpec podTemplateSpec = podTemplateHandler.getPodTemplate(config, null, images);

        //Assertion
        assertEquals("test-account",podTemplateSpec.getSpec().getServiceAccountName());
        assertTrue(podTemplateSpec.getSpec().getVolumes().isEmpty());
        assertNotNull(podTemplateSpec.getSpec().getContainers());
    }

    @Test
    public void podWithoutEmptyTypeTemplateHandlerTest(){
        //empty type
        VolumeConfig volumeConfig1 = VolumeConfig.builder().name("test").mounts(mounts).build();
        volumes1.clear();
        volumes1.add(volumeConfig1);

        ResourceConfig config = ResourceConfig.builder()
                .imagePullPolicy("IfNotPresent")
                .controllerName("testing")
                .serviceAccount("test-account")
                .replicas(5)
                .volumes(volumes1)
                .build();

        PodTemplateSpec podTemplateSpec = podTemplateHandler.getPodTemplate(config, null, images);

        //Assertion
        assertEquals("test-account",podTemplateSpec.getSpec().getServiceAccountName());
        assertTrue(podTemplateSpec.getSpec().getVolumes().isEmpty());
        assertNotNull(podTemplateSpec.getSpec().getContainers());
    }

    @Test
    public void getPodTemplate_withRestartPolicyAndResourceConfig_shouldGeneratePodTemplateWithConfiguredRestartPolicy() {
        // Given
        ResourceConfig config = ResourceConfig.builder().build();

        // When
        PodTemplateSpec podTemplateSpec = podTemplateHandler.getPodTemplate(config, "Always", images);

        // Then
        assertThat(podTemplateSpec)
            .hasFieldOrPropertyWithValue("spec.restartPolicy", "Always");
    }

    private ContainerHandler getContainerHandler() {
        return new ContainerHandler(project.getProperties(), new GroupArtifactVersion("g","a","v"), probeHandler);
    }
}