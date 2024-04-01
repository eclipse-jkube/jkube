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

import static org.eclipse.jkube.kit.resource.helm.HelmServiceUtil.initHelmConfig;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jkube.kit.resource.helm.HelmConfig;

public abstract class AbstractHelmMojo extends AbstractJKubeMojo {

  /**
   * One of:
   * <ul>
   * <li>A directory containing OpenShift Templates to use as Helm parameters.</li>
   * <li>A file containing a Kubernetes List with OpenShift Template entries to be used as Helm parameters.</li>
   * </ul>
   */
  @Parameter(property = "jkube.kubernetesTemplate", defaultValue = "${basedir}/target/classes/META-INF/jkube/kubernetes")
  File kubernetesTemplate;

  @Parameter
  HelmConfig helm;

  @Override
  public void init() throws MojoFailureException {
    super.init();

    try {
      helm = initHelmConfig(getDefaultHelmType(), javaProject, getKubernetesTemplate(), helm).build();
    } catch (IOException e) {
      throw new MojoFailureException(e.getMessage(), e);
    }
  }

  protected File getKubernetesTemplate() {
    return kubernetesTemplate;
  }

  protected HelmConfig.HelmType getDefaultHelmType() {
    return HelmConfig.HelmType.KUBERNETES;
  }

  protected HelmConfig getHelm() {
    return helm;
  }
}
