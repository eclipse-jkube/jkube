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

import io.fabric8.openshift.client.OpenShiftClient;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.RegistryConfig;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

@SuppressWarnings("unused")
public class OpenShiftBuildServiceTest {

  @Mocked
  private JKubeServiceHub jKubeServiceHub;
  @Mocked
  private KitLogger logger;
  @Mocked
  private ClusterAccess clusterAccess;
  @Mocked
  private OpenShiftClient client;

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Before
  public void setUp() throws Exception {
    // @formatter:off
    new Expectations() {{
      jKubeServiceHub.getLog(); result = logger;
      clusterAccess.createDefaultClient(); result = client;
      client.isAdaptable(OpenShiftClient.class); result = true;
    }};
    // @formatter:on
  }

  @Test
  public void push_withDefaults_shouldLogWarning() throws JKubeServiceException {
    // When
    new OpenshiftBuildService().push(jKubeServiceHub, Collections.emptyList(), 0, new RegistryConfig(), false);
    // Then
    //  @formatter:off
    new Verifications() {{
      logger.warn("Image is pushed to OpenShift's internal registry during oc:build goal. Skipping..."); times = 1;
    }};
    // @formatter:on
  }
}
