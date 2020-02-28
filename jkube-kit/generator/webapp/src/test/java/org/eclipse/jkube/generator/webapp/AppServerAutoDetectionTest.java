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
package org.eclipse.jkube.generator.webapp;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Collections;

import org.eclipse.jkube.kit.common.JKubeProject;
import org.eclipse.jkube.kit.common.JKubeProjectPlugin;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;

/**
 * @author kameshs
 */
public class AppServerAutoDetectionTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testIsWildFly() throws Exception {
        Object[] descriptorNames = new Object[] {
            "WEB-INF/jboss-deployment-structure.xml", true,
            "META-INF/jboss-deployment-structure.xml", true,
            "WEB-INF/jboss-web.xml", true,
            "WEB-INF/ejb-jar.xml", true,
            "WEB-INF/jboss-ejb3.xml", true,
            "META-INF/persistence.xml", true,
            "META-INF/foo-jms.xml", true,
            "WEB-INF/bar-jms.xml", true,
            "META-INF/my-datasource-ds.xml", true,
            "WEB-INF/my-datasource-ds.xml", true,
            "WEB-INF/jboss-ejb-client.xml", true,
            "META-INF/jbosscmp-jdbc.xml", true,
            "WEB-INF/jboss-webservices.xml", true,
            "META-INF/non-existent-descriptor.xml", false,
        };

        Object[] pluginNames = new Object[] {
            "org.jboss.as.plugins:jboss-as-maven-plugin", true,
            "org.wildfly.plugins:wildfly-maven-plugin", true,
            "org.wildfly.swarm:wildfly-swarm-plugin", false,
            "io.thorntail:thorntail-maven-plugin", false,
        };

        assertAppServerDescriptorApplicability(descriptorNames);
        assertPluginApplicability(pluginNames);
    }

    @Test
    public void testIsTomcat() throws Exception {
        Object[] descriptorNames = new Object[] {
            "META-INF/context.xml", true,
            "META-INF/non-existent-descriptor.xml", false,
        };

        Object[] pluginNames = new Object[] {
            "org.apache.tomcat.maven:tomcat6-maven-plugin", true,
            "org.apache.tomcat.maven:tomcat7-maven-plugin", true,
        };

        assertAppServerDescriptorApplicability(descriptorNames);
        assertPluginApplicability(pluginNames);
    }

    @Test
    public void testWithSpecifiedServer() throws Exception {
        JKubeProject jkubeProject = new JKubeProject.Builder()
                .build();

        AppServerHandler appServerHandler = new AppServerDetector(jkubeProject).detect("tomcat");
        assertEquals("tomcat", appServerHandler.getName());
    }


    @Test
    public void testIsJetty() throws Exception {
        Object[] descriptorNames = new Object[] {
            "META-INF/jetty-logging.properties", true,
            "WEB-INF/jetty-web.xml", true,
            "META-INF/non-existent-descriptor.xml", false,
        };

        Object[] pluginNames = new Object[] {
            "org.mortbay.jetty:jetty-maven-plugin", true,
            "org.eclipse.jetty:jetty-maven-plugin", true,
        };

        assertAppServerDescriptorApplicability(descriptorNames);
        assertPluginApplicability(pluginNames);
    }

    @Test
    public void testDefaultServer() throws IOException {
        File appDir = folder.newFolder("webapp");

        JKubeProject jkubeProject = new JKubeProject.Builder()
                .buildDirectory(appDir.getAbsolutePath())
                .plugins(Collections.singletonList(new JKubeProjectPlugin.Builder().groupId("org.apache.tomcat.maven").artifactId("org.apache.tomcat.maven").version("testversion").configuration(Collections.emptyMap()).build()))
                .build();
        AppServerHandler appServerHandler = new AppServerDetector(jkubeProject).detect(null);
        assertEquals("tomcat", appServerHandler.getName());
    }

    private void assertAppServerDescriptorApplicability(Object[] descriptors) throws IOException {
        for (int i = 0; i < descriptors.length; i += 2) {
            String descriptor = (String) descriptors[i];
            boolean expected = (boolean) descriptors[i + 1];

            File appDir = folder.newFolder("webapp" + i);
            new File(appDir, "META-INF/").mkdirs();
            new File(appDir, "WEB-INF/").mkdirs();
            new File(appDir, descriptor).createNewFile();

            JKubeProject jkubeProject = new JKubeProject.Builder()
                    .buildDirectory(appDir.getPath())
                    .plugins(Collections.emptyList())
                    .build();
            AppServerHandler appServerHandler = new AppServerDetector(jkubeProject).detect(null);

            String message = String.format("Expected descriptor %s to make isApplicable() return %s", descriptor, expected);
            assertEquals(message, expected, appServerHandler.isApplicable());
        }
    }

    private void assertPluginApplicability(Object[] plugins) {
        for (int i = 0; i < plugins.length; i += 2) {
            String pluginCoordinate = (String) plugins[i];
            String groupId = pluginCoordinate.split(":")[0];
            String artifactId = pluginCoordinate.split(":")[1];
            boolean expected = (boolean) plugins[i + 1];

            JKubeProject jkubeProject = new JKubeProject.Builder()
                    .buildDirectory(folder.getRoot().getPath())
                    .plugins(Collections.singletonList(new JKubeProjectPlugin.Builder().groupId(groupId).artifactId(artifactId).version("testversion").configuration(Collections.emptyMap()).build()))
                    .build();
            AppServerHandler appServerHandler = new AppServerDetector(jkubeProject).detect(null);

            String message = String.format("Expected plugin %s to make isApplicable() return %s", pluginCoordinate, expected);
            assertEquals(message, expected, appServerHandler.isApplicable());
        }
    }
}
