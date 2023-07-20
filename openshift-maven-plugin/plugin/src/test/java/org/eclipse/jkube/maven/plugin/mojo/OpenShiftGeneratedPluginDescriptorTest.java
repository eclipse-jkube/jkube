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

class OpenShiftGeneratedPluginDescriptorTest {
  private File pluginDescriptor;

  @BeforeEach
  void setUp() {
    URL pluginDescriptorUrl = getClass().getResource("/META-INF/maven/plugin.xml");
    assertThat(pluginDescriptorUrl).isNotNull();
    pluginDescriptor = new File(pluginDescriptorUrl.getFile());
  }

  public static Stream<Arguments> data() {
    return Stream.of(
        Arguments.of("build", "compile", "pre-integration-test"),
        Arguments.of("resource", "compile", "process-resources"),
        Arguments.of("apply", "compile+runtime", "install"),
        Arguments.of("deploy", "compile+runtime", "validate"),
        Arguments.of("watch", "compile+runtime", "package"),
        Arguments.of("undeploy", "compile", "install"),
        Arguments.of("debug", "compile+runtime", "package"),
        Arguments.of("log", "compile+runtime", "validate"),
        Arguments.of("push", "compile", "install"),
        Arguments.of("helm", "", "pre-integration-test"),
        Arguments.of("helm-push", "compile", "install")
    );
  }

  @ParameterizedTest(name = "{0}, should have {1} requiresDependencyResolution and {2} phase")
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
