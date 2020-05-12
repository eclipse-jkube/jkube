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

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;

/**
 * Jetty handler
 *
 * @author kameshs
 */
public class JettyAppSeverHandler extends AbstractAppServerHandler {


    public JettyAppSeverHandler(GeneratorContext context) {
        super("jetty", context);
    }

    @Override
    public boolean isApplicable() {
        try {
            return hasOneOf("**/WEB-INF/jetty-web.xml",
                    "**/META-INF/jetty-logging.properties") ||
                    JKubeProjectUtil.hasPlugin(getProject(), "org.mortbay.jetty", "jetty-maven-plugin") ||
                    JKubeProjectUtil.hasPlugin(getProject(), "org.eclipse.jetty", "jetty-maven-plugin");
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to scan output directory: ", exception);
        }
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