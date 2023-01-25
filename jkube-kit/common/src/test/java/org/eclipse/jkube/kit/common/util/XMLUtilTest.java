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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.kit.common.util.XMLUtil.stream;

class XMLUtilTest {

    @Test
    void testReadXML() throws Exception {
        // Given
        File sampleXML = new File(getClass().getResource("/util/xml-util.xml").toURI());
        // When
        Document document = XMLUtil.readXML(sampleXML);
        // Then
        assertThat(document).isNotNull();
        assertThat(document.getElementsByTagName("root").getLength()).isEqualTo(1);
    }

    @Test
    void testXMLWrite(@TempDir File folder) throws IOException, ParserConfigurationException, TransformerException {
        // Given
        File cloneXML = new File(folder, "pom-clone.xml");
        Document dom = XMLUtil.createNewDocument();
        Element rootElement = dom.createElement("project");
        rootElement.appendChild(createSimpleTextNode(dom, "groupId", "org.eclipse.jkube"));
        rootElement.appendChild(createSimpleTextNode(dom, "artifactId", "jkube-kit"));
        rootElement.appendChild(createSimpleTextNode(dom, "version", "1.0.0"));
        dom.appendChild(rootElement);

        // When
        XMLUtil.writeXML(dom, cloneXML);

        // Then
        assertThat(cloneXML).exists();
        assertThat(new String(Files.readAllBytes(cloneXML.toPath()))).isEqualTo(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><project><groupId>org.eclipse.jkube</groupId><artifactId>jkube-kit</artifactId><version>1.0.0</version></project>");
    }

    @Test
    void testEvaluateExpressionForItem() throws Exception {
        // Given
        File sampleXML = new File(getClass().getResource("/util/xml-util.xml").toURI());
        Document document = XMLUtil.readXML(sampleXML);
        // When
        final NodeList result = XMLUtil.evaluateExpressionForItem(document, "/root");
        // Then
        assertThat(document).isNotNull();
        assertThat(result.getLength()).isEqualTo(1);
        assertThat(result.item(0).getNodeName()).isEqualTo("root");
    }

    @Test
    void testStream() throws Exception {
        // Given
        final Document document = XMLUtil.readXML(new File(getClass().getResource("/util/xml-util.xml").toURI()));
        // When
        final boolean result = stream(document.getChildNodes()).anyMatch(node -> node.getNodeName().equals("root"));
        // Then
        assertThat(result).isTrue();
    }

    private Node createSimpleTextNode(Document doc, String name, String value) {
        Element node = doc.createElement(name);
        node.appendChild(doc.createTextNode(value));
        return node;
    }
}
