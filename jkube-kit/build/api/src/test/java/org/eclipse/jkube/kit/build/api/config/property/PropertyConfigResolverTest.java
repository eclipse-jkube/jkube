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
package org.eclipse.jkube.kit.build.api.config.property;

import org.eclipse.jkube.kit.common.Arguments;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.build.HealthCheckConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class PropertyConfigResolverTest {

  private PropertyConfigResolver propertyConfigResolver;
  private ImageConfiguration initialImageConfiguration;
  private JavaProject javaProject;

  @BeforeEach
  void setUp() {
    propertyConfigResolver = new PropertyConfigResolver();
    initialImageConfiguration = ImageConfiguration.builder()
      .name("initial-name")
      .alias("initial-alias")
      .build(BuildConfiguration.builder()
        .cmd(Arguments.builder().shell("./initial-cmd").build())
        .cleanup("none")
        .imagePullPolicy("Never")
        .from("initial-from")
        .addCacheFrom("initial-cache-from-image:0.0.0")
        .putEnv("VAR_1", "INITIAL_VALUE_1")
        .label("label-1", "value-1")
        .port("8082")
        .port("9082")
        .tag("initial-tag-1")
        .tag("initial-tag-2")
        .platform("darwin/amd64")
        .healthCheck(HealthCheckConfiguration.builder()
          .interval("30s")
          .build())
        .build())
      .build();
    javaProject = JavaProject.builder()
      .properties(new Properties())
      .build();
    javaProject.getProperties().put("jkube.container-image.name", "image-name");
    javaProject.getProperties().put("jkube.container-image.alias", "image-alias");
    javaProject.getProperties().put("jkube.container-image.cmd", "./cmd");
    javaProject.getProperties().put("jkube.container-image.cleanup", "try");
    javaProject.getProperties().put("jkube.container-image.imagePullPolicy", "Always");
    javaProject.getProperties().put("jkube.container-image.from", "busybox");
    javaProject.getProperties().put("jkube.container-image.cachefrom.1", "cache-from-image:0.0.1");
    javaProject.getProperties().put("jkube.container-image.env.VAR_2", "VALUE_2");
    javaProject.getProperties().put("jkube.container-image.envBuild.VAR_3", "VALUE_3");
    javaProject.getProperties().put("jkube.container-image.labels.label-from-property", "value-2");
    javaProject.getProperties().put("jkube.container-image.ports.1", "8080");
    javaProject.getProperties().put("jkube.container-image.ports.2", "9080");
    javaProject.getProperties().put("jkube.container-image.tags.1", "tag-1");
    javaProject.getProperties().put("jkube.container-image.tags.2", "tag-2");
    javaProject.getProperties().put("jkube.container-image.platforms.1", "linux/amd64");
    javaProject.getProperties().put("jkube.container-image.platforms.2", "linux/arm64");
    javaProject.getProperties().put("jkube.container-image.healthcheck.interval", "10s");
    javaProject.getProperties().put("jkube.container-image.registry", "example-registry.io");
    javaProject.getProperties().put("jkube.container-image.buildpacksBuilderImage", "custom-pack-builder-image:latest");
    javaProject.getProperties().put("jkube.container-image.createImageOptions.platform", "linux/amd64");
    javaProject.getProperties().put("jkube.container-image.assembly.name", "assembly1");
    javaProject.getProperties().put("jkube.container-image.assembly.excludeFinalOutputArtifact", "false");
  }

  @Test
  void usesPrefixForPropertyResolution() {
    javaProject.getProperties().put("app.images.image-1.name", "prefixed-image-name");
    initialImageConfiguration = initialImageConfiguration.toBuilder().propertyResolverPrefix("app.images.image-1").build();
    final ImageConfiguration resolved = propertyConfigResolver.resolve(initialImageConfiguration, javaProject);
    assertThat(resolved.getName()).isEqualTo("prefixed-image-name");
  }

  @Nested
  class SetsIfNotInConfig {

    private ImageConfiguration resolved;

    @BeforeEach
    void setUp() {
      final ImageConfiguration empty = new ImageConfiguration();
      resolved = propertyConfigResolver.resolve(empty, javaProject);
    }

    @Test
    void setsName() {
      assertThat(resolved.getName()).isEqualTo("image-name");
    }

    @Test
    void setsAlias() {
      assertThat(resolved.getAlias()).isEqualTo("image-alias");
    }

    @Test
    void setsCmd() {
      assertThat(resolved.getBuild().getCmd().asStrings()).singleElement().isEqualTo("./cmd");
    }

    @Test
    void setsCleanup() {
      assertThat(resolved.getBuild().getCleanup()).isEqualTo("try");
    }

    @Test
    void setsImagePullPolicy() {
      assertThat(resolved.getBuild().getImagePullPolicy()).isEqualTo("Always");
    }

    @Test
    void setsFrom() {
      assertThat(resolved.getBuild().getFrom()).isEqualTo("busybox");
    }

    @Test
    void setsCacheFrom() {
      assertThat(resolved.getBuild().getCacheFrom()).containsExactly("cache-from-image:0.0.1");
    }

    @Test
    void setsEnv() {
      assertThat(resolved.getBuild().getEnv())
        .containsOnly(
          entry("VAR_2", "VALUE_2"),
          entry("VAR_3", "VALUE_3")
        );
    }

    @Test
    void setsLabels() {
      assertThat(resolved.getBuild().getLabels()).containsOnly(entry("label-from-property", "value-2"));
    }

    @Test
    void setsPorts() {
      assertThat(resolved.getBuild().getPorts()).containsExactlyInAnyOrder("8080", "9080");
    }


    @Test
    void setsTags() {
      assertThat(resolved.getBuild().getTags()).containsExactlyInAnyOrder("tag-1", "tag-2");
    }

    @Test
    void setsPlatforms() {
      assertThat(resolved.getBuild().getPlatforms()).containsExactlyInAnyOrder("linux/amd64", "linux/arm64");
    }

    @Test
    void setsHealthCheckInterval() {
      assertThat(resolved.getBuild().getHealthCheck().getInterval()).isEqualTo("10s");
    }

    @Test
    void setsRegistry() {
      assertThat(resolved.getRegistry()).isEqualTo("example-registry.io");
    }

    @Test
    void setsCreateImageOptions() {
      assertThat(resolved.getBuild().getCreateImageOptions()).containsEntry("platform", "linux/amd64");
    }

    @Test
    void setsBuildPackBuilderImage() {
      assertThat(resolved.getBuild().getBuildpacksBuilderImage()).isEqualTo("custom-pack-builder-image:latest");
    }

    @Test
    void setsAssemblyName() {
      assertThat(resolved.getBuild().getAssembly().getName()).isEqualTo("assembly1");
    }

    @Test
    void setsAssemblyExcludeFinalOutputArtifact() {
      assertThat(resolved.getBuild().getAssembly().isExcludeFinalOutputArtifact()).isFalse();
    }
  }

  @Nested
  class OverridesIfInConfig {

    private ImageConfiguration resolved;

    @BeforeEach
    void setUp() {
      resolved = propertyConfigResolver.resolve(initialImageConfiguration, javaProject);
    }

    @Test
    void overridesName() {
      assertThat(resolved.getName()).isEqualTo("image-name");
    }

    @Test
    void overridesAlias() {
      assertThat(resolved.getAlias()).isEqualTo("image-alias");
    }

    @Test
    void overridesCmd() {
      assertThat(resolved.getBuild().getCmd().asStrings()).singleElement().isEqualTo("./cmd");
    }

    @Test
    void overridesCleanup() {
      assertThat(resolved.getBuild().getCleanup()).isEqualTo("try");
    }

    @Test
    void overridesImagePullPolicy() {
      assertThat(resolved.getBuild().getImagePullPolicy()).isEqualTo("Always");
    }

    @Test
    void overridesFrom() {
      assertThat(resolved.getBuild().getFrom()).isEqualTo("busybox");
    }

    @Test // Replace combine policy
    void overridesCacheFrom() {
      assertThat(resolved.getBuild().getCacheFrom()).containsExactlyInAnyOrder("cache-from-image:0.0.1");
    }

    @Test
    void mergesEnv() {
      assertThat(resolved.getBuild().getEnv())
        .containsOnly(
          entry("VAR_1", "INITIAL_VALUE_1"),
          entry("VAR_2", "VALUE_2"),
          entry("VAR_3", "VALUE_3")
        );
    }

    @Test // Merge combine policy
    void appendsPorts() {
      assertThat(resolved.getBuild().getPorts()).containsExactlyInAnyOrder("8080", "9080", "8082", "9082");
    }

    @Test
    void appendsTags() {
      assertThat(resolved.getBuild().getTags()).containsExactlyInAnyOrder("tag-1", "tag-2", "initial-tag-1", "initial-tag-2");
    }

    @Test
    void appendsPlatforms() {
      assertThat(resolved.getBuild().getPlatforms()).containsExactlyInAnyOrder("linux/amd64", "linux/arm64", "darwin/amd64");
    }

    @Test
    void overridesHealthCheckInterval() {
      assertThat(resolved.getBuild().getHealthCheck().getInterval()).isEqualTo("10s");
    }
  }

  @Nested
  class PreservesInConfig {

    private ImageConfiguration resolved;

    @BeforeEach
    void setUp() {
      javaProject.getProperties().clear();
      resolved = propertyConfigResolver.resolve(initialImageConfiguration, javaProject);
    }

    @Test
    void preservesName() {
      assertThat(resolved.getName()).isEqualTo("initial-name");
    }

    @Test
    void preservesAlias() {
      assertThat(resolved.getAlias()).isEqualTo("initial-alias");
    }

    @Test
    void preservesCmd() {
      assertThat(resolved.getBuild().getCmd().asStrings()).singleElement().isEqualTo("./initial-cmd");
    }

    @Test
    void preservesCleanup() {
      assertThat(resolved.getBuild().getCleanup()).isEqualTo("none");
    }

    @Test
    void preservesImagePullPolicy() {
      assertThat(resolved.getBuild().getImagePullPolicy()).isEqualTo("Never");
    }

    @Test
    void preservesFrom() {
      assertThat(resolved.getBuild().getFrom()).isEqualTo("initial-from");
    }

    @Test
    void preservesCacheFrom() {
      assertThat(resolved.getBuild().getCacheFrom()).containsExactly("initial-cache-from-image:0.0.0");
    }

    @Test
    void preservesEnv() {
      assertThat(resolved.getBuild().getEnv()).containsOnly(entry("VAR_1", "INITIAL_VALUE_1"));
    }

    @Test
    void preservesPorts() {
      assertThat(resolved.getBuild().getPorts()).containsExactlyInAnyOrder("8082", "9082");
    }

    @Test
    void preservesTags() {
      assertThat(resolved.getBuild().getTags()).containsExactlyInAnyOrder("initial-tag-1", "initial-tag-2");
    }

    @Test
    void preservesPlatforms() {
      assertThat(resolved.getBuild().getPlatforms()).containsExactlyInAnyOrder("darwin/amd64");
    }

    @Test
    void preservesHealthCheckInterval() {
      assertThat(resolved.getBuild().getHealthCheck().getInterval()).isEqualTo("30s");
    }
  }

}
