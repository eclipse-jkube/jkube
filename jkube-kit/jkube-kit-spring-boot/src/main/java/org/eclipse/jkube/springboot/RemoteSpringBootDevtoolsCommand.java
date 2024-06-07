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
package org.eclipse.jkube.springboot;

import org.eclipse.jkube.kit.common.ExternalCommand;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.PrefixedLogger;

import static org.eclipse.jkube.kit.common.util.EnvUtil.javaBinary;

public class RemoteSpringBootDevtoolsCommand extends ExternalCommand {
  private final String classPath;
  private final String remoteSecret;
  private final String url;
  private final KitLogger processLogger;

  public RemoteSpringBootDevtoolsCommand(String classPath, String remoteSecret, String url, KitLogger kitLogger) {
    super(kitLogger);
    this.classPath = classPath;
    this.remoteSecret = remoteSecret;
    this.url = url;
    this.processLogger = new PrefixedLogger("Spring-Remote", kitLogger);
  }

  @Override
  protected void start() {
    log.debug("Running: " + String.join(" ", getCommandAsString()));
  }

  @Override
  protected String[] getArgs() {
    return new String[] {
      javaBinary(),
      "-cp",
      classPath,
      "-Dspring.devtools.remote.secret=" + remoteSecret,
      "org.springframework.boot.devtools.RemoteSpringApplication",
      url
    };
  }

  @Override
  protected void processLine(String line) {
    processLogger.info("%s", line);
  }

  @Override
  protected void processError(String line) {
    processLogger.error("%s", line);
  }

  @Override
  protected void end() {
    if (getStatusCode() != 0) {
      log.warn("Process returned status: %s", getStatusCode());
    }
    log.info("Terminating the Spring remote client...");
  }
}
