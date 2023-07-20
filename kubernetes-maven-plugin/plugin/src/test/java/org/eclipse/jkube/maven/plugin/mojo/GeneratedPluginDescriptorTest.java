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
package org.eclipse.jkube.maven.plugin.mojo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class GeneratedPluginDescriptorTest {
  private File pluginDescriptor;

  @BeforeEach
  void setUp() {
    URL pluginDescriptorUrl = getClass().getResource("/META-INF/maven/plugin.xml");
    assertThat(pluginDescriptorUrl).isNotNull();
    pluginDescriptor = new File(pluginDescriptorUrl.getFile());
  }

  static Stream<Arguments> data() {
    return Stream.of(
        arguments("build", "compile", "pre-integration-test"),
        arguments("resource", "compile", "process-resources"),
        arguments("apply", "compile+runtime", "install"),
        arguments("deploy", "compile+runtime", "validate"),
        arguments("watch", "compile+runtime", "package"),
        arguments("undeploy", "compile", "install"),
        arguments("debug", "compile+runtime", "package"),
        arguments("log", "compile+runtime", "validate"),
        arguments("push", "compile", "install"),
        arguments("helm", "", "pre-integration-test"),
        arguments("helm-push", "compile", "install"));
  }

  @DisplayName("verify, phase and required dependency resolution")
  @ParameterizedTest(name = "{index}: {0}, should have {1} requiresDependencyResolution and {2} phase")
  @MethodSource("data")
  void verifyPhaseAndRequiresDependencyResolution(String mojo, String expectedRequiresDependencyResolution, String expectedPhase) throws Exception {
    assertThat(getField(pluginDescriptor, "/plugin/mojos/mojo[goal='" + mojo + "']/requiresDependencyResolution"))
        .isEqualTo(expectedRequiresDependencyResolution);
    assertThat(getField(pluginDescriptor, "/plugin/mojos/mojo[goal='" + mojo + "']/phase"))
        .isEqualTo(expectedPhase);
  }

  private String getField(File xmlFile, String expression) throws Exception {
    final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    final DocumentBuilder builder = factory.newDocumentBuilder();
    try (final FileInputStream fis = new FileInputStream(xmlFile)) {
      final Document pom = builder.parse(fis);
      final XPath xPath = XPathFactory.newInstance().newXPath();
      return xPath.compile(expression).evaluate(pom);
    }
  }
}
