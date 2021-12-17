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
package org.eclipse.jkube.vertx.generator;

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
public class VertxGeneratorIsApplicableTest {
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
        new Object[] { "Vertx generator SHOULD NOT be applicable if there is no plugin nor dependency", Collections.emptyList(), Collections.emptyList(), false},
        new Object[] { "Vertx generator SHOULD be applicable if there is vertx maven plugin", Collections.singletonList(Plugin.builder().groupId("io.reactiverse").artifactId("vertx-maven-plugin").build()), Collections.emptyList(), true},
        new Object[] { "Vertx generator SHOULD be applicable if there is vertx gradle plugin", Collections.singletonList(Plugin.builder().groupId("io.vertx").artifactId("io.vertx.vertx-plugin").build()), Collections.emptyList(), true},
        new Object[] { "Vertx generator SHOULD be applicable if there is dependency with io.vertx groupId", Collections.emptyList(), Collections.singletonList(Dependency.builder().groupId("io.vertx").build()), true}
    );
  }

  @Parameterized.Parameter
  public String testDescription;

  @Parameterized.Parameter(1)
  public List<Plugin> pluginList;

  @Parameterized.Parameter(2)
  public List<Dependency> dependencyList;

  @Parameterized.Parameter(3)
  public boolean expectedValue;


  @Test
  public void testIsApplicable() {
    // Given
    when(project.getPlugins()).thenReturn(pluginList);
    when(project.getDependencies()).thenReturn(dependencyList);
    // When
    final boolean result = new VertxGenerator(context).isApplicable(Collections.emptyList());
    // Then
    assertThat(result).isEqualTo(expectedValue);
  }
}
