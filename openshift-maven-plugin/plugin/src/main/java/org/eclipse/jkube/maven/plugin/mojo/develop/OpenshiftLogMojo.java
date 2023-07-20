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
package org.eclipse.jkube.maven.plugin.mojo.develop;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.eclipse.jkube.kit.config.service.PodLogService;
import org.eclipse.jkube.maven.plugin.mojo.OpenShift;

import java.io.File;

/**
 * This goal tails the log of the most recent pod for the app that was deployed via <code>oc:deploy</code>
 * <p>
 * To terminate the log hit
 * <code>Ctrl+C</code>
 */
@Mojo(name = "log", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.VALIDATE)
public class OpenshiftLogMojo extends LogMojo {

  /**
   * The S2I binary builder BuildConfig name suffix appended to the image name to avoid
   * clashing with the underlying BuildConfig for the Jenkins pipeline
   */
  @Parameter(property = "jkube.s2i.buildNameSuffix", defaultValue = "-s2i")
  protected String s2iBuildNameSuffix;

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
  protected PodLogService.PodLogServiceContext.PodLogServiceContextBuilder podLogServiceContextBuilder() {
    return super.podLogServiceContextBuilder()
        .s2iBuildNameSuffix(s2iBuildNameSuffix);
  }

  @Override
  protected String getLogPrefix() {
    return OpenShift.DEFAULT_LOG_PREFIX;
  }
}
