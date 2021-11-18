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
package org.eclipse.jkube.maven.plugin.mojo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@RunWith(Parameterized.class)
public class OpenShiftGeneratedPluginDescriptorTest {
  private File pluginDescriptor;

  @Parameterized.Parameter
  public String mojo;

  @Parameterized.Parameter(1)
  public String expectedRequiresDependencyResolution;

  @Parameterized.Parameter(2)
  public String expectedPhase;

  @Before
  public void setUp() {
    URL pluginDescriptorUrl = getClass().getResource("/META-INF/maven/plugin.xml");
    assertThat(pluginDescriptorUrl).isNotNull();
    pluginDescriptor = new File(pluginDescriptorUrl.getFile());
  }

  @Parameterized.Parameters(name = "{index} {0}, should have {1} requiresDependencyResolution and {2} phase")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[] { "build", "compile", "pre-integration-test"},
        new Object[] { "resource", "compile", "process-resources"},
        new Object[] { "apply", "compile+runtime", "install"},
        new Object[] { "deploy", "compile+runtime", "validate"},
        new Object[] { "watch", "compile+runtime", "package"},
        new Object[] { "undeploy", "compile", "install"},
        new Object[] { "debug", "compile+runtime", "package"},
        new Object[] { "log", "compile+runtime", "validate"},
        new Object[] { "push", "compile", "install"},
        new Object[] { "helm", "", "pre-integration-test"},
        new Object[] { "helm-push", "compile", "install"}
    );
  }

  @Test
  public void verifyPhaseAndRequiresDependencyResolution() throws Exception {
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
