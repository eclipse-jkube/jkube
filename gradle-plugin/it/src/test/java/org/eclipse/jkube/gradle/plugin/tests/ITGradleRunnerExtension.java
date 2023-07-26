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
package org.eclipse.jkube.gradle.plugin.tests;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ITGradleRunnerExtension implements BeforeEachCallback, AfterEachCallback {
  private GradleRunner gradleRunner;

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    gradleRunner = GradleRunner.create()
        .withGradleDistribution(new URI("https://services.gradle.org/distributions/gradle-8.2.1-bin.zip"))
        .withDebug(true)
        .withPluginClasspath(Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
            .map(File::new).collect(Collectors.toList()));
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    gradleRunner = null;
  }

  public ITGradleRunnerExtension withITProject(String name) {
    final String baseDir = System.getProperty("itDir", "");
    gradleRunner = gradleRunner
        .withProjectDir(new File(baseDir).toPath().resolve("src").resolve("it").resolve(name).toFile());
    return this;
  }

  public ITGradleRunnerExtension withArguments(String... originalArguments) {
    final String[] arguments = new String[originalArguments.length + 2];
    arguments[0] = "-PjKubeVersion=" + System.getProperty("jKubeVersion");
    arguments[1] = "--console=plain";
    System.arraycopy(originalArguments, 0, arguments, 2, originalArguments.length);
    gradleRunner = gradleRunner.withArguments(arguments);
    return this;
  }

  public File resolveFile(String... relativePaths) {
    Path path = gradleRunner.getProjectDir().toPath();
    for (String rp : relativePaths) {
      path = path.resolve(rp);
    }
    return path.toFile();
  }

  public File resolveDefaultKubernetesHelmMetadataFile(String projectName) {
    return resolveFile("build", "jkube", "helm", projectName, "kubernetes", "Chart.yaml");
  }

  public File resolveDefaultOpenShiftHelmMetadataFile(String projectName) {
    return resolveFile("build", "jkube", "helm", projectName, "openshift", "Chart.yaml");
  }

  public File resolveDefaultKubernetesResourceFile() {
    return resolveFile("build", "classes", "java", "main", "META-INF", "jkube", "kubernetes.yml");
  }

  public File resolveDefaultOpenShiftResourceFile() {
    return resolveFile("build", "classes", "java", "main", "META-INF", "jkube", "openshift.yml");
  }

  public File resolveDefaultOpenShiftResourceDir() {
    return resolveFile("build", "classes", "java", "main", "META-INF", "jkube", "openshift");
  }

  public File resolveDefaultDockerfile(String imageNamespace, String imageName, String imageTag) {
    return resolveFile("build", "docker", imageNamespace, imageName, imageTag, "build", "Dockerfile");
  }

  public BuildResult build() {
    return gradleRunner.build();
  }
}
