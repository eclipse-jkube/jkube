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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class JkubeAssemblyFileSet implements Serializable {
    private String directory;
    private String outputDirectory;
    private boolean filtered;
    private List<String> includes;
    private List<String> exludes;
    private String fileMode;

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public boolean isFiltered() {
        return filtered;
    }

    public void setFiltered(boolean filtered) {
        this.filtered = filtered;
    }

    public List<String> getIncludes() {
        return includes;
    }

    public void setIncludes(List<String> includes) {
        this.includes = includes;
    }

    public String getFileMode() {
        return fileMode;
    }

    public void setFileMode(String fileMode) {
        this.fileMode = fileMode;
    }

    public void setExludes(List<String> items) {
        this.exludes = items;
    }

    public List<String> getExludes() {
        return this.exludes;
    }

    public void addInclude(String item) {
        if (this.includes == null) {
            this.includes = new ArrayList<>();
        }
        this.includes.add(item);
    }

    public void addExclude(String item) {
        if (this.exludes == null) {
            this.exludes = new ArrayList<>();
        }
        this.exludes.add(item);
    }

    public static class Builder {
        private JkubeAssemblyFileSet jkubeAssemblyFileSet;

        public Builder() {
            this.jkubeAssemblyFileSet = new JkubeAssemblyFileSet();
        }

        public Builder(JkubeAssemblyFileSet jkubeAssemblyFileSet) {
            if (jkubeAssemblyFileSet != null) {
                this.jkubeAssemblyFileSet = jkubeAssemblyFileSet;
            }
        }

        public Builder directory(String directory) {
            this.jkubeAssemblyFileSet.directory = directory;
            return this;
        }

        public Builder outputDirectory(String outputDirectory) {
            this.jkubeAssemblyFileSet.outputDirectory = outputDirectory;
            return this;
        }

        public Builder filtered(boolean isFiltered) {
            this.jkubeAssemblyFileSet.filtered = isFiltered;
            return this;
        }

        public Builder includes(List<String> includes) {
            this.jkubeAssemblyFileSet.includes = includes;
            return this;
        }

        public Builder addInclude(String include) {
            if (this.jkubeAssemblyFileSet.includes == null) {
                this.jkubeAssemblyFileSet.includes = new ArrayList<>();
            }
            this.jkubeAssemblyFileSet.includes.add(include);
            return this;
        }

        public Builder excludes(List<String> excludes) {
            this.jkubeAssemblyFileSet.exludes = excludes;
            return this;
        }

        public Builder addExclude(String exclude) {
            if (this.jkubeAssemblyFileSet.exludes == null) {
                this.jkubeAssemblyFileSet.exludes = new ArrayList<>();
            }
            this.jkubeAssemblyFileSet.exludes.add(exclude);
            return this;
        }

        public Builder fileMode(String fileMode) {
            this.jkubeAssemblyFileSet.fileMode = fileMode;
            return this;
        }

        public JkubeAssemblyFileSet build() {
            return jkubeAssemblyFileSet;
        }

    }
}
