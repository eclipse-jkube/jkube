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

import org.eclipse.jkube.gradle.plugin.KubernetesExtension;
import org.eclipse.jkube.kit.common.util.AnsiLogger;
import org.eclipse.jkube.kit.remotedev.RemoteDevelopmentService;
import org.fusesource.jansi.AnsiConsole;

import javax.inject.Inject;
import java.util.concurrent.ExecutionException;

public class KubernetesRemoteDevTask extends AbstractJKubeTask {

  @Inject
  public KubernetesRemoteDevTask(Class<? extends KubernetesExtension> extensionClass) {
    super(extensionClass);
    setDescription("Starts a new JKube remote development session.");
  }

  @Override
  public void run() {
    final RemoteDevelopmentService remoteDevelopmentService = new RemoteDevelopmentService(
      jKubeServiceHub.getLog(), jKubeServiceHub.getClient(), kubernetesExtension.remoteDevelopment);
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      if (jKubeServiceHub.getLog() instanceof AnsiLogger) {
        // Perform uninstall before Maven does to avoid race conditions and messages being logged as Ansi to a closed
        // AnsiConsole
        AnsiConsole.systemUninstall();
      }
      remoteDevelopmentService.stop();
    }));
    try {
      remoteDevelopmentService.start().get();
    } catch (ExecutionException e) {
      remoteDevelopmentService.stop();
    } catch (InterruptedException e) {
      remoteDevelopmentService.stop();
      Thread.currentThread().interrupt();
    }
  }
}
