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
package org.eclipse.jkube.micronaut;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.eclipse.jkube.micronaut.MicronautUtils.getMicronautConfiguration;

@RunWith(Parameterized.class)
public class MicronautUtilsGetMicronautConfigurationTest {

  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[]{"properties", "PROPERTIES"},
        new Object[]{"yaml", "YAML"},
        new Object[]{"json", "JSON"}
    );
  }

  @Parameterized.Parameter
  public String directory;

  @Parameterized.Parameter(1)
  public String nameSuffix;

  @Test
  public void getMicronautConfigurationFromProperties() {
    // Given
    final URLClassLoader ucl = URLClassLoader.newInstance(new URL[] {
        MicronautUtilsGetMicronautConfigurationTest.class.getResource(String.format("/utils-test/port-config/%s/", directory))
    });
    // When
    final Properties props = getMicronautConfiguration(ucl);
    // Then
    assertThat(props).containsExactly(
        entry("micronaut.application.name", "port-config-test-" + nameSuffix),
        entry("micronaut.server.port", "1337"));
  }

}
