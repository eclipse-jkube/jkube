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

import static org.eclipse.jkube.kit.resource.helm.HelmServiceUtil.initHelmPushConfig;

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
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;

@Mojo(name = "helm-push", defaultPhase = LifecyclePhase.INSTALL, requiresDependencyResolution = ResolutionScope.COMPILE)
public class HelmPushMojo extends AbstractHelmMojo {

  @Override
  public void init() throws MojoFailureException {
    super.init();

    checkChartsExist(getHelm());
    initHelmPushConfig(helm, javaProject);
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
    try {
      if (securityDispatcher instanceof DefaultSecDispatcher) {
        ((DefaultSecDispatcher) securityDispatcher).setConfigurationFile(getHelm().getSecurity());
      }
      jkubeServiceHub.getHelmService().uploadHelmChart(getHelm());
    } catch (Exception exp) {
      getKitLogger().error("Error performing Helm push", exp);
      throw new MojoExecutionException(exp.getMessage(), exp);
    }
  }

  protected void logChartNotFoundWarning(final Path chart) {
    getKitLogger().warn("No Helm chart has been generated yet by the k8s:helm goal at: " + chart);
  }
}
