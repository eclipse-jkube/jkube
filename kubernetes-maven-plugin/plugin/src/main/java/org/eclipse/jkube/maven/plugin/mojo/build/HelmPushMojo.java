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

import org.eclipse.jkube.kit.common.util.MavenUtil;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;

import static org.eclipse.jkube.kit.resource.helm.HelmServiceUtil.initHelmPushConfig;

@Mojo(name = "helm-push", defaultPhase = LifecyclePhase.INSTALL, requiresDependencyResolution = ResolutionScope.COMPILE)
public class HelmPushMojo extends HelmMojo {

  @Component(role = org.sonatype.plexus.components.sec.dispatcher.SecDispatcher.class, hint = "default")
  protected SecDispatcher securityDispatcher;

  @Override
  protected boolean canExecute() {
    return super.canExecute() && !skip;
  }

  @Override
  public void executeInternal() throws MojoExecutionException {
    if (skip) {
      return;
    }
    try {
      super.executeInternal();
      helm = initHelmPushConfig(helm, javaProject);
      jkubeServiceHub.getHelmService()
          .uploadHelmChart(helm, MavenUtil.getRegistryServerFromMavenSettings(settings), this::getMavenPasswordDecryptionMethod);
    } catch (Exception exp) {
      getKitLogger().error("Error performing helm push", exp);
      throw new MojoExecutionException(exp.getMessage(), exp);
    }
  }

  protected SecDispatcher getSecDispatcher() {
    if (securityDispatcher instanceof DefaultSecDispatcher) {
      ((DefaultSecDispatcher) securityDispatcher).setConfigurationFile(getHelm().getSecurity());
    }
    return securityDispatcher;
  }

  String getMavenPasswordDecryptionMethod(String password) {
    try {
      return getSecDispatcher().decrypt(password);
    } catch (SecDispatcherException e) {
      getKitLogger().error("Failure in decrypting password");
    }
    return null;
  }
}
