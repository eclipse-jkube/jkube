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
package org.eclipse.jkube.kit.common;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class AssemblyConfigurationTest {

  @Test
  public void testDefaultPermissions() {
    assertThat(new AssemblyConfiguration().getPermissions(), is(AssemblyConfiguration.PermissionMode.keep));
  }

  @Test
  public void testDefaultMode() {
    assertThat(new AssemblyConfiguration().getMode(), is(AssemblyMode.dir));
  }

  /**
   * Verifies that deserialization works for raw deserialization (Maven-Plexus) disregarding annotations.
   *
   * Especially designed to catch problems if Enum names are changed.
   */
  @Test
  public void rawDeserialization() throws IOException {
    // Given
    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(MapperFeature.USE_ANNOTATIONS, false);
    // When
    final AssemblyConfiguration result = mapper.readValue(
        AssemblyConfigurationTest.class.getResourceAsStream("/assembly-configuration.json"),
        AssemblyConfiguration.class
    );
    // Then
    assertThat(result, notNullValue());
    assertThat(result.getName(), is("assembly"));
    assertThat(result.getTargetDir(), is("target"));
    assertThat(result.getDescriptor(), is("assDescriptor"));
    assertThat(result.getDescriptorRef(), is("ass reference"));
    assertThat(result.getExportTargetDir(), is(false));
    assertThat(result.isExcludeFinalOutputArtifact(), is(true));
    assertThat(result.getPermissions(), is(AssemblyConfiguration.PermissionMode.exec));
    assertThat(result.getPermissionsRaw(), is("exec"));
    assertThat(result.getMode(), is(AssemblyMode.zip));
    assertThat(result.getModeRaw(), is("zip"));
    assertThat(result.getUser(), is("root"));
    assertThat(result.getTarLongFileMode(), is("posix"));
    assertThat(result.getInline(), notNullValue());
  }
}
