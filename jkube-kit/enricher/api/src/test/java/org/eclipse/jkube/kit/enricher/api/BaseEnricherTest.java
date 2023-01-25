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
package org.eclipse.jkube.kit.enricher.api;

import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BaseEnricherTest {
  private BaseEnricher baseEnricher;
  private ResourceConfig resourceConfig;
  private JKubeEnricherContext context;

  @BeforeEach
  void setup() {
    resourceConfig = mock(ResourceConfig.class, RETURNS_DEEP_STUBS);
    context = mock(JKubeEnricherContext.class, RETURNS_DEEP_STUBS);
    baseEnricher = new BaseEnricher(context, "base-enricher");
  }

  @Test
  void getImagePullPolicy_whenNoConfigPresent_shouldReturnDefaultImagePullPolicy() {
    // Given + When
    String value = baseEnricher.getImagePullPolicy(null, null);

    // Then
    assertThat(value).isEqualTo("IfNotPresent");
  }

  @Test
  void getImagePullPolicy_whenPullPolicySpecifiedInResourceConfig_shouldReturnPullPolicy() {
    // Given
    when(resourceConfig.getImagePullPolicy()).thenReturn("Never");

    // When
    String value = baseEnricher.getImagePullPolicy(resourceConfig, null);

    // Then
    assertThat(value).isEqualTo("Never");
  }

  @Test
  void getImagePullPolicy_whenPullPolicySpecifiedViaProperty_shouldReturnPullPolicy() {
    // Given
    when(context.getProperty("jkube.imagePullPolicy")).thenReturn("Always");

    // When
    String value = baseEnricher.getImagePullPolicy(resourceConfig, null);

    // Then
    assertThat(value).isEqualTo("Always");
  }
}
