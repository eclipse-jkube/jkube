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

import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class MigrateService {
    private File projectBasedir;
    private static final String POM_XML = "pom.xml";
    private static final String FABRIC8 = "fabric8";
    private static final String JKUBE = "jkube";
    private static final String DEFAULT_FABRIC8_RESOURCE_FRAGMENT_DIRECTORY = "src/main/" + FABRIC8;
    private static final String DEFAULT_RESOURCE_FRAGMENT_DIRECTORY = "src/main/" + JKUBE;
    private static final String ARTIFACT_ID = "artifactId";
    private static final String GROUP_ID = "groupId";
    private static final String VERSION = "version";
    private static final String FABRIC8_MAVEN_PLUGIN_ARTIFACT_ID = FABRIC8 + "-maven-plugin";
    private static final String POM_PROJECT_PATH = "/project";
    private static final String POM_PROFILES_PATH = POM_PROJECT_PATH + "/profiles";
    private static final String POM_PLUGINS_PATH = "/build/plugins";
    private static final String POM_PROPERTIES_PATH = "/properties";
    private static final String POM_PLUGIN_PATH = "/plugin";
    private KitLogger logger;

    public MigrateService(File projectBaseDirectory, KitLogger logger) {
        this.projectBasedir = projectBaseDirectory;
        this.logger = logger;
    }

    public void migrate(String pluginGroupId, String pluginArtifactId, String pluginVersion) throws IOException, ParserConfigurationException, SAXException, TransformerException, XPathExpressionException {
        File pomFile = getPomFile();
        String pomContent = new String(Files.readAllBytes(pomFile.toPath()), StandardCharsets.UTF_8);
        String modifiedPomContent = searchAndReplaceEnricherRefsInPomToJKube(pomContent);
        Files.write(pomFile.toPath(), modifiedPomContent.getBytes(StandardCharsets.UTF_8));
        if (pomContainsFMP(pomContent)) {
            Document dom = XMLUtil.readXML(pomFile);

            // Check Whether plugin is present in project.build.plugins
            modifyFMPPluginSectionInsideBuild(dom, pluginGroupId, pluginArtifactId, pluginVersion);
            // Check Whether plugin is present in project.profiles.build.plugins
            modifyFMPSectionInsideProfile(dom, pluginGroupId, pluginArtifactId, pluginVersion);
            // Rename all Fabric8 related properties to JKube
            modifyFMPPropertiesInsidePom(dom);

            XMLUtil.writeXML(dom, pomFile);
            renameResourceFragmentDirectoryToJKube();
        } else {
            logger.warn("Unable to find Fabric8 Maven Plugin inside pom");
        }
    }

    private String searchAndReplaceEnricherRefsInPomToJKube(String pomContent) {
        pomContent = pomContent.replace("fmp-", JKUBE + "-");
        pomContent = pomContent.replace("f8-", JKUBE + "-");

        return pomContent;
    }

    private boolean pomContainsFMP(String pomContent) {
        return pomContent.contains(FABRIC8_MAVEN_PLUGIN_ARTIFACT_ID);
    }

    private File getPomFile() {
        return new File(projectBasedir, POM_XML);
    }

    private File getResourceFragmentDirectory(File projectBasedir) {
        return new File(projectBasedir, DEFAULT_FABRIC8_RESOURCE_FRAGMENT_DIRECTORY);
    }

    private void modifyFMPPluginSectionInsideBuild(Document dom, String pluginGroupId, String pluginArtifactId, String pluginVersion) throws XPathExpressionException {
        convertFMPNodeToJKube(dom,  pluginGroupId, pluginArtifactId, pluginVersion, POM_PROJECT_PATH + POM_PLUGINS_PATH);
    }

    private void modifyFMPSectionInsideProfile(Document dom, String pluginGroupId, String pluginArtifactId, String pluginVersion) throws XPathExpressionException {
        Node profilesNode = XMLUtil.getNodeFromDocument(dom, POM_PROFILES_PATH);
        if (profilesNode != null) {
            NodeList nodeList = profilesNode.getChildNodes();
            for (int i = 0; i < nodeList.getLength(); i++) {
                convertFMPNodeToJKube(dom, pluginGroupId, pluginArtifactId, pluginVersion, POM_PROFILES_PATH + "/profile[" + i + "]" + POM_PLUGINS_PATH);
            }
        }
    }

    private void convertFMPNodeToJKube(Document dom, String pluginGroupId, String pluginArtifactId, String pluginVersion, String parentXPathExpression) throws XPathExpressionException {
        Node fmpNode = XMLUtil.getNodeFromDocument(dom, parentXPathExpression + POM_PLUGIN_PATH + "[artifactId='" + FABRIC8_MAVEN_PLUGIN_ARTIFACT_ID + "']");
        if (fmpNode != null) {
            Element fmpNodeElem = (Element) fmpNode;
            Node artifactNode = fmpNodeElem.getElementsByTagName(ARTIFACT_ID).item(0);
            Node groupNode = fmpNodeElem.getElementsByTagName(GROUP_ID).item(0);
            Node versionNode = fmpNodeElem.getElementsByTagName(VERSION).item(0);
            logger.info("Found Fabric8 Maven Plugin in pom with version " + versionNode.getTextContent());
            groupNode.setTextContent(pluginGroupId);
            artifactNode.setTextContent(pluginArtifactId);
            versionNode.setTextContent(pluginVersion);
        }
    }

    private void renameResourceFragmentDirectoryToJKube() {
        File resourceFragmentDir = getResourceFragmentDirectory(projectBasedir);
        if (resourceFragmentDir.exists()) {
            File jkubeResourceDir = new File(projectBasedir, DEFAULT_RESOURCE_FRAGMENT_DIRECTORY);
            boolean isRenamed = resourceFragmentDir.renameTo(jkubeResourceDir);
            if (!isRenamed) {
                logger.warn("Unable to rename resource fragment directory in project");
            } else {
                logger.info("Renamed " + DEFAULT_FABRIC8_RESOURCE_FRAGMENT_DIRECTORY + " to " + DEFAULT_RESOURCE_FRAGMENT_DIRECTORY);
            }
        }
    }

    private void modifyFMPPropertiesInsidePom(Document dom) throws XPathExpressionException {
        Node propertiesNode = XMLUtil.getNodeFromDocument(dom, POM_PROJECT_PATH + POM_PROPERTIES_PATH);
        if (propertiesNode != null) {
            NodeList nodeList = propertiesNode.getChildNodes();
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node property = nodeList.item(i);
                if (property.getNodeName().contains(FABRIC8)) {
                    String nodeName = property.getNodeName();
                    nodeName = nodeName.replace(FABRIC8, JKUBE);
                    dom.renameNode(property, null, nodeName);
                }
            }
        }
    }

}
