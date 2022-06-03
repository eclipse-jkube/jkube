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

import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.JavaProject;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class MicroprofileHealthUtilTest {
  @Test
  public void hasMicroProfileHealthDependency_withMicroProfileInInvalidGroup_shouldReturnTrue() {
    // Given
    JavaProject javaProject = createNewJavaProjectDependency("org.wildfly.swarm", "microprofile", "2018.5.0");

    // When
    boolean result = MicroprofileHealthUtil.hasMicroProfileHealthDependency(javaProject);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  public void hasMicroProfileDependency_withMicroProfile_shouldReturnTrue() {
    // Given
    JavaProject javaProject = createNewJavaProjectDependency("org.eclipse.microprofile", "microprofile", "5.0");

    // When
    boolean result = MicroprofileHealthUtil.hasMicroProfileDependency(javaProject);

    // Then
    assertThat(result).isTrue();
  }

  @Test
  public void hasMicroProfileHealthDependency_withMicroProfileHealth_shouldReturnTrue() {
    // Given
    JavaProject javaProject = createNewJavaProjectDependency("org.eclipse.microprofile", "microprofile-health", "5.0");

    // When
    boolean result = MicroprofileHealthUtil.hasMicroProfileHealthDependency(javaProject);

    // Then
    assertThat(result).isTrue();
  }

  @Test
  public void isStartupEndpointSupported_withNoDependency_shouldReturnFalse() {
    // Given
    JavaProject javaProject = JavaProject.builder().build();

    // When
    boolean result = MicroprofileHealthUtil.isStartupEndpointSupported(javaProject);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  public void isStartupEndpointSupported_withMicroprofileBefore3_1_shouldReturnFalse() {
    // Given
    JavaProject javaProject = createNewJavaProjectDependency("org.eclipse.microprofile", "microprofile", "2.2");

    // When
    boolean result = MicroprofileHealthUtil.isStartupEndpointSupported(javaProject);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  public void isStartupEndpointSupported_withMicroprofileAfter3_1_shouldReturnTrue() {
    // Given
    JavaProject javaProject = createNewJavaProjectDependency("org.eclipse.microprofile", "microprofile", "5.0");

    // When
    boolean result = MicroprofileHealthUtil.isStartupEndpointSupported(javaProject);

    // Then
    assertThat(result).isTrue();
  }

  @Test
  public void isStartupEndpointSupported_withMicroprofileHealthAfter3_1_shouldReturnTrue() {
    // Given
    JavaProject javaProject = createNewJavaProjectDependency("org.eclipse.microprofile", "microprofile-health", "5.0");

    // When
    boolean result = MicroprofileHealthUtil.isStartupEndpointSupported(javaProject);

    // Then
    assertThat(result).isTrue();
  }

  private JavaProject createNewJavaProjectDependency(String groupId, String artifactId, String version) {
    return JavaProject.builder().dependency(Dependency.builder()
        .groupId(groupId)
        .artifactId(artifactId)
        .version(version)
        .build()).build();
  }
}
