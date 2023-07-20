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
import java.util.List;
import java.util.Map;

import org.eclipse.jkube.generator.api.FromSelector;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;

/**
 * Handler for wildfly
 *
 * @author kameshs
 */
public class WildFlyAppSeverHandler extends AbstractAppServerHandler {

  private static final String HANDLER_NAME = "wildfly";
  private static final String PROPERTY_IMAGE_NAME = HANDLER_NAME;

  private final FromSelector fromSelector;

  public WildFlyAppSeverHandler(GeneratorContext context) {
    super(HANDLER_NAME, context);
    this.fromSelector = new FromSelector.Default(context, PROPERTY_IMAGE_NAME);
  }

  @Override
  public boolean isApplicable() {
    try {
      return isNotWildflySwarm() && isNotThorntail() && (isWildFlyWebApp() || hasWildFlyPlugin()) && isNotWildFlyJAR();
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to scan output directory: ", exception);
    }
  }

  private boolean hasWildFlyPlugin() {
    return JKubeProjectUtil.hasPlugin(getProject(), "org.jboss.as.plugins", "jboss-as-maven-plugin") ||
        JKubeProjectUtil.hasPlugin(getProject(), "org.wildfly.plugins", "wildfly-maven-plugin");
  }

  private boolean isWildFlyWebApp() throws IOException {
    return hasOneOf("glob:**/WEB-INF/jboss-deployment-structure.xml",
        "glob:**/META-INF/jboss-deployment-structure.xml",
        "glob:**/WEB-INF/jboss-web.xml",
        "glob:**/WEB-INF/ejb-jar.xml",
        "glob:**/WEB-INF/jboss-ejb3.xml",
        "glob:**/META-INF/persistence.xml",
        "glob:**/META-INF/*-jms.xml",
        "glob:**/WEB-INF/*-jms.xml",
        "glob:**/META-INF/*-ds.xml",
        "glob:**/WEB-INF/*-ds.xml",
        "glob:**/WEB-INF/jboss-ejb-client.xml",
        "glob:**/META-INF/jbosscmp-jdbc.xml",
        "glob:**/WEB-INF/jboss-webservices.xml");
  }

  private boolean isNotWildflySwarm() {
    return !JKubeProjectUtil.hasPlugin(getProject(), "org.wildfly.swarm", "wildfly-swarm-plugin");
  }

  private boolean isNotThorntail() {
    return !JKubeProjectUtil.hasPlugin(getProject(), "io.thorntail", "thorntail-maven-plugin");
  }
  
  private boolean isNotWildFlyJAR() {
    return !JKubeProjectUtil.hasPlugin(getProject(), "org.wildfly.plugins", "wildfly-jar-maven-plugin");
  }

  @Override
  public String getFrom() {
    return fromSelector.getFrom();
  }

  @Override
  public String getDeploymentDir() {
    if (generatorContext.getRuntimeMode() == RuntimeMode.OPENSHIFT
        && generatorContext.getStrategy() == JKubeBuildStrategy.s2i) {
      return "/deployments";
    }
    return "/opt/jboss/wildfly/standalone/deployments";
  }

  @Override
  public String getCommand() {
    // Applicable for Docker image - ignored for s2i
    return "/opt/jboss/wildfly/bin/standalone.sh -b 0.0.0.0";
  }

  @Override
  public Map<String, String> getEnv() {
    // Applicable for s2i image - ignored for Docker
    return Collections.singletonMap("GALLEON_PROVISION_LAYERS", "cloud-server,web-clustering");
  }


  @Override
  public String getUser() {
    return "jboss:jboss:jboss";
  }

  @Override
  public List<String> runCmds() {
    if (generatorContext.getRuntimeMode() == RuntimeMode.OPENSHIFT
        && generatorContext.getStrategy() == JKubeBuildStrategy.docker) {
      // OpenShift runs pods in a restricted security context (SCC) which randomizes the user.
      // Make required runtime directories writeable for all users
      return Collections.singletonList(
          "chmod -R a+rw /opt/jboss/wildfly/standalone/");
    }
    return super.runCmds();
  }

  @Override
  public boolean supportsS2iBuild() {
    return true;
  }
}
