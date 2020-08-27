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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static org.eclipse.jkube.kit.common.util.XMLUtil.stream;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class XMLUtilTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testReadXML() throws Exception {
        // Given
        File sampleXML = new File(getClass().getResource("/util/xml-util.xml").toURI());
        // When
        Document document = XMLUtil.readXML(sampleXML);
        // Then
        assertNotNull(document);
        assertEquals(1, document.getElementsByTagName("root").getLength());
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
    public void testEvaluateExpressionForItem() throws Exception {
        // Given
        File sampleXML = new File(getClass().getResource("/util/xml-util.xml").toURI());
        Document document = XMLUtil.readXML(sampleXML);
        // When
        final NodeList result = XMLUtil.evaluateExpressionForItem(document, "/root");
        // Then
        assertNotNull(document);
        assertEquals(1, result.getLength());
        assertEquals("root", result.item(0).getNodeName());
    }

    @Test
    public void testStream() throws Exception {
        // Given
        final Document document = XMLUtil.readXML(new File(getClass().getResource("/util/xml-util.xml").toURI()));
        // When
        final boolean result = stream(document.getChildNodes()).anyMatch(node -> node.getNodeName().equals("root"));
        // Then
        assertTrue(result);
    }

    private Node createSimpleTextNode(Document doc, String name, String value) {
        Element node = doc.createElement(name);
        node.appendChild(doc.createTextNode(value));
        return node;
    }
}
