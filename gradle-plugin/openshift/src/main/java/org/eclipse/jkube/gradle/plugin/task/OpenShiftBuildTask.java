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
package org.eclipse.jkube.gradle.plugin.task;

import org.eclipse.jkube.gradle.plugin.OpenShiftExtension;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.gradle.api.tasks.Internal;

import javax.inject.Inject;

public class OpenShiftBuildTask extends KubernetesBuildTask {
  @Inject
  public OpenShiftBuildTask(Class<? extends OpenShiftExtension> extensionClass) {
    super(extensionClass);
    setDescription(
      "Builds the container images configured for this project via a Docker, S2I binary build or any of the other available build strategies.");
  }

  @Override
  @Internal
  protected String getLogPrefix() {
    return OpenShiftExtension.DEFAULT_LOG_PREFIX;
  }

  @Override
  protected BuildServiceConfig.BuildServiceConfigBuilder buildServiceConfigBuilder() {
    return super.buildServiceConfigBuilder()
      .openshiftPullSecret(asOpenShiftExtension().getOpenshiftPullSecret().getOrElse("pullsecret-jkube"))
      .s2iBuildNameSuffix(asOpenShiftExtension().getS2iBuildNameSuffix().getOrElse("-s2i"))
      .s2iImageStreamLookupPolicyLocal(asOpenShiftExtension().getS2iImageStreamLookupPolicyLocal().getOrElse(true))
      .openshiftPushSecret(asOpenShiftExtension().getOpenshiftPushSecret().getOrNull())
      .resourceConfig(asOpenShiftExtension().resources)
      .buildOutputKind(asOpenShiftExtension().getBuildOutputKind().getOrElse("ImageStreamTag"));
  }

  private OpenShiftExtension asOpenShiftExtension() {
    return (OpenShiftExtension) kubernetesExtension;
  }
}
