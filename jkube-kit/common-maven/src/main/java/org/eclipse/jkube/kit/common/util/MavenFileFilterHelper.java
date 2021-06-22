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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenFilteringException;

import java.io.File;
import java.io.IOException;

public class MavenFileFilterHelper {
    private MavenFileFilterHelper() { }

    public static File[] mavenFilterFiles(MavenFileFilter mavenFileFilter, MavenProject project, MavenSession session, File[] resourceFiles, File outDir) throws IOException {
        if (resourceFiles == null) {
            return new File[0];
        }
        if (!outDir.exists() && !outDir.mkdirs()) {
            throw new IOException("Cannot create working dir " + outDir);
        }
        File[] ret = new File[resourceFiles.length];
        int i = 0;
        for (File resource : resourceFiles) {
            File targetFile = new File(outDir, resource.getName());
            try {
                mavenFileFilter.copyFile(resource, targetFile, true,
                        project, null, false, "utf8", session);
                ret[i++] = targetFile;
            } catch (MavenFilteringException exp) {
                throw new IOException(
                        String.format("Cannot filter %s to %s", resource, targetFile), exp);
            }
        }
        return ret;
    }
}
