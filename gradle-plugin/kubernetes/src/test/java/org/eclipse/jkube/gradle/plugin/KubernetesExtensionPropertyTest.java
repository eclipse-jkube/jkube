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
package org.eclipse.jkube.gradle.plugin;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.jkube.kit.common.JavaProject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class KubernetesExtensionPropertyTest {

  @Parameterized.Parameters(name = "{index} {0}, with {1}={2}, returns {3}")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[] { "getOfflineOrDefault", "jkube.offline", "false", false },
        new Object[] { "getUseProjectClassPathOrDefault", "jkube.useProjectClasspath", "true", true });
  }

  @Parameterized.Parameter
  public String method;

  @Parameterized.Parameter(1)
  public String property;

  @Parameterized.Parameter(2)
  public String propertyValue;

  @Parameterized.Parameter(3)
  public Object expectedValue;

  @Test
  public void test() throws Exception {
    // Given
    final KubernetesExtension extension = new TestKubernetesExtension();
    extension.javaProject = JavaProject.builder().build();
    extension.javaProject.getProperties().setProperty(property, propertyValue);
    // When
    final Object result = extension.getClass().getMethod(method).invoke(extension);
    // Then
    assertThat(result).isEqualTo(expectedValue);
  }
}
