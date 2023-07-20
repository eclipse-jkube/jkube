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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.eclipse.jkube.kit.common.util.AnsiLogger;
import org.eclipse.jkube.kit.remotedev.RemoteDevelopmentConfig;
import org.eclipse.jkube.kit.remotedev.RemoteDevelopmentService;
import org.eclipse.jkube.maven.plugin.mojo.build.AbstractJKubeMojo;
import org.fusesource.jansi.AnsiConsole;

import java.util.concurrent.ExecutionException;

@Mojo(name = "remote-dev", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.VALIDATE)
public class RemoteDevMojo extends AbstractJKubeMojo {

  @Parameter
  protected RemoteDevelopmentConfig remoteDevelopment;

  @Override
  public void executeInternal() throws MojoExecutionException, MojoFailureException {
    final RemoteDevelopmentService remoteDevelopmentService =
      new RemoteDevelopmentService(jkubeServiceHub.getLog(), jkubeServiceHub.getClient(), remoteDevelopment);
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      if (jkubeServiceHub.getLog() instanceof AnsiLogger) {
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
