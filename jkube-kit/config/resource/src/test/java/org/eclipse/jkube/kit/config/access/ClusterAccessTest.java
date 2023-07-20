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
package org.eclipse.jkube.kit.config.access;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.openshift.client.OpenShiftClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@EnableKubernetesMockClient(https = false)
class ClusterAccessTest {
  private KubernetesMockServer mockServer;
  private ClusterConfiguration clusterConfiguration;

  @BeforeEach
  void setUp() {
    clusterConfiguration = ClusterConfiguration.builder()
      .masterUrl(mockServer.url("/"))
      .build();
  }

  @Test
  void createDefaultClientShouldReturnKubernetesClient() {
    // When
    final KubernetesClient result = new ClusterAccess(clusterConfiguration).createDefaultClient();
    // Then
    assertThat(result).isNotNull().isNotInstanceOf(OpenShiftClient.class);
  }
}
