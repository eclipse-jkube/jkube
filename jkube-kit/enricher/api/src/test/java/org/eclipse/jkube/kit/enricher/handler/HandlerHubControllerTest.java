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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Properties;

import org.eclipse.jkube.kit.config.resource.GroupArtifactVersion;

import org.junit.Before;
import org.junit.Test;

public class HandlerHubControllerTest {

  private HandlerHub handlerHub;

  @Before
  public void setUp() throws Exception {
    handlerHub = new HandlerHub(new GroupArtifactVersion("com.example", "artifact", "1.33.7"), new Properties());
  }

  @Test
  public void getControllerHandlers_shouldReturnAllImplementationsOfControllerHandler() {
    // When
    final List<? extends ControllerHandler<?>> result = handlerHub.getControllerHandlers();
    // Then
    assertThat(result)
        .hasSize(7)
        .flatExtracting(Object::getClass)
        .containsExactlyInAnyOrder(
            DaemonSetHandler.class,
            DeploymentConfigHandler.class,
            DeploymentHandler.class,
            JobHandler.class,
            ReplicaSetHandler.class,
            ReplicationControllerHandler.class,
            StatefulSetHandler.class
        );
  }

}