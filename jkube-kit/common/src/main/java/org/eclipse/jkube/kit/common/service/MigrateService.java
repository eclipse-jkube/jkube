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
package org.eclipse.jkube.kit.common.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.function.BiConsumer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.io.FileUtils;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.XMLUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import static org.eclipse.jkube.kit.common.util.XMLUtil.stream;

public class MigrateService {
  private static final String POM_XML = "pom.xml";
  private static final String JKUBE = "jkube";
  private static final String FABRIC8 = "fabric8";
  private static final String PREFIX_FMP = "fmp-";
  private static final String PREFIX_F8 = "f8-";
  private static final String PREFIX_JKUBE = "jkube-";
  private static final String DEFAULT_FABRIC8_RESOURCE_FRAGMENT_DIRECTORY = "src/main/fabric8";
  private static final String DEFAULT_RESOURCE_FRAGMENT_DIRECTORY = "src/main/jkube";
  private static final String ARTIFACT_ID = "artifactId";
  private static final String GROUP_ID = "groupId";
  private static final String VERSION = "version";
  private static final String FABRIC8_MAVEN_PLUGIN_ARTIFACT_ID = "fabric8-maven-plugin";
  private File projectBasedir;
  private KitLogger logger;

  public MigrateService(File projectBaseDirectory, KitLogger logger) {
    this.projectBasedir = projectBaseDirectory;
    this.logger = logger;
  }

  public void migrate(String pluginGroupId, String pluginArtifactId, String pluginVersion) throws IOException, ParserConfigurationException, SAXException, TransformerException, XPathExpressionException {
    final File pomFile = findPomFile();
    final Document pom = XMLUtil.readXML(pomFile);
    if (pomContainsFMP(pom)) {
      replaceFabric8Properties(pom);
      replaceFabric8Plugin(pom, pluginGroupId, pluginArtifactId, pluginVersion);
      XMLUtil.writeXML(pom, pomFile);
      renameResourceFragmentDirectoryToJKube();
    } else {
      logger.warn("Unable to find Fabric8 Maven Plugin inside pom.xml ({})", pomFile.getAbsolutePath());
    }
  }

  private boolean pomContainsFMP(Document document) throws XPathExpressionException {
    return XMLUtil.evaluateExpressionForItem(document,
        String.format("//plugins/plugin[artifactId='%s']", FABRIC8_MAVEN_PLUGIN_ARTIFACT_ID)).getLength() > 0;
  }

  private File findPomFile() throws FileNotFoundException {
    final File pomFile = new File(projectBasedir, POM_XML);
    if (pomFile.exists() && pomFile.isFile()){
      return pomFile;
    }
    throw new FileNotFoundException("Project pom.xml was not found, check a pom.xml file exists in your project root");
  }

  private File getFabric8ResourceFragmentDirectory(File projectBasedir) {
    return new File(projectBasedir, DEFAULT_FABRIC8_RESOURCE_FRAGMENT_DIRECTORY);
  }

  @SuppressWarnings("squid:S3864")
  private void replaceFabric8Plugin(
      Document document, String groupId, String artifactId, String version) throws XPathExpressionException {

    final NodeList nodeList = XMLUtil.evaluateExpressionForItem(document,
        String.format("/project//build//plugins/plugin[artifactId='%s']", FABRIC8_MAVEN_PLUGIN_ARTIFACT_ID));
    stream(nodeList)
        .map(Element.class::cast)
        .peek(e -> stream(e.getElementsByTagName(VERSION))
            .forEach(v -> logger.info("Found Fabric8 Maven Plugin in pom with version %s", v.getTextContent())))
        .peek(this::replaceConfigurationTagNames)
        .peek(this::replaceConfigurationIncludesExcludes)
        .map(MigrateService::replaceChildNodeValues)
        .forEach(bc -> {
          bc.accept(GROUP_ID, groupId);
          bc.accept(ARTIFACT_ID, artifactId);
          bc.accept(VERSION, version);
        });
  }

  private void replaceFabric8Properties(Document document) throws XPathExpressionException {
    final NodeList nodeList = XMLUtil.evaluateExpressionForItem(document,
        "(/project/properties|/project/profiles/profile/properties)");
    stream(nodeList).map(Node::getChildNodes).flatMap(XMLUtil::stream)
        .filter(n -> Element.class.isAssignableFrom(n.getClass()))
        .map(Element.class::cast)
        .filter(e -> e.getTagName().startsWith(FABRIC8 + "."))
        .forEach(e -> document.renameNode(e, null, e.getNodeName()
            .replace(FABRIC8, JKUBE)
            .replace(PREFIX_FMP, PREFIX_JKUBE)
            .replace(PREFIX_F8, PREFIX_JKUBE)));
  }

  private void replaceConfigurationTagNames(Element pluginNode) {
    try {
      final NodeList nodeList = XMLUtil.evaluateExpressionForItem(pluginNode, "configuration//child::*");
      stream(nodeList).map(Element.class::cast)
          .filter(e -> e.getTagName().startsWith(PREFIX_F8) || e.getTagName().startsWith(PREFIX_FMP))
          .forEach(e -> e.getOwnerDocument().renameNode(e, null, e.getNodeName()
              .replaceFirst(PREFIX_FMP, PREFIX_JKUBE)
              .replaceFirst(PREFIX_F8, PREFIX_JKUBE)));
    } catch (XPathExpressionException e) {
      logger.error("Could not replace configuration for plugin (%s)", e.getMessage());
    }
  }

  private void replaceConfigurationIncludesExcludes(Element pluginNode) {
    try {
    final NodeList nodeList = XMLUtil.evaluateExpressionForItem(pluginNode, "(configuration//exclude|configuration//include)");
    stream(nodeList).map(Element.class::cast)
        .filter(e -> e.getTextContent().startsWith(PREFIX_FMP) || e.getTextContent().startsWith(PREFIX_F8))
        .forEach(e -> e.setTextContent(e.getTextContent()
            .replaceFirst(PREFIX_FMP, PREFIX_JKUBE)
            .replaceFirst(PREFIX_F8, PREFIX_JKUBE)
        ));
    } catch (XPathExpressionException e) {
      logger.error("Could not replace configuration includes/excludes for plugin (%s)", e.getMessage());
    }
  }

  private static BiConsumer<String, String> replaceChildNodeValues(Element element) {
    return (tagName, newValue) -> stream(element.getElementsByTagName(tagName)).forEach(n -> n.setTextContent(newValue));
  }

  private void renameResourceFragmentDirectoryToJKube() {
    File fabric8FragmentDirectory = getFabric8ResourceFragmentDirectory(projectBasedir);
    if (fabric8FragmentDirectory.exists()) {
      File jkubeResourceDir = new File(projectBasedir, DEFAULT_RESOURCE_FRAGMENT_DIRECTORY);
      try {
        FileUtils.copyDirectory(fabric8FragmentDirectory, jkubeResourceDir);
        FileUtils.deleteDirectory(fabric8FragmentDirectory);
        logger.info("Renamed " + DEFAULT_FABRIC8_RESOURCE_FRAGMENT_DIRECTORY + " to " + DEFAULT_RESOURCE_FRAGMENT_DIRECTORY);
      } catch(IOException ex) {
        logger.warn("Unable to rename resource fragment directory in project: %s", ex.getMessage());
      }
    }
  }

}
