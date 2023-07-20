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

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jkube.kit.resource.helm.HelmConfig;
import org.eclipse.jkube.maven.plugin.mojo.OpenShift;

import java.io.File;

@Mojo(name = "helm", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST)
public class OpenshiftHelmMojo extends HelmMojo {

  /**
   * The generated kubernetes YAML file
   */
  @Parameter(property = "jkube.openshiftManifest", defaultValue = "${basedir}/target/classes/META-INF/jkube/openshift.yml")
  private File openShiftManifest;

  /**
   * The generated kubernetes YAML file
   */
  @Parameter(property = "jkube.kubernetesTemplate", defaultValue = "${basedir}/target/classes/META-INF/jkube/openshift")
  private File openShiftTemplate;

  @Override
  protected File getKubernetesManifest() {
    return openShiftManifest;
  }

  @Override
  protected File getKubernetesTemplate() {
    return openShiftTemplate;
  }

  @Override
  protected HelmConfig.HelmType getDefaultHelmType() {
    return HelmConfig.HelmType.OPENSHIFT;
  }

  @Override
  protected String getLogPrefix() {
    return OpenShift.DEFAULT_LOG_PREFIX;
  }

  @Override
  protected void logManifestNotFoundWarning(File manifest) {
    getKitLogger().warn("No openshift manifest file has been generated yet by the oc:resource goal at: " + manifest);
  }
}
