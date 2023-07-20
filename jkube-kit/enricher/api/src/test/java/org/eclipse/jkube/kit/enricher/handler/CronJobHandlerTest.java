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
package org.eclipse.jkube.kit.enricher.handler;

import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.batch.v1.CronJob;
import io.fabric8.kubernetes.api.model.batch.v1.JobSpec;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.resource.ControllerResourceConfig;
import org.eclipse.jkube.kit.config.resource.GroupArtifactVersion;
import org.eclipse.jkube.kit.config.resource.VolumeConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class CronJobHandlerTest {
  private List<VolumeConfig> volumes;
  private List<ImageConfiguration> images;
  private CronJobHandler cronJobHandler;

  @BeforeEach
  void setUp() {
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

    cronJobHandler = new CronJobHandler(new PodTemplateHandler(new ContainerHandler(new Properties(),
        new GroupArtifactVersion("g", "a", "v"), new ProbeHandler())));
  }

  @Test
  void get_withValidConfigs_shouldReturnConfigWithContainers() {
    // Given
    ControllerResourceConfig config = ControllerResourceConfig.builder()
        .schedule("* * * * *")
        .controllerName("testing")
        .restartPolicy("Never")
        .volumes(volumes)
        .build();

    // When
    CronJob cronJob = cronJobHandler.get(config, images);

    // Then
    assertThat(cronJob.getSpec().getJobTemplate().getSpec().getTemplate().getSpec().getContainers())
        .isNotNull();
    assertThat(cronJob)
        .hasFieldOrPropertyWithValue("metadata.name", "testing")
        .hasFieldOrPropertyWithValue("spec.schedule", "* * * * *")
        .satisfies(cj -> assertThat(cj.getSpec().getJobTemplate().getSpec())
            .isNotNull()
            .extracting(JobSpec::getTemplate)
            .extracting(PodTemplateSpec::getSpec)
            .hasFieldOrPropertyWithValue("restartPolicy", "Never")
            .extracting(PodSpec::getVolumes).asList().first()
            .hasFieldOrPropertyWithValue("name", "test")
            .hasFieldOrPropertyWithValue("hostPath.path", "/test/path")
        );
  }

  @Test
  void get_withoutSchedule_shouldThrowException() {
    // Given
    ControllerResourceConfig config = ControllerResourceConfig.builder()
        .controllerName("testing")
        .restartPolicy("Never")
        .volumes(volumes)
        .build();

    // When & Then
    assertThatIllegalArgumentException()
        .isThrownBy(() -> cronJobHandler.get(config, images))
        .withMessage("No schedule is specified!");
  }

  @Test
  void get_withInvalidControllerName_shouldThrowException() {
    // Given
    ControllerResourceConfig config = ControllerResourceConfig.builder()
        .schedule("* * * * *")
        .imagePullPolicy("IfNotPresent")
        .controllerName("TesTing")
        .volumes(volumes)
        .build();
    // When & Then
    assertThatIllegalArgumentException()
        .isThrownBy(() -> cronJobHandler.get(config, images))
        .withMessageStartingWith("Invalid upper case letter 'T'")
        .withMessageEndingWith("controller name value: TesTing");
  }

  @Test
  void get_withoutControllerName_shouldThrowException() {
    // Given
    ControllerResourceConfig config = ControllerResourceConfig.builder()
        .schedule("* * * * *")
        .imagePullPolicy("IfNotPresent")
        .volumes(volumes)
        .build();
    // When & Then
    assertThatIllegalArgumentException()
        .isThrownBy(() -> cronJobHandler.get(config, images))
        .withMessage("No controller name is specified!");
  }

}