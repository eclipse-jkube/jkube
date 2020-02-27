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
    private String id;
    private List<JkubeAssemblyFile> files;
    private List<JkubeAssemblyFileSet> fileSets;
    private File baseDirectory;
    private String fileMode;

    public List<JkubeAssemblyFile> getFiles() {
        return files;
    }

    public List<JkubeAssemblyFileSet> getFileSets() {
        return fileSets;
    }

    public File getBaseDirectory() {
        return baseDirectory;
    }

    public void setBaseDirectory(File baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    public String getFileMode() {
        return fileMode;
    }

    public void setFileMode(String fileMode) {
        this.fileMode = fileMode;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setFiles(List<JkubeAssemblyFile> files) {
        this.files = files;
    }

    public void setFileSets(List<JkubeAssemblyFileSet> fileSets) {
        this.fileSets = fileSets;
    }

    public static class Builder {
        private JkubeProjectAssembly jkubeProjectAssembly;

        public Builder() {
            this.jkubeProjectAssembly = new JkubeProjectAssembly();
        }

        public Builder(JkubeProjectAssembly jkubeProjectAssembly) {
            if (jkubeProjectAssembly != null) {
                this.jkubeProjectAssembly = jkubeProjectAssembly;
            }
        }

        public Builder id(String id) {
            this.jkubeProjectAssembly.id = id;
            return this;
        }

        public Builder baseDirectory(File baseDir) {
            this.jkubeProjectAssembly.baseDirectory = baseDir;
            return this;
        }

        public Builder fileMode(String fileMode) {
            this.jkubeProjectAssembly.fileMode = fileMode;
            return this;
        }

        public Builder files(List<JkubeAssemblyFile> file) {
            this.jkubeProjectAssembly.files = file;
            return this;
        }

        public Builder fileSets(List<JkubeAssemblyFileSet> fileSets) {
            this.jkubeProjectAssembly.fileSets = fileSets;
            return this;
        }

        public Builder fileSet(JkubeAssemblyFileSet fileSet) {
            if (this.jkubeProjectAssembly.fileSets == null) {
                this.jkubeProjectAssembly.fileSets = new ArrayList<>();
            }
            this.jkubeProjectAssembly.fileSets.add(fileSet);
            return this;
        }

        public JkubeProjectAssembly build() {
            return this.jkubeProjectAssembly;
        }

    }

}
