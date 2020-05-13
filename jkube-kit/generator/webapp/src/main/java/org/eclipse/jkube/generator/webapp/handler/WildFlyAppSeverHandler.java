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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.jkube.generator.api.FromSelector;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;
import org.eclipse.jkube.kit.config.image.build.OpenShiftBuildStrategy;
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
    this.fromSelector = new FromSelector.NoRedHatSupportFromSelector(context, PROPERTY_IMAGE_NAME);
  }

  @Override
  public boolean isApplicable() {
    try {
      return isNotWildflySwarm() && isNotThorntail() &&
          (hasOneOf("**/WEB-INF/jboss-deployment-structure.xml",
              "**/META-INF/jboss-deployment-structure.xml",
              "**/WEB-INF/jboss-web.xml", "**/WEB-INF/ejb-jar.xml",
              "**/WEB-INF/jboss-ejb3.xml", "**/META-INF/persistence.xml",
              "**/META-INF/*-jms.xml", "**/WEB-INF/*-jms.xml",
              "**/META-INF/*-ds.xml", "**/WEB-INF/*-ds.xml",
              "**/WEB-INF/jboss-ejb-client.xml", "**/META-INF/jbosscmp-jdbc.xml",
              "**/WEB-INF/jboss-webservices.xml") ||
              JKubeProjectUtil.hasPlugin(getProject(), "org.jboss.as.plugins", "jboss-as-maven-plugin") ||
              JKubeProjectUtil.hasPlugin(getProject(), "org.wildfly.plugins", "wildfly-maven-plugin"));
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to scan output directory: ", exception);
    }
  }

  private boolean isNotWildflySwarm() {
    return !JKubeProjectUtil.hasPlugin(getProject(), "org.wildfly.swarm", "wildfly-swarm-plugin");
  }

  private boolean isNotThorntail() {
    return !JKubeProjectUtil.hasPlugin(getProject(), "io.thorntail", "thorntail-maven-plugin");
  }

  @Override
  public String getFrom() {
    return fromSelector.getFrom();
  }

  @Override
  public List<String> exposedPorts() {
    return Collections.singletonList("8080");
  }

  @Override
  public String getDeploymentDir() {
    // Applicable for Docker image - ignored for s2i
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
  public String getAssemblyName() {
    return "deployments";
  }

  @Override
  public String getUser() {
    return "jboss:jboss:jboss";
  }

  @Override
  public List<String> runCmds() {
    if (generatorContext.getRuntimeMode() == RuntimeMode.openshift
        && generatorContext.getStrategy() == OpenShiftBuildStrategy.docker) {
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
