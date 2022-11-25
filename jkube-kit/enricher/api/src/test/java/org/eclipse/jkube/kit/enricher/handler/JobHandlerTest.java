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

import java.util.ArrayList;
import java.util.List;

import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.batch.v1.JobSpec;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.resource.GroupArtifactVersion;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.resource.VolumeConfig;

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

class JobHandlerTest {
    private ProbeHandler probeHandler;
    private JavaProject project;
    private List<VolumeConfig> volumes;
    private List<ImageConfiguration> images;
    private JobHandler jobHandler;

    @BeforeEach
    void setUp(){
        probeHandler = mock(ProbeHandler.class);
        project = mock(JavaProject.class);
        volumes = new ArrayList<>();
        images = new ArrayList<>();
        List<String> mounts = new ArrayList<>();
        List<String> ports = new ArrayList<>();
        List<String> tags = new ArrayList<>();

        //volume config with name and multiple mount
        mounts.add("/path/system");
        mounts.add("/path/sys");

        ports.add("8080");
        ports.add("9090");

        tags.add("latest");
        tags.add("test");

        VolumeConfig volumeConfig = VolumeConfig.builder()
                .name("test").mounts(mounts).type("hostPath").path("/test/path").build();
        volumes.add(volumeConfig);

        //container name with alias
        final BuildConfiguration buildImageConfiguration = BuildConfiguration.builder()
                .ports(ports).from("fabric8/maven:latest").cleanup("try")
                .tags(tags).compressionString("gzip").build();

        ImageConfiguration imageConfiguration = ImageConfiguration.builder()
                .name("test").alias("test-app").build(buildImageConfiguration)
                .registry("docker.io").build();
        images.add(imageConfiguration);

        jobHandler = new JobHandler(new PodTemplateHandler(new ContainerHandler(project.getProperties(),
                new GroupArtifactVersion("g","a","v"), probeHandler)));
    }

    @Test
    void get_withValidControllerName_shouldReturnConfigWithContainers() {
        ResourceConfig config = ResourceConfig.builder()
                .imagePullPolicy("IfNotPresent")
                .controllerName("testing")
                .serviceAccount("test-account")
                .restartPolicy("OnFailure")
                .volumes(volumes)
                .build();

        Job job = jobHandler.get(config,images);
        assertThat(job.getSpec().getTemplate().getSpec().getContainers()).isNotNull();
        assertThat(job)
            .satisfies(j -> assertThat(j.getMetadata())
                .isNotNull()
                .hasFieldOrPropertyWithValue("name", "testing")
            )
            .satisfies(j -> assertThat(j.getSpec())
                .isNotNull()
                .extracting(JobSpec::getTemplate).isNotNull()
                .extracting(PodTemplateSpec::getSpec)
                .hasFieldOrPropertyWithValue("serviceAccountName", "test-account")
                .hasFieldOrPropertyWithValue("restartPolicy", "OnFailure")
                .extracting(PodSpec::getVolumes).asList()
                .first()
                .hasFieldOrPropertyWithValue("name", "test")
                .hasFieldOrPropertyWithValue("hostPath.path", "/test/path")
            );
    }

    @Test
    void get_withInvalidControllerName_shouldThrowException() {
      ResourceConfig config = ResourceConfig.builder()
              .imagePullPolicy("IfNotPresent")
              .controllerName("TesTing")
              .serviceAccount("test-account")
              .volumes(volumes)
              .build();

      assertThatIllegalArgumentException()
          .isThrownBy(() -> jobHandler.get(config, images))
          .withMessageStartingWith("Invalid upper case letter 'T'")
          .withMessageEndingWith("controller name value: TesTing");
    }

    @Test
    void get_withoutControllerName_shouldThrowException() {
      ResourceConfig config = ResourceConfig.builder()
              .imagePullPolicy("IfNotPresent")
              .serviceAccount("test-account")
              .volumes(volumes)
              .build();

      assertThatIllegalArgumentException().isThrownBy(() -> jobHandler.get(config, images))
          .withMessage("No controller name is specified!");
    }

    @Test
    void overrideReplicas() {
        // Given
        final KubernetesListBuilder klb = new KubernetesListBuilder().addToItems(new Job());
        // When
        jobHandler.overrideReplicas(klb, 1337);
        // Then
        assertThat(klb.buildItems())
            .singleElement()
            .isEqualTo(new Job());
    }
}
