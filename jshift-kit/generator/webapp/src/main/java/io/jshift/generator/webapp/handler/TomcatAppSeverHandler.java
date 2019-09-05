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
package io.jshift.generator.webapp.handler;

import java.util.Arrays;
import java.util.List;

import io.jshift.kit.common.util.MavenUtil;
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
