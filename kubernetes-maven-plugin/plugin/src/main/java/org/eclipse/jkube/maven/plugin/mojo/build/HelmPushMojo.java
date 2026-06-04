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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import static org.eclipse.jkube.kit.resource.helm.HelmServiceUtil.initHelmPushConfig;

@Mojo(name = "helm-push", defaultPhase = LifecyclePhase.INSTALL, requiresDependencyResolution = ResolutionScope.COMPILE)
public class HelmPushMojo extends AbstractHelmMojo {

  private static final String DEFAULT_SECURITY = "~/.m2/settings-security.xml";

  @Override
  public void init() throws MojoFailureException {
    super.init();

    initHelmPushConfig(helm, javaProject);
    if (helm.getSecurity() != null && !DEFAULT_SECURITY.equals(helm.getSecurity())) {
      getKitLogger().warn("The <security> helm configuration and jkube.helm.security property are deprecated" +
          " and will be removed in a future version." +
          " Use Maven's -Dsettings.security=<file> property instead.");
    }
  }

  @Override
  public void executeInternal() throws MojoExecutionException {
    try {
      jkubeServiceHub.getHelmService().uploadHelmChart(getHelm());
    } catch (Exception exp) {
      getKitLogger().error("Error performing Helm push", exp);
      throw new MojoExecutionException(exp.getMessage(), exp);
    }
  }
}
