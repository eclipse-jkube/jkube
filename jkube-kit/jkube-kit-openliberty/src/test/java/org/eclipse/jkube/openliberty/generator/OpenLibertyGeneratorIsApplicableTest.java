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
package org.eclipse.jkube.openliberty.generator;

import org.eclipse.jkube.generator.api.GeneratorContext;
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

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class OpenLibertyGeneratorIsApplicableTest {
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
        new Object[] { "OpenLiberty generator SHOULD NOT be applicable if there is no plugin nor dependency", Collections.emptyList(), Collections.emptyList(), false},
        new Object[] { "OpenLiberty generator SHOULD be applicable if there is liberty maven plugin", Collections.singletonList(Plugin.builder().groupId("io.openliberty.tools").artifactId("liberty-maven-plugin").build()), Collections.emptyList(), true},
        new Object[] { "OpenLiberty generator SHOULD be applicable if there is liberty gradle plugin", Collections.singletonList(Plugin.builder().groupId("io.openliberty.tools.gradle.Liberty").artifactId("io.openliberty.tools.gradle.Liberty.gradle.plugin").build()), Collections.emptyList(), true},
        new Object[] { "OpenLiberty generator SHOULD be applicable if there is gradle plugin with class name io.openliberty.tools.gradle.Liberty", Collections.emptyList(), Collections.singletonList("io.openliberty.tools.gradle.Liberty"), true}
    );
  }

  @Parameterized.Parameter
  public String testDescription;

  @Parameterized.Parameter(1)
  public List<Plugin> pluginList;

  @Parameterized.Parameter(2)
  public List<String> gradlePluginsList;

  @Parameterized.Parameter(3)
  public boolean expectedValue;


  @Test
  public void testIsApplicable() {
    // Given
    when(project.getPlugins()).thenReturn(pluginList);
    when(project.getGradlePlugins()).thenReturn(gradlePluginsList);
    // When
    final boolean result = new OpenLibertyGenerator(context).isApplicable(Collections.emptyList());
    // Then
    assertThat(result).isEqualTo(expectedValue);
  }
}
