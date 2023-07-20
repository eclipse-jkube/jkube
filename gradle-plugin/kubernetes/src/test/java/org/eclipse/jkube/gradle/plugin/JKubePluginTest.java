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

import java.util.Collection;
import java.util.Map;

import org.gradle.api.Task;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JKubePluginTest {

  @Test
  void getTaskPrecedence_withDefaults_shouldReturnEmpty() {
    // Given
    final JKubePlugin partial = mock(JKubePlugin.class);
    when(partial.getTaskPrecedence()).thenCallRealMethod();
    //When
    final Map<String, Collection<Class<? extends Task>>> result = partial.getTaskPrecedence();
    // Then
    assertThat(result).isEmpty();
  }

}