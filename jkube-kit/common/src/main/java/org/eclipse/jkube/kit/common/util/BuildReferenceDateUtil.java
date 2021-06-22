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

public class BuildReferenceDateUtil {
    private BuildReferenceDateUtil() { }

    public static void storeReferenceDateInPluginContext(Date now, Map<String, Object> pluginContext, String buildTimestampKey) {
        pluginContext.put(buildTimestampKey, now);
    }

    // get a reference date
    public static Date getBuildReferenceDate(String buildDirectory, String dockerBuildTimestampFile) {
        // Pick up an existing build date created by build goal previously
        File tsFile = new File(buildDirectory, dockerBuildTimestampFile);
        if (!tsFile.exists()) {
            return new Date();
        }
        try {
            return EnvUtil.loadTimestamp(tsFile);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read timestamp from " + tsFile, e);
        }
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
    public static synchronized Date getBuildTimestamp(Map<String, Object> pluginContext, String buildTimestampContextKey,
                                                  String projectBuildDir, String dockerBuildTimestampFile) {
        Date now = (Date) pluginContext.get(buildTimestampContextKey);
        if (now == null) {
            now = getBuildReferenceDate(projectBuildDir, dockerBuildTimestampFile);
            pluginContext.put(buildTimestampContextKey, now);
        }
        return now;
    }
}
