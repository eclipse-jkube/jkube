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

import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jkube.maven.plugin.mojo.OpenShift;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;

/**
 * Base class for goals which deploy the generated artifacts into the OpenShift cluster
 */
@Mojo(name = "apply", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.INSTALL)
public class OpenshiftApplyMojo extends ApplyMojo {

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
