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

import org.eclipse.jkube.kit.config.resource.GroupArtifactVersion;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class HandlerHubTest {

  @Parameterized.Parameters
  public static Collection<Function<HandlerHub, Supplier<Object>>> data() {
    return Arrays.asList(
        hh -> hh::getDeploymentHandler,
        hh -> hh::getDeploymentConfigHandler,
        hh -> hh::getReplicaSetHandler,
        hh -> hh::getReplicationControllerHandler,
        hh -> hh::getStatefulSetHandler,
        hh -> hh::getDaemonSetHandler,
        hh -> hh::getJobHandler,
        hh -> hh::getNamespaceHandler,
        hh -> hh::getProjectHandler,
        hh -> hh::getServiceHandler
    );
  }

  @Parameterized.Parameter
  public Function<HandlerHub, Supplier<Object>> func;

  private HandlerHub handlerHub;

  @Before
  public void setUp() throws Exception {
    handlerHub = new HandlerHub(new GroupArtifactVersion("com.example", "artifact", "1.33.7"), new Properties());
  }

  @Test
  public void lazyBuilderForHandler_returnsAlwaysCachedInstance() {
    assertThat(func.apply(handlerHub).get())
        .isNotNull()
        .isSameAs(func.apply(handlerHub).get());
  }

}