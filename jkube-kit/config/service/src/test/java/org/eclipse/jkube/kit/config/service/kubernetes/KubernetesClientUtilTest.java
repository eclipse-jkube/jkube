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

import static org.eclipse.jkube.kit.config.service.kubernetes.KubernetesClientUtil.doDeleteAndWait;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import mockit.Mocked;
import mockit.Verifications;
import org.junit.Test;

public class KubernetesClientUtilTest {

  @Mocked
  private KubernetesClient kubernetesClient;

  @Test
  public void doDeleteAndWait_withExistingResource_shouldDeleteAndReachWaitLimit() {
    // Given
    final CustomResourceDefinitionContext context = new CustomResourceDefinitionContext();
    // When
    doDeleteAndWait(kubernetesClient, context, "namespace", "name", 2L);
    // Then
    // @formatter:off
    new Verifications(){{
      kubernetesClient.customResource(context).inNamespace("namespace").withName("name").delete(); times = 1;
      kubernetesClient.customResource(context).inNamespace("namespace").withName("name").get(); times = 2;
    }};
    // @formatter:on
  }

}