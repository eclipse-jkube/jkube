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
package org.eclipse.jkube.generator.javaexec;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.Plugin;
import org.eclipse.jkube.kit.config.image.build.OpenShiftBuildStrategy;
import org.eclipse.jkube.generator.api.FromSelector;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import mockit.Expectations;
import mockit.Mocked;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author roland
 */
public class JavaRunGeneratorTest {

  @Mocked
  GeneratorContext ctx;

  @Test
  public void fromSelector() throws IOException {
    final List<TestCase> testCases = Arrays.asList(
        new TestCase("3.1.123", false, RuntimeMode.KUBERNETES, null, "java.upstream.docker"),
        new TestCase("3.1.redhat-101", true, RuntimeMode.KUBERNETES, null, "java.upstream.docker"),
        new TestCase("3.1.123", false, RuntimeMode.OPENSHIFT, OpenShiftBuildStrategy.docker, "java.upstream.docker"),
        new TestCase("3.1.redhat-101", true, RuntimeMode.OPENSHIFT, OpenShiftBuildStrategy.docker, "java.upstream.docker"),
        new TestCase("3.1.123", false, RuntimeMode.OPENSHIFT, OpenShiftBuildStrategy.s2i, "java.upstream.s2i"),
        new TestCase("3.1.redhat-101", true, RuntimeMode.OPENSHIFT, OpenShiftBuildStrategy.s2i, "java.upstream.s2i"));

    Properties imageProps = getDefaultImageProps();

    for (TestCase tc : testCases) {
      prepareExpectation(tc);
      final GeneratorContext context = ctx;
      FromSelector selector = new FromSelector.Default(context, "java");
      String from = selector.getFrom();
      assertEquals(imageProps.getProperty(tc.expectedFrom), from);
    }
  }

  private void prepareExpectation(TestCase testCase) {
    final JavaProject.JavaProjectBuilder projectBuilder = JavaProject.builder();
    if (testCase.hasOpenShiftPlugin) {
      projectBuilder.plugins(Collections.singletonList(
          Plugin.builder().groupId("org.eclipse.jkube").artifactId("openshift-maven-plugin")
              .version(testCase.version).configuration(Collections.emptyMap()).build()));
    }
    new Expectations() {{
      ctx.getRuntimeMode();
      result = testCase.mode;
      ctx.getStrategy();
      result = testCase.strategy;
    }};
  }

  private Properties getDefaultImageProps() throws IOException {
    Properties props = new Properties();
    Enumeration<URL> resources = getClass().getClassLoader().getResources("META-INF/jkube/default-images.properties");
    while (resources.hasMoreElements()) {
      props.load(resources.nextElement().openStream());
    }
    return props;
  }

  private static final class TestCase {
    final String version;
    final boolean hasOpenShiftPlugin;
    final RuntimeMode mode;
    final OpenShiftBuildStrategy strategy;
    final String expectedFrom;

    public TestCase(String version, boolean hasOpenShiftPlugin, RuntimeMode mode, OpenShiftBuildStrategy strategy,
        String expectedFrom) {
      this.version = version;
      this.hasOpenShiftPlugin = hasOpenShiftPlugin;
      this.mode = mode;
      this.strategy = strategy;
      this.expectedFrom = expectedFrom;
    }
  }
}
