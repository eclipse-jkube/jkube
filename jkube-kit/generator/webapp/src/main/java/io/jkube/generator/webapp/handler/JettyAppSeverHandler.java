/**
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
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