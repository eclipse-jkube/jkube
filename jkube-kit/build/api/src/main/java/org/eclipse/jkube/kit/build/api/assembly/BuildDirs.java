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
package org.eclipse.jkube.kit.build.api.assembly;


import java.io.File;

import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.util.EnvUtil;

/**
 * Helper object grouping together all working and output
 * directories.
 *
 * @author roland
 */
public class BuildDirs {

    private final String buildTopDir;
    private final JKubeConfiguration params;

    /**
     * Constructor building up the the output directories
     *
     * @param imageName image name for the image to build
     * @param params mojo params holding base and global output dir
     */
    public BuildDirs(String imageName, JKubeConfiguration params) {
        this.params = params;
        // Replace tag separator with a slash to avoid problems
        // with OSs which gets confused by colons.
        this.buildTopDir = imageName != null ? imageName.replace(':', '/') : null;
    }

    public File getOutputDirectory() {
        return getDir("build");
    }

    public File getWorkingDirectory() {
        return getDir("work");
    }

    public File getTemporaryRootDirectory() {
        return getDir("tmp");
    }

    void createDirs() {
        for (String workDir : new String[] { "build", "work", "tmp" }) {
            File dir = getDir(workDir);
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IllegalArgumentException("Cannot create directory " + dir.getAbsolutePath());
            }
        }
    }

    private File getDir(String dir) {
        return EnvUtil.prepareAbsoluteOutputDirPath(params.getOutputDirectory(),
                params.getProject().getBaseDirectory() != null ? params.getProject().getBaseDirectory().toString() : null, buildTopDir, dir);
    }
}
