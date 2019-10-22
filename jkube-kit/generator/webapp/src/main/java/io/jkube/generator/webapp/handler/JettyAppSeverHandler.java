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
package io.jkube.generator.webapp.handler;

import java.util.Arrays;
import java.util.List;

import io.jkube.kit.common.util.MavenUtil;
import org.apache.maven.project.MavenProject;

/**
 * Jetty handler
 *
 * @author kameshs
 */
public class JettyAppSeverHandler extends AbstractAppServerHandler {


    public JettyAppSeverHandler(MavenProject mavenProject) {
        super("jetty", mavenProject);
    }

    @Override
    public boolean isApplicable() {
        return hasOneOf("**/WEB-INF/jetty-web.xml",
                        "**/META-INF/jetty-logging.properties") ||
               MavenUtil.hasPlugin(project, "org.mortbay.jetty", "jetty-maven-plugin") ||
               MavenUtil.hasPlugin(project, "org.eclipse.jetty", "jetty-maven-plugin");
    }

    @Override
    public String getFrom() {
        return imageLookup.getImageName("jetty.upstream.docker");
    }

    @Override
    public List<String> exposedPorts() {
        return Arrays.asList("8080","8778");
    }

    @Override
    public String getDeploymentDir() {
        return "/deployments";
    }

    @Override
    public String getCommand() {
        return "/opt/jetty/bin/deploy-and-run.sh";
    }

    @Override
    public String getUser() {
        return "jboss:jboss:jboss";
    }
}