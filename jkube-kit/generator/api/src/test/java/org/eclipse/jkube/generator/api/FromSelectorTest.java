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
import org.eclipse.jkube.kit.config.image.build.OpenShiftBuildStrategy;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import mockit.Mocked;
import org.junit.Test;

import static org.eclipse.jkube.kit.config.image.build.OpenShiftBuildStrategy.s2i;
import static org.eclipse.jkube.kit.config.image.build.OpenShiftBuildStrategy.docker;
import static org.eclipse.jkube.kit.config.resource.RuntimeMode.openshift;
import static org.junit.Assert.assertEquals;

public class FromSelectorTest {

  @Mocked
  JavaProject project;

  @Mocked
  Plugin plugin;

  @Mocked
  KitLogger logger;

  @Test
  public void simple() {
    final TestCase[] testCases = new TestCase[]{
        new TestCase(openshift, s2i, "s2i-prop", "istag-prop"),
        new TestCase(openshift, docker, "docker-prop", "istag-prop"),
        new TestCase(null, s2i,  "docker-prop", "istag-prop"),
        new TestCase(null, docker,  "docker-prop", "istag-prop"),
        new TestCase(openshift, null,  "docker-prop", "istag-prop"),
        new TestCase(openshift, null,  "docker-prop", "istag-prop"),
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
      assertEquals("ImageStreamTag", fromExt.get(OpenShiftBuildStrategy.SourceStrategy.kind.key()));
      assertEquals("openshift", fromExt.get(OpenShiftBuildStrategy.SourceStrategy.namespace.key()));
      assertEquals(tc.expectedName, fromExt.get(OpenShiftBuildStrategy.SourceStrategy.name.key()));
    }
  }

  private static final class TestCase {
    private final RuntimeMode runtimeMode;
    private final OpenShiftBuildStrategy strategy;
    private final String expectedFrom;
    private final String expectedName;

    public TestCase(RuntimeMode runtimeMode, OpenShiftBuildStrategy strategy, String expectedFrom, String expectedName) {
      this.runtimeMode = runtimeMode;
      this.strategy = strategy;
      this.expectedFrom = expectedFrom;
      this.expectedName = expectedName;
    }
  }
}
