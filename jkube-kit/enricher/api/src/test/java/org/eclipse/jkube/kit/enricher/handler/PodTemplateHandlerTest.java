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

import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.resource.GroupArtifactVersion;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.resource.VolumeConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

class PodTemplateHandlerTest {

    private ProbeHandler probeHandler;
    private JavaProject project;
    private PodTemplateHandler podTemplateHandler;
    private List<String> mounts;
    private List<VolumeConfig> volumes;
    private List<ImageConfiguration> images;

    @BeforeEach
    void setUp() {
        mounts = new ArrayList<>();
        volumes = new ArrayList<>();
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
    void getPodTemplate_withoutVolumeConfig_shouldGeneratePodTemplateWithoutVolume() {
        ResourceConfig config = ResourceConfig.builder()
                .imagePullPolicy("IfNotPresent")
                .controllerName("testing")
                .serviceAccount("test-account")
                .replicas(5)
                .build();

        PodTemplateSpec podTemplateSpec = podTemplateHandler.getPodTemplate(config, null, images);
        assertThat(podTemplateSpec.getSpec().getVolumes()).isEmpty();
        assertThat(podTemplateSpec.getSpec())
            .hasFieldOrPropertyWithValue("serviceAccountName", "test-account")
            .extracting(PodSpec::getContainers).isNotNull()
            .asList()
            .first()
            .hasFieldOrPropertyWithValue("name", "test-app")
            .hasFieldOrPropertyWithValue("image", "docker.io/test:latest")
            .hasFieldOrPropertyWithValue("imagePullPolicy", "IfNotPresent");
    }

    @Test
    void getPodTemplate_withEmptyVolumeAndWithoutServiceAccount_shouldGeneratePodTemplateWithNoVolume(){
        ResourceConfig config = ResourceConfig.builder()
                .imagePullPolicy("IfNotPresent")
                .controllerName("testing")
                .replicas(5)
                .volumes(volumes)
                .build();

        PodTemplateSpec podTemplateSpec = podTemplateHandler.getPodTemplate(config, null, images);
        assertThat(podTemplateSpec.getSpec())
            .hasFieldOrPropertyWithValue("serviceAccountName", null)
            .returns(true, spec -> spec.getVolumes().isEmpty())
            .extracting(PodSpec::getContainers)
            .isNotNull();
    }

    @Test
    void getPodTemplate_withVolumeAndServiceAccount_shouldGeneratePodTemplateWithConfiguredVolumeAndServiceAccount() {
      VolumeConfig volumeConfig = VolumeConfig.builder().name("test")
          .mounts(mounts).type("hostPath").path("/test/path").build();
      volumes.add(volumeConfig);

      ResourceConfig config = ResourceConfig.builder()
          .imagePullPolicy("IfNotPresent")
          .controllerName("testing")
          .serviceAccount("test-account")
          .replicas(5)
          .volumes(volumes)
          .build();

      PodTemplateSpec podTemplateSpec = podTemplateHandler.getPodTemplate(config, null, images);

      assertThat(podTemplateSpec.getSpec().getContainers()).isNotNull();
      assertThat(podTemplateSpec.getSpec())
          .hasFieldOrPropertyWithValue("serviceAccountName", "test-account")
          .extracting(PodSpec::getVolumes).asList()
          .isNotEmpty()
          .first()
          .hasFieldOrPropertyWithValue("name", "test")
          .hasFieldOrPropertyWithValue("hostPath.path", "/test/path");
    }

    @Test
    void getPodTemplate_withInvalidVolume_shouldGeneratePodTemplateWithNoVolume(){
        VolumeConfig volumeConfig1 = VolumeConfig.builder().name("test")
                .mounts(mounts).type("hoStPath").path("/test/path").build();
        volumes.add(volumeConfig1);

        ResourceConfig config = ResourceConfig.builder()
                .imagePullPolicy("IfNotPresent")
                .controllerName("testing")
                .serviceAccount("test-account")
                .replicas(5)
                .volumes(volumes)
                .build();

        PodTemplateSpec podTemplateSpec = podTemplateHandler.getPodTemplate(config, null, images);
        assertThat(podTemplateSpec.getSpec())
            .hasFieldOrPropertyWithValue("serviceAccountName", "test-account")
            .returns(true, s -> s.getVolumes().isEmpty())
            .extracting(PodSpec::getContainers)
            .isNotNull();
    }

    @Test
    void getPodTemplate_withoutEmptyVolume_shouldGeneratePodTemplateWithNoVolume(){
        VolumeConfig volumeConfig1 = VolumeConfig.builder().name("test").mounts(mounts).build();
        volumes.add(volumeConfig1);

        ResourceConfig config = ResourceConfig.builder()
                .imagePullPolicy("IfNotPresent")
                .controllerName("testing")
                .serviceAccount("test-account")
                .replicas(5)
                .volumes(volumes)
                .build();

        PodTemplateSpec podTemplateSpec = podTemplateHandler.getPodTemplate(config, null, images);
        assertThat(podTemplateSpec.getSpec())
            .hasFieldOrPropertyWithValue("serviceAccountName", "test-account")
            .returns(true, s -> s.getVolumes().isEmpty())
            .extracting(PodSpec::getContainers)
            .isNotNull();
    }

    @Test
    void getPodTemplate_withRestartPolicyAndResourceConfig_shouldGeneratePodTemplateWithConfiguredRestartPolicy() {
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