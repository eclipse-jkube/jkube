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
package org.eclipse.jkube.kit.build.service.docker.watch;

import java.io.Serializable;
import java.util.Date;

import org.eclipse.jkube.kit.build.core.GavLabel;
import org.eclipse.jkube.kit.build.service.docker.DockerServiceHub;
import org.eclipse.jkube.kit.build.service.docker.WatchService;
import org.eclipse.jkube.kit.build.service.docker.helper.Task;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.WatchMode;
import org.eclipse.jkube.kit.common.JKubeConfiguration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Context class to hold the watch configuration
 */
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@EqualsAndHashCode
public class WatchContext implements Serializable {

  private JKubeConfiguration buildContext;
  private WatchMode watchMode;
  private int watchInterval;
  private String watchPostExec;
  private GavLabel gavLabel;

  private transient Task<ImageConfiguration> imageCustomizer;
  private transient Task<WatchService.ImageWatcher> containerRestarter;
  private transient ExecTask containerCommandExecutor;
  private transient CopyFilesTask containerCopyTask;
  private transient DockerServiceHub hub;
  private transient Runnable postGoalTask;

  private Date buildTimestamp;

}
