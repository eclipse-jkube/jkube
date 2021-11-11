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
package org.eclipse.jkube.kit.config.service.kubernetes;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.GenericKubernetesResourceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import mockit.Mocked;
import mockit.Verifications;
import org.junit.Test;

import static org.eclipse.jkube.kit.config.service.kubernetes.KubernetesClientUtil.doDeleteAndWait;

public class KubernetesClientUtilTest {

  @Mocked
  private KubernetesClient kubernetesClient;

  @Test
  public void doDeleteAndWait_withExistingResource_shouldDeleteAndReachWaitLimit() {
    // Given
    GenericKubernetesResource resource = new GenericKubernetesResourceBuilder()
        .withApiVersion("org.eclipse.jkube/v1beta1")
        .withKind("JKubeCustomResource")
        .withNewMetadata().withName("name").endMetadata()
        .build();
    // When
    doDeleteAndWait(kubernetesClient, resource, "namespace",  2L);
    // Then
    // @formatter:off
    new Verifications(){{
      kubernetesClient.genericKubernetesResources("org.eclipse.jkube/v1beta1", "JKubeCustomResource").inNamespace("namespace").withName("name").delete(); times = 1;
    }};
    // @formatter:on
  }

}