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
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;

import static org.eclipse.jkube.kit.resource.helm.HelmServiceUtil.initHelmPushConfig;

@Mojo(name = "helm-push", defaultPhase = LifecyclePhase.INSTALL, requiresDependencyResolution = ResolutionScope.COMPILE)
public class HelmPushMojo extends AbstractHelmMojo {

  @Override
  public void init() throws MojoFailureException {
    super.init();

    initHelmPushConfig(helm, javaProject);
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
}
