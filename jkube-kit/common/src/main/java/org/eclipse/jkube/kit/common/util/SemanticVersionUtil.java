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
package org.eclipse.jkube.kit.common.util;

import org.apache.commons.lang3.StringUtils;

public class SemanticVersionUtil {
  private SemanticVersionUtil() { }

  public static boolean isVersionAtLeast(int majorVersion, int minorVersion, String version) {
    if (StringUtils.isNotBlank(version) && version.contains(".")) {
      final String[] versionParts = version.split("\\.");
      final int parsedMajorVersion = parseInt(versionParts[0]);
      if (parsedMajorVersion > majorVersion) {
        return true;
      } else if (parsedMajorVersion == majorVersion) {
        return parseInt(versionParts[1]) >= minorVersion;
      }
    }
    return false;
  }

  private static int parseInt(String toParse) {
    try {
      return Integer.parseInt(toParse);
    } catch (NumberFormatException ex) {
      return -1;
    }
  }
}
