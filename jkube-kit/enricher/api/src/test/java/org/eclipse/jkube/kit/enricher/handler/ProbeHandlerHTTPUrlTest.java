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
package org.eclipse.jkube.kit.enricher.handler;

import io.fabric8.kubernetes.api.model.Probe;
import org.eclipse.jkube.kit.config.resource.ProbeConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ProbeHandlerHTTPUrlTest {
  private ProbeHandler probeHandler;

  @BeforeEach
  void setUp() {
    probeHandler = new ProbeHandler();
  }

  @ParameterizedTest(name = "HTTP Probe with ''{0}'' URL returns null probe")
  @NullSource
  @ValueSource(strings = {"tcp://www.healthcheck.com:8080/healthz", "www.healthcheck.com:8080/healthz"})
  void getProbe_withProvidedUrl_shouldGenerateHTTPProbe(String url) {
    // Given
    ProbeConfig probeConfig = ProbeConfig.builder()
        .initialDelaySeconds(5).timeoutSeconds(5).getUrl(url)
        .build();

    // When
    Probe generatedProbe = probeHandler.getProbe(probeConfig);

    // Then
    assertThat(generatedProbe).isNull();
  }
}
