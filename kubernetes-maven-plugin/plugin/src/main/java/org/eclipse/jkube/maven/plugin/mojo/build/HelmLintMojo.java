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
package org.eclipse.jkube.maven.plugin.mojo.build;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.eclipse.jkube.kit.resource.helm.HelmConfig;
import org.eclipse.jkube.kit.resource.helm.HelmService;

@Mojo(name = "helm-lint", defaultPhase = LifecyclePhase.INTEGRATION_TEST, requiresDependencyResolution = ResolutionScope.COMPILE)
public class HelmLintMojo extends AbstractHelmMojo {

  @Override
  public void init() throws MojoFailureException {
    super.init();

    checkChartsExist(getHelm());
  }

  private void checkChartsExist(final HelmConfig helmConfig) {
    for (HelmConfig.HelmType helmType : helmConfig.getTypes()) {
      final Path chart = Paths.get(helmConfig.getOutputDir(), helmType.getOutputDir(), HelmService.CHART_FILENAME);
      if (Files.notExists(chart)) {
        logChartNotFoundWarning(chart);
      }
    }
  }

  @Override
  public void executeInternal() throws MojoExecutionException {
    jkubeServiceHub.getHelmService().lint(getHelm());
  }

  protected void logChartNotFoundWarning(final Path chart) {
    getKitLogger().warn("No Helm chart has been generated yet by the k8s:helm goal at: " + chart);
  }
}
