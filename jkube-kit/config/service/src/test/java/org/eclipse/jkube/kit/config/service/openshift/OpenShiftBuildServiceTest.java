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
package org.eclipse.jkube.kit.config.service.openshift;

import mockit.Mocked;
import mockit.Verifications;
import org.eclipse.jkube.kit.common.RegistryConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.junit.Test;

import java.util.Collections;

@SuppressWarnings("unused")
public class OpenShiftBuildServiceTest {

  @Mocked
  private JKubeServiceHub jKubeServiceHub;

  @Test
  public void push_withDefaults_shouldLogWarning() throws JKubeServiceException {
    // When
    new OpenshiftBuildService(jKubeServiceHub).push(Collections.emptyList(), 0, new RegistryConfig(), false);
    // Then
    //  @formatter:off
    new Verifications() {{
      jKubeServiceHub.getLog().warn("Image is pushed to OpenShift's internal registry during oc:build goal. Skipping..."); times = 1;
    }};
    // @formatter:on
  }
}
