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
package org.eclipse.jkube.maven.plugin.mojo.build;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import org.eclipse.jkube.kit.resource.helm.HelmConfig;
import org.eclipse.jkube.kit.resource.helm.HelmService;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProjectHelper;

import static org.eclipse.jkube.kit.resource.helm.HelmServiceUtil.initHelmConfig;

/**
 * Generates a Helm chart for the kubernetes resources
 */
@Mojo(name = "helm", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST)
public class HelmMojo extends AbstractJKubeMojo {

  @Component
  private MavenProjectHelper projectHelper;

  /**
   * The generated kubernetes YAML file
   */
  @Parameter(property = "jkube.kubernetesManifest", defaultValue = "${basedir}/target/classes/META-INF/jkube/kubernetes.yml")
  File kubernetesManifest;

  /**
   * The generated kubernetes YAML file
   */
  @Parameter(property = "jkube.kubernetesTemplate", defaultValue = "${basedir}/target/classes/META-INF/jkube/kubernetes")
  File kubernetesTemplate;

  @Parameter
  HelmConfig helm;

  @Override
  public void executeInternal() throws MojoExecutionException {
    try {
      File manifest = getKubernetesManifest();
      if (manifest == null || !manifest.isFile()) {
        getKitLogger().warn("No kubernetes manifest file has been generated yet by the kubernetes:resource goal at: " + manifest);
      }
      helm = initHelmConfig(getDefaultHelmType(), javaProject, getKubernetesManifest(), getKubernetesTemplate(), helm)
          .generatedChartListeners(Collections.singletonList((helmConfig, type, chartFile) -> projectHelper
              .attachArtifact(project, helmConfig.getChartExtension(), type.getClassifier(), chartFile)))
          .build();
      HelmService.generateHelmCharts(log, helm);
    } catch (IOException exception) {
      throw new MojoExecutionException(exception.getMessage());
    }
  }

  protected File getKubernetesManifest() {
    return kubernetesManifest;
  }

  protected File getKubernetesTemplate() {
     return kubernetesTemplate;
  }

  protected HelmConfig.HelmType getDefaultHelmType() {
    return HelmConfig.HelmType.KUBERNETES;
  }

  HelmConfig getHelm() {
    return helm;
  }
}
