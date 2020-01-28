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
package org.eclipse.jkube.kit.common;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class JkubeProjectAssembly implements Serializable {
    private File baseDirectory;
    private List<String> relativePathsToBaseDirectory;
    private String fileMode;

    public JkubeProjectAssembly(File baseDir, List<String> relativePaths, String fileModes) {
        this.baseDirectory = baseDir;
        this.relativePathsToBaseDirectory = relativePaths;
        this.fileMode = fileModes;
    }

    public File getBaseDirectory() {
        return baseDirectory;
    }

    public void setBaseDirectory(File baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    public List<String> getRelativePathsToBaseDirectory() {
        return relativePathsToBaseDirectory;
    }

    public void setRelativePathsToBaseDirectory(List<String> relativePathsToBaseDirectory) {
        this.relativePathsToBaseDirectory = relativePathsToBaseDirectory;
    }

    public String getFileMode() {
        return fileMode;
    }

    public void setFileMode(String fileMode) {
        this.fileMode = fileMode;
    }

    public void add(String relativeFilePath) {
        if (this.relativePathsToBaseDirectory == null) {
            this.relativePathsToBaseDirectory = new ArrayList<>();
        }
        this.relativePathsToBaseDirectory.add(relativeFilePath);
    }

}
