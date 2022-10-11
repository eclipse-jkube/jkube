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
package org.eclipse.jkube.generator.api;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy.s2i;
import static org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy.docker;
import static org.eclipse.jkube.kit.config.resource.RuntimeMode.OPENSHIFT;

class FromSelectorTest {


  @Test
  void simple() {
    final TestCase[] testCases = new TestCase[]{
        new TestCase(OPENSHIFT, s2i, "s2i-prop", "istag-prop"),
        new TestCase(OPENSHIFT, docker, "docker-prop", "istag-prop"),
        new TestCase(null, s2i,  "docker-prop", "istag-prop"),
        new TestCase(null, docker,  "docker-prop", "istag-prop"),
        new TestCase(OPENSHIFT, null,  "docker-prop", "istag-prop"),
        new TestCase(OPENSHIFT, null,  "docker-prop", "istag-prop"),
        new TestCase(null, null,  "docker-prop", "istag-prop"),
        new TestCase(null, null,  "docker-prop", "istag-prop"),
    };
    for (TestCase tc : testCases) {
      GeneratorContext ctx = GeneratorContext.builder()
          .config(new ProcessorConfig())
          .runtimeMode(tc.runtimeMode)
          .strategy(tc.strategy)
          .build();

      FromSelector selector = new FromSelector.Default(ctx, "test");
      assertThat(selector)
          .hasFieldOrPropertyWithValue("from", tc.expectedFrom)
          .extracting(FromSelector::getImageStreamTagFromExt)
          .asInstanceOf(InstanceOfAssertFactories.MAP)
          .hasSize(3)
          .containsEntry(JKubeBuildStrategy.SourceStrategy.kind.key(), "ImageStreamTag")
          .containsEntry(JKubeBuildStrategy.SourceStrategy.namespace.key(), "openshift")
          .containsEntry(JKubeBuildStrategy.SourceStrategy.name.key(), tc.expectedName);
    }
  }

  private static final class TestCase {
    private final RuntimeMode runtimeMode;
    private final JKubeBuildStrategy strategy;
    private final String expectedFrom;
    private final String expectedName;

    public TestCase(RuntimeMode runtimeMode, JKubeBuildStrategy strategy, String expectedFrom, String expectedName) {
      this.runtimeMode = runtimeMode;
      this.strategy = strategy;
      this.expectedFrom = expectedFrom;
      this.expectedName = expectedName;
    }
  }
}
