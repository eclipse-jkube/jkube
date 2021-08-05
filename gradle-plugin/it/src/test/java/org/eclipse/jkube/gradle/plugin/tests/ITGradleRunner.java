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
package org.eclipse.jkube.gradle.plugin.tests;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.rules.ExternalResource;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ITGradleRunner extends ExternalResource {

  private GradleRunner gradleRunner;

  @Override
  protected void before() throws Throwable {
    gradleRunner = GradleRunner.create()
        .withGradleDistribution(new URI("https://services.gradle.org/distributions/gradle-6.9-bin.zip"))
        .withDebug(true)
        .withPluginClasspath(Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
            .map(File::new).collect(Collectors.toList()));
  }

  @Override
  protected void after() {
    gradleRunner = null;
  }

  public ITGradleRunner withITProject(String name) {
    final String baseDir = System.getProperty("itDir", "");
    gradleRunner = gradleRunner
        .withProjectDir(new File(baseDir).toPath().resolve("src").resolve("it").resolve(name).toFile());
    return this;
  }

  public ITGradleRunner withArguments(String... originalArguments) {
    final String[] arguments = new String[originalArguments.length + 1];
    arguments[0] = "-PjKubeVersion=" + System.getProperty("jKubeVersion");
    System.arraycopy(originalArguments, 0, arguments, 1, originalArguments.length);
    gradleRunner = gradleRunner.withArguments(arguments);
    return this;
  }

  public BuildResult build() {
    return gradleRunner.build();
  }
}
