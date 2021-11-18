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

import java.util.Collection;

import org.eclipse.jkube.maven.plugin.mojo.build.ApplyMojo;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Ensures that the current app has debug enabled, then opens the debug port so that you can debug the latest pod
 * from your IDE
 */
@Mojo(name = "debug", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.PACKAGE)
public class DebugMojo extends ApplyMojo {

  @Parameter(property = "jkube.debug.port", defaultValue = "5005")
  private String localDebugPort;

  @Parameter(property = "jkube.debug.suspend", defaultValue = "false")
  private boolean debugSuspend;

  @Override
  protected void applyEntities(KubernetesClient kubernetes, String fileName, Collection<HasMetadata> entities) {
    jkubeServiceHub.getDebugService().debug(
        applyService.getNamespace(), fileName, entities, localDebugPort, debugSuspend, createLogger("[[Y]][W][[Y]] [[s]]"));
  }

}

