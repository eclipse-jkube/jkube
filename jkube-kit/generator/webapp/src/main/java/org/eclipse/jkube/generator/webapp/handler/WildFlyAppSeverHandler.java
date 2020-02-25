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

import java.util.Arrays;
import java.util.List;

import org.eclipse.jkube.kit.common.JkubeProject;
import org.eclipse.jkube.kit.common.util.JkubeProjectUtil;

/**
 * Handler for wildfly
 *
 * @author kameshs
 */
public class WildFlyAppSeverHandler extends AbstractAppServerHandler {

    public WildFlyAppSeverHandler(JkubeProject project) {
        super("wildfly", project);
    }

    @Override
    public boolean isApplicable() {
        return
            !JkubeProjectUtil.hasPlugin(project, "org.wildfly.swarm", "wildfly-swarm-plugin") &&
            !JkubeProjectUtil.hasPlugin(project, "io.thorntail", "thorntail-maven-plugin") &&
            (hasOneOf("**/WEB-INF/jboss-deployment-structure.xml",
                     "**/META-INF/jboss-deployment-structure.xml",
                     "**/WEB-INF/jboss-web.xml", "**/WEB-INF/ejb-jar.xml",
                     "**/WEB-INF/jboss-ejb3.xml", "**/META-INF/persistence.xml",
                     "**/META-INF/*-jms.xml", "**/WEB-INF/*-jms.xml",
                     "**/META-INF/*-ds.xml", "**/WEB-INF/*-ds.xml",
                     "**/WEB-INF/jboss-ejb-client.xml", "**/META-INF/jbosscmp-jdbc.xml",
                     "**/WEB-INF/jboss-webservices.xml") ||
            JkubeProjectUtil.hasPlugin(project, "org.jboss.as.plugins", "jboss-as-maven-plugin") ||
            JkubeProjectUtil.hasPlugin(project, "org.wildfly.plugins", "wildfly-maven-plugin"));
    }

    @Override
    public String getFrom() {
        return imageLookup.getImageName("wildfly.upstream.docker");
    }

    @Override
    public List<String> exposedPorts() {
        return Arrays.asList("8080");
    }

    @Override
    public String getDeploymentDir() {
        return "/opt/jboss/wildfly/standalone/deployments";
    }

    @Override
    public String getCommand() {
        return null;
    }

    @Override
    public String getUser() {
        return "jboss:jboss:jboss";
    }
}