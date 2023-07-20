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
package org.eclipse.jkube.generator.webapp.handler;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;

/**
 * Detector for tomcat app servers.
 *
 * @author kameshs
 */
public class TomcatAppSeverHandler extends AbstractAppServerHandler {

  private static final String TOMCAT_GROUPID = "org.apache.tomcat.maven";

  public TomcatAppSeverHandler(GeneratorContext context) {
    super("tomcat", context);
  }

  @Override
  public boolean isApplicable() {
    try {
      return hasOneOf("glob:**/META-INF/context.xml") || hasTomcatMavenPlugin();
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to scan output directory: ", exception);
    }
  }

  private boolean hasTomcatMavenPlugin() {
    return Stream.of("tomcat6-maven-plugin", "tomcat7-maven-plugin", "tomcat8-maven-plugin")
        .anyMatch(artifactId -> JKubeProjectUtil.hasPlugin(getProject(), TOMCAT_GROUPID, artifactId));
  }

  @Override
  public String getFrom() {
    return imageLookup.getImageName("tomcat.upstream.docker");
  }

  @Override
  public String getDeploymentDir() {
    return "/deployments";
  }

  @Override
  public String getCommand() {
    return "/usr/local/s2i/run";
  }

  @Override
  public boolean supportsS2iBuild() {
    return true;
  }

  @Override
  public Map<String, String> getEnv() {
    // Setting Tomcat webapps dir to `webapps-javaee` by default for
    // retrocompatibility.
    // If the project is already JakartaEE compliant, user should override
    // `jkube.generator.webapp.env` to `TOMCAT_WEBAPPS_DIR=webapps` for a faster
    // startup.
    return Collections.singletonMap("TOMCAT_WEBAPPS_DIR", "webapps-javaee");
  }
}
