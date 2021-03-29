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
package org.eclipse.jkube.kit.build.service.docker.config.handler.compose;

import org.eclipse.jkube.kit.build.service.docker.helper.DockerPathUtil;

import java.io.File;
import java.io.IOException;

/**
 * Path-resolution methods
 */
class ComposeUtils {

    /**
     * Resolves a docker-compose file against the supplied base directory.  The returned {@code File} is guaranteed to
     * be {@link File#isAbsolute() absolute}.
     * <p>
     * If {@code composeFile} is {@link File#isAbsolute() absolute}, then it is returned unmodified.  Otherwise, the
     * {@code composeFile} is returned as an absolute {@code File} using the {@link #resolveAbsolutely(String,
     * String) resolved} {@code baseDir} as its parent.
     * </p>
     *
     * @param baseDir the base directory containing the docker-compose file (ignored if {@code composeFile} is absolute)
     * @param composeFile the path of the docker-compose file, may be absolute
     * @param projectAbsolutePath the {@code String} used to resolve the {@code baseDir}
     * @return an absolute {@code File} reference to the {@code composeFile}
     */
    static File resolveComposeFileAbsolutely(String baseDir, String composeFile, String projectAbsolutePath) {
        File yamlFile = new File(composeFile);
        if (yamlFile.isAbsolute()) {
            return yamlFile;
        }

        File toCanonicalize = new File(resolveAbsolutely(baseDir, projectAbsolutePath), composeFile);

        try {
            return toCanonicalize.getCanonicalFile();
        } catch (IOException e) {
            throw new RuntimeException("Unable to canonicalize the resolved docker-compose file path '" + toCanonicalize + "'");
        }
    }

    /**
     * Resolves the supplied resource (a path or directory on the filesystem) relative the Maven base directory.  The returned {@code File} is guaranteed to be {@link
     * File#isAbsolute() absolute}.  The returned file is <em>not</em> guaranteed to exist.
     * <p>
     * If {@code pathToResolve} is {@link File#isAbsolute() absolute}, then it is returned unmodified.  Otherwise, the
     * {@code pathToResolve} is returned as an absolute {@code File} using the  Maven
     * Project base directory} as its parent.
     * </p>
     *
     * @param pathToResolve represents a filesystem resource, which may be an absolute path
     * @param absolutePath absolute path of the Maven project used to resolve non-absolute path resources, may be {@code null} if
     *                {@code pathToResolve} is {@link File#isAbsolute() absolute}
     * @return an absolute {@code File} reference to {@code pathToResolve}; <em>not</em> guaranteed to exist
     */
    static File resolveAbsolutely(String pathToResolve, String absolutePath) {
        // avoid an NPE if the Maven project is not needed by DockerPathUtil
        return DockerPathUtil.resolveAbsolutely(pathToResolve, absolutePath);
    }
}
