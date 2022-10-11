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
package org.eclipse.jkube.wildfly.jar;

import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.JavaProject;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.wildfly.jar.WildflyJarUtils.isStartupEndpointSupported;

public class WildflyJarUtilsTest {
  private JavaProject project;

  @Before
  public void setup() throws IOException {
    project = JavaProject.builder()
        .properties(new Properties())
        .build();
  }

  @Test
  public void isStartupEndpointSupported_withWildflyJar_Before25_0shouldReturnFalse() {
    // Given
    project.setDependencies(createNewJavaProjectDependencyList("24.0.1.Final"));
    // When
    boolean result = isStartupEndpointSupported(project);
    // Then
    assertThat(result).isFalse();
  }

  @Test
  public void isStartupEndpointSupported_withWildflyJar_After25_0shouldReturnTrue() {
    // Given
    project.setDependencies(createNewJavaProjectDependencyList("26.1.1.Final"));
    // When
    boolean result = isStartupEndpointSupported(project);
    // Then
    assertThat(result).isTrue();
  }

  private List<Dependency> createNewJavaProjectDependencyList(String version) {
    return Collections.singletonList(Dependency.builder()
        .groupId("org.wildfly.plugins")
        .artifactId("wildfly-jar-maven-plugin")
        .version(version)
        .build());
  }
}