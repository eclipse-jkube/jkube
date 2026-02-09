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
    return isVersionAtLeast(majorVersion, minorVersion, 0, version);
  }

  public static boolean isVersionAtLeast(int majorVersion, int minorVersion, int patchVersion, String version) {
    if (StringUtils.isNotBlank(version) && version.contains(".")) {
      final String[] versionParts = version.split("\\.");
      final int parsedMajorVersion = parseInt(versionParts[0]);
      if (parsedMajorVersion > majorVersion) {
        return true;
      } else if (parsedMajorVersion == majorVersion) {
        final int parsedMinorVersion = parseInt(versionParts[1]);
        if (parsedMinorVersion > minorVersion) {
          return true;
        } else if (parsedMinorVersion == minorVersion) {
          // If no patch version in the string, treat as .0
          final int parsedPatchVersion = versionParts.length > 2 ? parseInt(versionParts[2]) : 0;
          return parsedPatchVersion >= patchVersion;
        }
      }
    }
    return false;
  }

  /**
   * Remove build metadata from provided version
   *
   * @param version version with full version+(build metadata) format
   * @return string containing just the version
   */
  public static String removeBuildMetadata(String version) {
    if (StringUtils.isNotBlank(version) && version.contains("+")) {
      int indexOfBuildMetadataDelimiter = version.indexOf('+');
      return version.substring(0, indexOfBuildMetadataDelimiter);
    }
    return version;
  }

  private static int parseInt(String toParse) {
    try {
      return Integer.parseInt(toParse);
    } catch (NumberFormatException ex) {
      return -1;
    }
  }
}
