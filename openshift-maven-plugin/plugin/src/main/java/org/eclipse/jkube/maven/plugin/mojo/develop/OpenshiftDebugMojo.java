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
package org.eclipse.jkube.maven.plugin.mojo.develop;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.eclipse.jkube.maven.plugin.mojo.OpenShift;

import java.io.File;

/**
 * Ensures that the current app has debug enabled, then opens the debug port so that you can debug the latest pod
 * from your IDE
 */
@Mojo(name = "debug", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.PACKAGE)
public class OpenshiftDebugMojo extends DebugMojo {

  /**
   * The generated openshift YAML file
   */
  @Parameter(property = "jkube.openshiftManifest", defaultValue = DEFAULT_OPENSHIFT_MANIFEST)
  private File openshiftManifest;

  @Override
  public File getManifest(KubernetesClient kubernetesClient) {
    return OpenShift.getOpenShiftManifest(kubernetesClient, getKubernetesManifest(), openshiftManifest);
  }

  @Override
  protected String getLogPrefix() {
    return OpenShift.DEFAULT_LOG_PREFIX;
  }
}