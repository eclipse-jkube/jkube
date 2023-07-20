/*
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.stream.Stream;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.generator.webapp.handler.TomcatAppSeverHandler;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author kameshs
 */

class AppServerAutoDetectionTest {

    @TempDir
    Path folder;

  @ParameterizedTest(name = "{index}: with ''{1}'' descriptor file should return ''{0}'' handler")
  @MethodSource("descriptorFiles")
  void detect_withDescriptorFiles_shouldReturnApplicableHandler(String handlerName, Path relativeDescriptor) throws IOException {
    final Path descriptor = folder.resolve("webapp").resolve(relativeDescriptor);
    final Path xxxInf = descriptor.getParent();
    Files.createDirectories(descriptor.getParent());
    Files.createFile(descriptor);

    GeneratorContext generatorContext = GeneratorContext.builder().project(JavaProject.builder()
      .buildDirectory(xxxInf.toFile())
      .build()).build();

    AppServerHandler appServerHandler = new AppServerDetector(generatorContext).detect(null);
    assertThat(appServerHandler)
      .hasFieldOrPropertyWithValue("name", handlerName)
      .returns(true, AppServerHandler::isApplicable);
  }

  static Stream<Arguments> descriptorFiles() {
    return Stream.of(
        Arguments.arguments("wildfly", Paths.get("WEB-INF", "jboss-deployment-structure.xml")),
        Arguments.arguments("wildfly", Paths.get("META-INF", "jboss-deployment-structure.xml")),
        Arguments.arguments("wildfly", Paths.get("WEB-INF", "jboss-web.xml")),
        Arguments.arguments("wildfly", Paths.get("WEB-INF", "ejb-jar.xml")),
        Arguments.arguments("wildfly", Paths.get("WEB-INF", "jboss-ejb3.xml")),
        Arguments.arguments("wildfly", Paths.get("META-INF", "persistence.xml")),
        Arguments.arguments("wildfly", Paths.get("META-INF", "foo-jms.xml")),
        Arguments.arguments("wildfly", Paths.get("WEB-INF", "bar-jms.xml")),
        Arguments.arguments("wildfly", Paths.get("META-INF", "my-datasource-ds.xml")),
        Arguments.arguments("wildfly", Paths.get("WEB-INF", "my-datasource-ds.xml")),
        Arguments.arguments("wildfly", Paths.get("WEB-INF", "jboss-ejb-client.xml")),
        Arguments.arguments("wildfly", Paths.get("META-INF", "jbosscmp-jdbc.xml")),
        Arguments.arguments("wildfly", Paths.get("WEB-INF", "jboss-webservices.xml")),
        Arguments.arguments("tomcat", Paths.get("META-INF", "context.xml")),
        Arguments.arguments("jetty", Paths.get("META-INF", "jetty-logging.properties")),
        Arguments.arguments("jetty", Paths.get("WEB-INF", "jetty-web.xml"))
    );
  }

  @ParameterizedTest(name = "{index}: with ''{1}:{2}:1.0.0'' plugin should return ''{0}'' handler")
  @MethodSource("plugins")
  void detect_withPlugin_shouldReturnApplicableHandler(String handlerName, String groupId, String artifactId) {
    GeneratorContext generatorContext = GeneratorContext.builder().project(JavaProject.builder()
      .buildDirectory(folder.toFile())
      .plugins(Collections.singletonList(Plugin.builder()
        .groupId(groupId).artifactId(artifactId).version("1.0.0")
        .build()))
      .build()).build();
    AppServerHandler appServerHandler = new AppServerDetector(generatorContext).detect(null);
    assertThat(appServerHandler)
      .hasFieldOrPropertyWithValue("name", handlerName)
      .returns(true, AppServerHandler::isApplicable);
  }

  static Stream<Arguments> plugins() {
    return Stream.of(
      Arguments.arguments("wildfly", "org.jboss.as.plugins", "jboss-as-maven-plugin"),
      Arguments.arguments("wildfly", "org.wildfly.plugins", "wildfly-maven-plugin"),
      Arguments.arguments("tomcat", "org.apache.tomcat.maven", "tomcat6-maven-plugin"),
      Arguments.arguments("tomcat", "org.apache.tomcat.maven", "tomcat7-maven-plugin"),
      Arguments.arguments("jetty", "org.mortbay.jetty", "jetty-maven-plugin"),
      Arguments.arguments("jetty", "org.eclipse.jetty", "jetty-maven-plugin")
    );
  }

    @Test
    void detect_withSpecifiedServer_shouldReturnSpecifiedServer() {
        GeneratorContext generatorContext = GeneratorContext.builder().project(JavaProject.builder().build()).build();

        AppServerHandler appServerHandler = new AppServerDetector(generatorContext).detect("wildfly");
        assertThat(appServerHandler.getName()).isEqualTo("wildfly");
    }

    @Test
    void detect_withNotApplicableDescriptor_shouldReturnDefaultServer() throws IOException {
      final Path descriptor = folder.resolve("webapp").resolve("META-INF").resolve("not-valid-descriptor.xml");
      final Path xxxInf = descriptor.getParent();
      Files.createDirectories(descriptor.getParent());
      Files.createFile(descriptor);

      GeneratorContext generatorContext = GeneratorContext.builder().project(JavaProject.builder()
        .buildDirectory(xxxInf.toFile())
        .build()).build();

        AppServerHandler appServerHandler = new AppServerDetector(generatorContext).detect(null);
        assertThat(appServerHandler)
          .isInstanceOf(TomcatAppSeverHandler.class)
          .hasFieldOrPropertyWithValue("name", "tomcat")
          .returns(false, AppServerHandler::isApplicable);
    }

  @ParameterizedTest(name = "{index}: with ''{0}:{1}:1.0.0'' plugin should return tomcat handler")
  @MethodSource("notApplicablePlugins")
  void detect_withNotApplicablePlugin_shouldReturnDefaultServer(String groupId, String artifactId) {
    GeneratorContext generatorContext = GeneratorContext.builder().project(JavaProject.builder()
      .buildDirectory(folder.toFile())
      .plugins(Collections.singletonList(Plugin.builder()
        .groupId(groupId).artifactId(artifactId).version("1.0.0")
        .build()))
      .build()).build();
    AppServerHandler appServerHandler = new AppServerDetector(generatorContext).detect(null);
    assertThat(appServerHandler)
      .isInstanceOf(TomcatAppSeverHandler.class)
      .hasFieldOrPropertyWithValue("name", "tomcat")
      .returns(false, AppServerHandler::isApplicable);
  }

  static Stream<Arguments> notApplicablePlugins() {
    return Stream.of(
      Arguments.arguments("org.wildfly.swarm", "wildfly-swarm-plugin"),
      Arguments.arguments("io.thorntail", "thorntail-maven-plugin"),
      Arguments.arguments("org.wildfly.plugins", "wildfly-jar-maven-plugin")
    );
  }
}
