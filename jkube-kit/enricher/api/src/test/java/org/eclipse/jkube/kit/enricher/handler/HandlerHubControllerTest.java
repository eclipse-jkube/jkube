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

import java.util.List;
import java.util.Properties;

import org.eclipse.jkube.kit.config.resource.GroupArtifactVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HandlerHubControllerTest {

  private HandlerHub handlerHub;

  @BeforeEach
  void setUp() {
    handlerHub = new HandlerHub(new GroupArtifactVersion("com.example", "artifact", "1.33.7"), new Properties());
  }

  @Test
  void getControllerHandlers_shouldReturnAllImplementationsOfControllerHandler() {
    // When
    final List<ControllerHandlerLazyBuilder<?>> result = handlerHub.getControllerHandlers();
    // Then
    assertThat(result)
        .hasSize(7)
        .extracting(ControllerHandlerLazyBuilder::get)
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
