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
 * Detector for tomat app servers.
 *
 * @author kameshs
 */
public class TomcatAppSeverHandler extends AbstractAppServerHandler {

    public TomcatAppSeverHandler(MavenProject project) {
        super("tomcat", project);
    }

    @Override
    public boolean isApplicable() {
        return hasOneOf("**/META-INF/context.xml") ||
                MavenUtil.hasPlugin(project, "org.apache.tomcat.maven", "tomcat6-maven-plugin") ||
                MavenUtil.hasPlugin(project, "org.apache.tomcat.maven", "tomcat7-maven-plugin");
    }

    @Override
    public String getFrom() {
        return imageLookup.getImageName("tomcat.upstream.docker");
    }

    @Override
    public List<String> exposedPorts() {
        return Arrays.asList("8080", "8778");
    }

    @Override
    public String getDeploymentDir() {
        return "/deployments";
    }

    @Override
    public String getCommand() {
        return "/opt/tomcat/bin/deploy-and-run.sh";
    }

    @Override
    public String getUser() {
        return "jboss:jboss:jboss";
    }
}
