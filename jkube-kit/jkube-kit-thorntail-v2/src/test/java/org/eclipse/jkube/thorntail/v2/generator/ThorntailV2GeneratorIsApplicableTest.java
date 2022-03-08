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
package org.eclipse.jkube.thorntail.v2.generator;


import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.Plugin;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class ThorntailV2GeneratorIsApplicableTest {

  private JavaProject project;
  private GeneratorContext context;

  @Before
  public void setUp() {
    project = mock(JavaProject.class, Mockito.RETURNS_DEEP_STUBS);
    context = mock(GeneratorContext.class, Mockito.RETURNS_DEEP_STUBS);
    when(context.getProject()).thenReturn(project);
  }

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[] { "ThorntailV2 generator SHOULD NOT be applicable if there is no plugin nor dependency", Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false},
        new Object[] { "ThorntailV2 generator SHOULD be applicable if there is thorntail maven plugin", Collections.singletonList(Plugin.builder().groupId("io.thorntail").artifactId("thorntail-maven-plugin").build()), Collections.emptyList(), Collections.emptyList(), true},
        new Object[] { "ThorntailV2 generator SHOULD be applicable if there is thorntail gradle plugin", Collections.emptyList(), Collections.emptyList(), Collections.singletonList("org.wildfly.swarm.plugin.gradle.ThorntailArquillianPlugin"), true},
        new Object[] { "ThorntailV2 generator SHOULD NOT be applicable if there is thorntail kernel dependency", Collections.singletonList(Plugin.builder().groupId("io.thorntail").artifactId("thorntail-maven-plugin").build()), Collections.singletonList(Dependency.builder().groupId("io.thorntail").artifactId("thorntail-kernel").build()), Collections.emptyList(), false}
    );
  }

  @Parameterized.Parameter
  public String testDescription;

  @Parameterized.Parameter (1)
  public List<Plugin> pluginList;

  @Parameterized.Parameter (2)
  public List<Dependency> dependencyList;

  @Parameterized.Parameter (3)
  public List<String> gradlePluginList;

  @Parameterized.Parameter (4)
  public boolean expectedValue;


  @Test
  public void testIsApplicable() {
    // Given
    when(project.getPlugins()).thenReturn(pluginList);
    when(project.getDependencies()).thenReturn(dependencyList);
    when(project.getGradlePlugins()).thenReturn(gradlePluginList);
    // When
    final boolean result = new ThorntailV2Generator(context).isApplicable(Collections.emptyList());
    // Then
    assertThat(result).isEqualTo(expectedValue);
  }
}
