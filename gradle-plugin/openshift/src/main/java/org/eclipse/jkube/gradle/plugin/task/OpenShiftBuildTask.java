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
package org.eclipse.jkube.gradle.plugin.task;

import javax.inject.Inject;

import org.eclipse.jkube.gradle.plugin.OpenShiftExtension;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;

public class OpenShiftBuildTask extends KubernetesBuildTask implements OpenShiftJKubeTask {

  @Inject
  public OpenShiftBuildTask(Class<? extends OpenShiftExtension> extensionClass) {
    super(extensionClass);
    setDescription(
      "Builds the container images configured for this project via a Docker, S2I binary build or any of the other available build strategies.");
  }

  @Override
  protected BuildServiceConfig.BuildServiceConfigBuilder buildServiceConfigBuilder() {
    return super.buildServiceConfigBuilder()
        .openshiftPullSecret(getOpenShiftExtension().getOpenshiftPullSecretOrDefault())
        .s2iBuildNameSuffix(getOpenShiftExtension().getS2iBuildNameSuffixOrDefault())
        .s2iImageStreamLookupPolicyLocal(getOpenShiftExtension().getS2iImageStreamLookupPolicyLocalOrDefault())
        .openshiftPushSecret(getOpenShiftExtension().getOpenshiftPushSecretOrDefault())
        .resourceConfig(getOpenShiftExtension().resources)
        .buildOutputKind(getOpenShiftExtension().getBuildOutputKindOrDefault())
        .enricherTask(e -> {
          enricherManager.enrich(PlatformMode.kubernetes, e);
          enricherManager.enrich(PlatformMode.openshift, e);
        });
  }
}
