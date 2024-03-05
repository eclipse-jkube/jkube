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

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProjectHelper;
import org.eclipse.jkube.kit.resource.helm.GeneratedChartListener;
import org.eclipse.jkube.kit.resource.helm.HelmConfig;

/**
 * Generates a Helm chart for the Kubernetes resources
 */
@Mojo(name = "helm", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST)
public class HelmMojo extends AbstractHelmMojo {

  /**
   * The generated Kubernetes YAML file
   */
  @Parameter(property = "jkube.kubernetesManifest", defaultValue = "${basedir}/target/classes/META-INF/jkube/kubernetes.yml")
  private File kubernetesManifest;

  @Component
  MavenProjectHelper projectHelper;

  @Override
  public void init() throws MojoFailureException {
    super.init();

    final File manifest = getKubernetesManifest();
    if (manifest == null || !manifest.isFile()) {
      logManifestNotFoundWarning(manifest);
    }

    final GeneratedChartListener generatedChartListener = (helmConfig, type, chartFile) -> projectHelper.attachArtifact(project, helmConfig.getChartExtension(), type.getClassifier(), chartFile);
    getHelm().getGeneratedChartListeners().add(generatedChartListener);
  }

  @Override
  public void executeInternal() throws MojoExecutionException {
    try {
      jkubeServiceHub.getHelmService().generateHelmCharts(getHelm());
    } catch (IOException exception) {
      throw new MojoExecutionException(exception.getMessage());
    }
  }

  protected void logManifestNotFoundWarning(File manifest) {
    getKitLogger().warn("No Kubernetes manifest file has been generated yet by the k8s:resource goal at: " + manifest);
  }

  protected File getKubernetesManifest() {
    return kubernetesManifest;
  }

  @Override
  protected HelmConfig.HelmType getDefaultHelmType() {
    return HelmConfig.HelmType.KUBERNETES;
  }
}
