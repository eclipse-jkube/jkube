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
package org.eclipse.jkube.kit.common.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PluginServiceFactoryNestedContextTest {
  private PluginServiceFactory<TestContext> pluginServiceFactory;

  @BeforeEach
  public void setup() {
    pluginServiceFactory = new PluginServiceFactory<>(new DefaultTestContext());
  }

  @Test
  void createServiceObjects_whenSubInterfacePassed_shouldMatchServiceImplementation() {
    // Given
    final String[] descriptorPaths = new String[] { "service/test-services-nested-context"};
    // When
    final List<TestService> result = pluginServiceFactory.createServiceObjects(descriptorPaths);
    // Then
    assertThat(result).hasSize(1);
  }

  public static class TestServiceImplWithBaseContext implements TestService {
    public TestServiceImplWithBaseContext(BaseContext ctx) { }

    @Override
    public String getName() { return "one"; }
  }

  private interface TestService {
    String getName();
  }

  private static class DefaultTestContext implements TestContext { }

  private interface TestContext extends BaseContext { }

  private interface BaseContext { }
}
