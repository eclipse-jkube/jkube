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
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleIT {

  @Test
  public void tasks_containsKubernetesAndOpenShiftTasks() throws Exception {
    final String baseDir = System.getProperty("itDir", "");
    final BuildResult result = GradleRunner.create()
        .withGradleDistribution(new URI("https://services.gradle.org/distributions/gradle-6.8-bin.zip"))
        .withDebug(true)
        .withProjectDir(new File(baseDir).toPath().resolve("src").resolve("it").resolve("simple").toFile())
        .withPluginClasspath(Arrays.asList(
            module(baseDir, "jkube-kit", "common"),
            module(baseDir, "jkube-kit", "config", "resource"),
            module(baseDir, "jkube-kit", "config", "service"),
            module(baseDir, "jkube-kit", "config", "image"),
            module(baseDir, "jkube-kit", "build", "service", "docker"),
            module(baseDir, "gradle-plugin", "kubernetes"),
            module(baseDir, "gradle-plugin", "openshift")
        ))
        .withArguments("tasks")
        .build();
    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("Kubernetes tasks")
        .contains("k8sApply - ")
        .contains("k8sBuild - ")
        .contains("k8sResource - ")
        .contains("Openshift tasks")
        .contains("ocApply - ")
        .contains("ocBuild - ")
        .contains("ocResource - ");
  }

  private static File module(String baseDir, String... subdirs) {
    Path dir = new File(baseDir).getAbsoluteFile().toPath().getParent().getParent();
    for (String subdir : subdirs) {
      dir = dir.resolve(subdir);
    }
    return dir.resolve("target").resolve("classes").toFile();
  }
}
