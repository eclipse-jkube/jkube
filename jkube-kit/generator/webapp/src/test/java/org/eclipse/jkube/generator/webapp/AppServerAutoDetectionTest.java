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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author kameshs
 */

class AppServerAutoDetectionTest {

    @TempDir
    Path folder;

    @Test
    void isWildFly() throws Exception {
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
            "org.wildfly.plugins:wildfly-jar-maven-plugin", false,
        };

        assertAppServerDescriptorApplicability(descriptorNames);
        assertPluginApplicability(pluginNames);
    }

    @Test
    void isTomcat() throws Exception {
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
    void detect_withSpecifiedServer_shouldReturnSpecifiedServer() {
        GeneratorContext generatorContext = GeneratorContext.builder().project(JavaProject.builder().build()).build();

        AppServerHandler appServerHandler = new AppServerDetector(generatorContext).detect("tomcat");
        assertThat(appServerHandler.getName()).isEqualTo("tomcat");
    }


    @Test
    void isJetty() throws Exception {
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
    void detect_withDefaultServer_shouldReturnTomcat() throws IOException {
        File appDir = Files.createDirectory(folder.resolve("webapp")).toFile();
        GeneratorContext generatorContext = GeneratorContext.builder().project(JavaProject.builder()
            .buildDirectory(appDir)
            .plugins(Collections.singletonList(Plugin.builder()
                .groupId("org.apache.tomcat.maven")
                .artifactId("org.apache.tomcat.maven")
                .version("testversion")
                .configuration(Collections.emptyMap()).build()))
            .build()).build();

        AppServerHandler appServerHandler = new AppServerDetector(generatorContext).detect(null);
        assertThat(appServerHandler.getName()).isEqualTo("tomcat");
    }

    private void assertAppServerDescriptorApplicability(Object[] descriptors) throws IOException {
      for (int i = 0; i < descriptors.length; i += 2) {
        String[] descriptor = descriptors[i].toString().split("/");
        String appDir = descriptor[0];
        String file = descriptor[1];
        boolean expected = (boolean) descriptors[i + 1];

        Path webInf = Files.createDirectories(folder.resolve("webapp" + i).resolve(appDir));
        Files.createFile(webInf.resolve(file));

        GeneratorContext generatorContext = GeneratorContext.builder().project(JavaProject.builder()
            .buildDirectory(webInf.toFile())
            .plugins(Collections.emptyList())
            .build()).build();

        AppServerHandler appServerHandler = new AppServerDetector(generatorContext).detect(null);

        String message = String.format("Expected descriptor %s to make isApplicable() return %s", descriptor, expected);
        assertThat(appServerHandler.isApplicable()).as(message).isEqualTo(expected);
      }
    }

    private void assertPluginApplicability(Object[] plugins) {
      for (int i = 0; i < plugins.length; i += 2) {
        String[] pluginCoordinate = plugins[i].toString().split(":");
        String groupId = pluginCoordinate[0];
        String artifactId = pluginCoordinate[1];
        boolean expected = (boolean) plugins[i + 1];

        GeneratorContext generatorContext = GeneratorContext.builder().project(JavaProject.builder()
            .buildDirectory(folder.toFile())
            .plugins(Collections.singletonList(Plugin.builder()
                .groupId(groupId).artifactId(artifactId)
                .version("testversion")
                .configuration(Collections.emptyMap()).build()))
            .build()).build();

        AppServerHandler appServerHandler = new AppServerDetector(generatorContext).detect(null);

        String message = String.format("Expected plugin %s to make isApplicable() return %s", pluginCoordinate, expected);
        assertThat(appServerHandler.isApplicable()).as(message).isEqualTo(expected);
      }
    }
}
