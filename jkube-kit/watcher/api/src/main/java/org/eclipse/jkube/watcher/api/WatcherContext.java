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
package org.eclipse.jkube.watcher.api;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.eclipse.jkube.kit.build.service.docker.watch.WatchContext;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;

/**
 * @author nicola
 */
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@EqualsAndHashCode
public class WatcherContext {

  private ProcessorConfig config;
  private KitLogger logger;
  private KitLogger newPodLogger;
  private KitLogger oldPodLogger;
  private boolean useProjectClasspath;
  private WatchContext watchContext;
  private JKubeConfiguration buildContext;
  private JKubeServiceHub jKubeServiceHub;

  public String getNamespace() {
    return getJKubeServiceHub().getClusterAccess().getNamespace();
  }
}
