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

import org.eclipse.jkube.kit.config.resource.RuntimeMode;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OpenShiftExtensionTest {

  @Test
  public void getRuntimeMode_withDefaults_shouldReturnKubernetes() {
    // Given
    final OpenShiftExtension partial = mock(OpenShiftExtension.class);
    when(partial.getRuntimeMode()).thenCallRealMethod();
    // When
    final RuntimeMode result = partial.getRuntimeMode();
    // Then
    assertThat(result).isEqualTo(RuntimeMode.OPENSHIFT);
  }
}
