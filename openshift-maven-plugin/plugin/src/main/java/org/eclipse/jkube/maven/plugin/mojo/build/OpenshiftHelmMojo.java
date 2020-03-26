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

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jkube.kit.resource.helm.HelmConfig;

import java.io.File;

@Mojo(name = "helm", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST)
public class OpenshiftHelmMojo extends HelmMojo {

  /**
   * The generated kubernetes YAML file
   */
  @Parameter(property = PROPERTY_KUBERNETES_MANIFEST, defaultValue = "${basedir}/target/classes/META-INF/jkube/openshift.yml")
  File kubernetesManifest; // NOSONAR (Override Mojo Property en HelmMojo)

  /**
   * The generated kubernetes YAML file
   */
  @Parameter(property = PROPERTY_KUBERNETES_TEMPLATE, defaultValue = "${basedir}/target/classes/META-INF/jkube/openshift")
  File kubernetesTemplate; // NOSONAR (Override Mojo Property en HelmMojo)

  @Override
  protected HelmConfig.HelmType getDefaultHelmType() {
    return HelmConfig.HelmType.OPENSHIFT;
  }
}
