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

import java.util.Map;

import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.Plugin;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.junit.Test;
import org.mockito.Mock;

import static org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy.s2i;
import static org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy.docker;
import static org.eclipse.jkube.kit.config.resource.RuntimeMode.OPENSHIFT;
import static org.junit.Assert.assertEquals;

public class FromSelectorTest {

  JavaProject project;

  Plugin plugin;

  KitLogger logger;

  @Test
  public void simple() {
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
          .project(project)
          .config(new ProcessorConfig())
          .logger(logger)
          .runtimeMode(tc.runtimeMode)
          .strategy(tc.strategy)
          .build();

      FromSelector selector = new FromSelector.Default(ctx, "test");
      assertEquals(tc.expectedFrom, selector.getFrom());
      Map<String, String> fromExt = selector.getImageStreamTagFromExt();
      assertEquals(3, fromExt.size());
      assertEquals("ImageStreamTag", fromExt.get(JKubeBuildStrategy.SourceStrategy.kind.key()));
      assertEquals("openshift", fromExt.get(JKubeBuildStrategy.SourceStrategy.namespace.key()));
      assertEquals(tc.expectedName, fromExt.get(JKubeBuildStrategy.SourceStrategy.name.key()));
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
