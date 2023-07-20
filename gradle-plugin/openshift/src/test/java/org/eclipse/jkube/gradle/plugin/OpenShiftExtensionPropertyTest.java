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
package org.eclipse.jkube.gradle.plugin;

import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class OpenShiftExtensionPropertyTest {

  public static File BASE = new File("");
  private TestOpenShiftExtension extension;

  @BeforeEach
  void setUp() {
    extension = new TestOpenShiftExtension();
    extension.javaProject = JavaProject.builder()
        .artifactId("artifact-id")
        .baseDirectory(BASE)
        .buildDirectory(new File(BASE, "build"))
        .outputDirectory(new File(BASE, "build"))
        .build();
  }

  @ParameterizedTest(name = "{index}: {0} with defaults return ''{1}''")
  @MethodSource("defaultValues")
  void getValue_withDefaults_shouldReturnDefaultValue(String method, Object expectedDefault) throws Exception {
    // When
    final Object result = extension.getClass().getMethod(method).invoke(extension);
    // Then
    assertThat(result).isEqualTo(expectedDefault);
  }

  static Stream<Arguments> defaultValues() {
    return Stream.of(
        arguments("getBuildStrategyOrDefault", JKubeBuildStrategy.s2i),
        arguments("getOpenshiftPullSecretOrDefault", "pullsecret-jkube"),
        arguments("getS2iBuildNameSuffixOrDefault", "-s2i"),
        arguments("getS2iImageStreamLookupPolicyLocalOrDefault", true),
        arguments("getBuildOutputKindOrDefault", "ImageStreamTag"),
        arguments("getProcessTemplatesLocallyOrDefault", false),
        arguments("getKubernetesManifestOrDefault",
            new File(BASE, "build").toPath().resolve(Paths.get("META-INF", "jkube", "openshift.yml")).toFile()),
        arguments("getImageStreamManifestOrDefault", new File(BASE, "build").toPath().resolve("artifact-id-is.yml").toFile()),
        arguments("getKubernetesTemplateOrDefault",
            new File(BASE, "build").toPath().resolve(Paths.get("META-INF", "jkube", "openshift")).toFile()));
  }

  @ParameterizedTest(name = "{index}: {0} with property ''{1}={2}'' returns ''{3}''")
  @MethodSource("propertiesAndValues")
  void getValue_withProperty_shouldReturnFromPropertyValue(String method, String property, String propertyValue, Object expectedValue) throws Exception {
    // Given
    extension.javaProject.getProperties().setProperty(property, propertyValue);
    // When
    final Object result = extension.getClass().getMethod(method).invoke(extension);
    // Then
    assertThat(result).isEqualTo(expectedValue);
  }

  static Stream<Arguments> propertiesAndValues() {
    return Stream.of(
        arguments("getBuildStrategyOrDefault", "jkube.build.strategy", "jib", JKubeBuildStrategy.jib),
        arguments("getBuildStrategyOrDefault", "jkube.build.strategy", "docker", JKubeBuildStrategy.docker),
        arguments("getOpenshiftPullSecretOrDefault", "jkube.build.pullSecret", "pullsecret-other", "pullsecret-other",
            "pullsecret-jkube"),
        arguments("getS2iBuildNameSuffixOrDefault", "jkube.s2i.buildNameSuffix", "-other", "-other"),
        arguments("getS2iImageStreamLookupPolicyLocalOrDefault", "jkube.s2i.imageStreamLookupPolicyLocal", "false", false),
        arguments("getBuildOutputKindOrDefault", "jkube.build.buildOutput.kind", "DockerImage", "DockerImage"),
        arguments("getProcessTemplatesLocallyOrDefault", "jkube.deploy.processTemplatesLocally", "true", true),
        arguments("getKubernetesManifestOrDefault", "jkube.openshiftManifest",
            Paths.get("META-INF", "jkube", "other.yml").toString(),
            Paths.get("META-INF", "jkube", "other.yml").toFile()),
        arguments("getImageStreamManifestOrDefault", "jkube.openshiftImageStreamManifest",
            Paths.get("test-project-is.yml").toString(),
            Paths.get("test-project-is.yml").toFile()),
        arguments("getKubernetesTemplateOrDefault", "jkube.kubernetesTemplate",
            Paths.get("META-INF", "jkube", "other").toString(),
            Paths.get("META-INF", "jkube", "other").toFile()));
  }
}
