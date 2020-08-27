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
package org.eclipse.jkube.generator.webapp.handler;

import java.io.IOException;
import java.util.stream.Stream;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;

/**
 * Jetty handler
 *
 * @author kameshs
 */
public class JettyAppSeverHandler extends AbstractAppServerHandler {

  private static final String JETTY_MAVEN_PLUGIN_ARTIFACT_ID = "jetty-maven-plugin";

  public JettyAppSeverHandler(GeneratorContext context) {
    super("jetty", context);
  }

  @Override
  public boolean isApplicable() {
    try {
      return isJettyWebApp() || hasJettyMavenPlugin();
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to scan output directory: ", exception);
    }
  }

  private boolean isJettyWebApp() throws IOException {
    return hasOneOf(
        "glob:**/WEB-INF/jetty-web.xml",
        "glob:**/META-INF/jetty-logging.properties"
    );
  }

  private boolean hasJettyMavenPlugin() {
    return Stream.of("org.mortbay.jetty", "org.eclipse.jetty")
        .anyMatch(groupId -> JKubeProjectUtil.hasPlugin(getProject(), groupId, JETTY_MAVEN_PLUGIN_ARTIFACT_ID));
  }

  @Override
  public String getFrom() {
    return imageLookup.getImageName("jetty.upstream.docker");
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
}