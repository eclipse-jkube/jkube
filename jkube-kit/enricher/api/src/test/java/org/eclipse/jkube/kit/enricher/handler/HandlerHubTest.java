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

import java.util.Properties;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.eclipse.jkube.kit.config.resource.GroupArtifactVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class HandlerHubTest {

  static Stream<Function<HandlerHub, Supplier<Object>>> data() {
    return Stream.of(
        hh -> hh::getNamespaceHandler,
        hh -> hh::getProjectHandler,
        hh -> hh::getServiceHandler
    );
  }

  private HandlerHub handlerHub;

  @BeforeEach
  void setUp() {
    handlerHub = new HandlerHub(new GroupArtifactVersion("com.example", "artifact", "1.33.7"), new Properties());
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("data")
  void lazyBuilderForHandler_returnsAlwaysCachedInstance(Function<HandlerHub, Supplier<Object>> func) {
    assertThat(func.apply(handlerHub).get())
        .isNotNull()
        .isSameAs(func.apply(handlerHub).get());
  }

}
