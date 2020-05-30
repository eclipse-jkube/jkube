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
package org.eclipse.jkube.kit.common.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class XMLUtilTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testReadXML() throws URISyntaxException, IOException, SAXException, ParserConfigurationException, XPathExpressionException {
        // Given
        File sampleXML = new File(getClass().getResource("/test-project/pom.xml").toURI());

        // When
        Document document = XMLUtil.readXML(sampleXML);

        // Then
        assertNotNull(document);
        assertEquals("random-generator", XMLUtil.getNodeValueFromDocument(document, "/project/name"));
    }

    @Test
    public void testXMLWrite() throws IOException, ParserConfigurationException, TransformerException {
        // Given
        File cloneXML = folder.newFile("pom-clone.xml");
        Document dom = XMLUtil.createNewDocument();
        Element rootElement = dom.createElement("project");
        rootElement.appendChild(createSimpleTextNode(dom, "groupId", "org.eclipse.jkube"));
        rootElement.appendChild(createSimpleTextNode(dom, "artifactId", "jkube-kit"));
        rootElement.appendChild(createSimpleTextNode(dom, "version", "1.0.0"));
        dom.appendChild(rootElement);

        // When
        XMLUtil.writeXML(dom, cloneXML);

        // Then
        assertTrue(cloneXML.exists());
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><project><groupId>org.eclipse.jkube</groupId><artifactId>jkube-kit</artifactId><version>1.0.0</version></project>", new String(Files.readAllBytes(cloneXML.toPath())));
    }

    @Test
    public void testGetNodeFromDocument() throws IOException, SAXException, ParserConfigurationException, XPathExpressionException, URISyntaxException {
        // Given
        File sampleXML = new File(getClass().getResource("/test-project/pom.xml").toURI());

        // When
        Document document = XMLUtil.readXML(sampleXML);
        Node node = XMLUtil.getNodeFromDocument(document, "/project/build");

        // Then
        assertNotNull(document);
        assertEquals("build", node.getNodeName());
        assertEquals(3, node.getChildNodes().getLength());
    }

    private Node createSimpleTextNode(Document doc, String name, String value) {
        Element node = doc.createElement(name);
        node.appendChild(doc.createTextNode(value));
        return node;
    }
}
