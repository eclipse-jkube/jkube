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
import java.util.Arrays;
import java.util.List;

import org.eclipse.jkube.kit.common.JkubeProject;
import org.eclipse.jkube.kit.common.util.JkubeProjectUtil;

/**
 * Detector for tomcat app servers.
 *
 * @author kameshs
 */
public class TomcatAppSeverHandler extends AbstractAppServerHandler {

    private static final String TOMCAT_GROUPID = "org.apache.tomcat.maven";

    public TomcatAppSeverHandler(JkubeProject project) {
        super("tomcat", project);
    }

    @Override
    public boolean isApplicable() {
        try {
            return hasOneOf("**/META-INF/context.xml") ||
                    JkubeProjectUtil.hasPlugin(project, TOMCAT_GROUPID, "tomcat6-maven-plugin") ||
                    JkubeProjectUtil.hasPlugin(project, TOMCAT_GROUPID, "tomcat7-maven-plugin");
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to scan output directory: ", exception);
        }
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
