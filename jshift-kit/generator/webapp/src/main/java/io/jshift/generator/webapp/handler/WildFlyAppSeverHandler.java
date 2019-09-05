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
 * Handler for wildfly
 *
 * @author kameshs
 */
public class WildFlyAppSeverHandler extends AbstractAppServerHandler {

    public WildFlyAppSeverHandler(MavenProject project) {
        super("wildfly", project);
    }

    @Override
    public boolean isApplicable() {
        return
            !MavenUtil.hasPlugin(project, "org.wildfly.swarm", "wildfly-swarm-plugin") &&
            !MavenUtil.hasPlugin(project, "io.thorntail", "thorntail-maven-plugin") &&
            (hasOneOf("**/WEB-INF/jboss-deployment-structure.xml",
                     "**/META-INF/jboss-deployment-structure.xml",
                     "**/WEB-INF/jboss-web.xml", "**/WEB-INF/ejb-jar.xml",
                     "**/WEB-INF/jboss-ejb3.xml", "**/META-INF/persistence.xml",
                     "**/META-INF/*-jms.xml", "**/WEB-INF/*-jms.xml",
                     "**/META-INF/*-ds.xml", "**/WEB-INF/*-ds.xml",
                     "**/WEB-INF/jboss-ejb-client.xml", "**/META-INF/jbosscmp-jdbc.xml",
                     "**/WEB-INF/jboss-webservices.xml") ||
            MavenUtil.hasPlugin(project, "org.jboss.as.plugins", "jboss-as-maven-plugin") ||
            MavenUtil.hasPlugin(project, "org.wildfly.plugins", "wildfly-maven-plugin"));
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