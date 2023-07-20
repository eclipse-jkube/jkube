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
package org.eclipse.jkube.kit.config.service;

import java.util.List;

import org.eclipse.jkube.kit.common.util.PluginServiceFactory;

public class BuildServiceManager {

  private static final String[] SERVICE_PATHS = new String[] {
      "META-INF/jkube/build-service"
  };
  private final List<BuildService> buildServices;

  public BuildServiceManager(JKubeServiceHub jKubeServiceHub) {
    buildServices = new PluginServiceFactory<>(jKubeServiceHub).createServiceObjects(SERVICE_PATHS);
  }

  /**
   * Returns the first applicable {@link BuildService} for the provided {@link JKubeServiceHub}.
   *
   * @return the first applicable BuildService or an {@link IllegalStateException} if none was found.
   */
  public BuildService resolveBuildService() {
    return buildServices.stream().filter(BuildService::isApplicable).findAny()
        .orElseThrow(() -> new IllegalStateException("No suitable Build Service was found for your current configuration"));
  }

}
