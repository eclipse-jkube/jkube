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
package org.eclipse.jkube.kit.enricher.handler;

import io.fabric8.kubernetes.api.model.Probe;
import org.eclipse.jkube.kit.config.resource.ProbeConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@RunWith(Parameterized.class)
public class ProbeHandlerHTTPUrlTest {
  private ProbeHandler probeHandler;

  @Parameterized.Parameters(name = "get HTTP Probe with {0} URL returns null probe")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[] { null},
        new Object[] { "tcp://www.healthcheck.com:8080/healthz"},
        new Object[] { "www.healthcheck.com:8080/healthz"}
    );
  }

  @Parameterized.Parameter
  public String url;

  @Before
  public void setUp() throws Exception {
    probeHandler = new ProbeHandler();
  }

  @Test
  public void getProbe_withProvidedUrl_shouldGenerateHTTPProbe() {
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
