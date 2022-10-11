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
package org.eclipse.jkube.openliberty;

import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.util.XMLUtil;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;

public class OpenLibertyUtils {
  private static final String SERVER_CONFIG_FILE_PATH = "src/main/liberty/config/server.xml";

  private OpenLibertyUtils() { }

  public static boolean isMicroProfileHealthEnabled(JavaProject javaProject) {
    return hasAnyFeatureMatching(javaProject, "microProfile-") ||
        hasAnyFeatureMatching(javaProject, "mpHealth-");
  }

  public static boolean hasAnyFeatureMatching(JavaProject javaProject, String feature)  {
    try {
      File serverXml = new File(javaProject.getBaseDirectory(), SERVER_CONFIG_FILE_PATH);
      final Document serverConfig = XMLUtil.readXML(serverXml);

      return XMLUtil.evaluateExpressionForItem(serverConfig,
          String.format("/server/featureManager/feature[contains(text(),'%s')]", feature)).getLength() > 0;
    } catch (IOException | ParserConfigurationException | SAXException | XPathExpressionException exception) {
      return false;
    }
  }
}
