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
import java.util.Date;
import java.util.Map;
import java.util.Optional;

public class BuildReferenceDateUtil {
  private BuildReferenceDateUtil() { }

  static Date getBuildReferenceDate(String buildDirectory, String dockerBuildTimestampFile) throws IOException {
    return Optional.ofNullable(EnvUtil.loadTimestamp(getBuildTimestampFile(buildDirectory, dockerBuildTimestampFile)))
        .orElse(new Date());
  }

  public static File getBuildTimestampFile(String projectBuildDirectory, String dockerBuildTimestampFile) {
    return new File(projectBuildDirectory, dockerBuildTimestampFile);
  }

  /**
   * Get the current build timestamp. this has either already been created by a previous
   * call or a new current date is created
   *
   * @param pluginContext Plugin Context
   * @param buildTimestampContextKey Plugin Context's key for build timestamp
   * @param projectBuildDir project's build directory
   * @param dockerBuildTimestampFile docker build timestamp file
   * @return timestamp to use
   */
  public static Date getBuildTimestamp(Map<String, Object> pluginContext, String buildTimestampContextKey,
                                                    String projectBuildDir, String dockerBuildTimestampFile) throws IOException {
    Date now = (Date) (pluginContext != null ? pluginContext.get(buildTimestampContextKey) : null);
    if (now == null) {
      now = getBuildReferenceDate(projectBuildDir, dockerBuildTimestampFile);
      if (pluginContext != null) {
        pluginContext.put(buildTimestampContextKey, now);
      }
    }
    return now;
  }
}