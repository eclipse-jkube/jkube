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
package org.eclipse.jkube.kit.config.resource;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class IngressRuleConfigTest {
  /**
   * Verifies that deserialization works for raw deserialization (Maven-Plexus).
   */
  @Test
  public void rawDeserialization() throws IOException {
    // Given
    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(MapperFeature.USE_ANNOTATIONS, false);
    // When
    final IngressRuleConfig result = mapper.readValue(
        IngressRuleConfigTest.class.getResourceAsStream("/ingress-rule-config.json"),
        IngressRuleConfig.class);
    // Then
    assertThat(result).isEqualTo(IngressRuleConfig.builder()
        .host("example.com")
        .path(IngressRulePathConfig.builder()
            .pathType("ImplementationSpecific")
            .path("/path")
            .serviceName("service-name")
            .servicePort(8080)
            .resource(IngressRulePathResourceConfig.builder()
                .apiGroup("group.k8s.io")
                .kind("ResourceKind")
                .name("resource-name")
                .build())
        .build())
      .build()
    );
  }
}