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
package org.eclipse.jkube.kit.config.image.build;

/**
 * Various modes how to add file for the tarball for "docker:build".
 *
 * @author roland
 * @since 18/05/15
 */
public enum AssemblyMode {

    /**
     * Copy files directly in the directory
     */
    dir("dir",false),

    /**
     * Use a ZIP container as intermediate format
     */
    zip("zip",true),

    /**
     * Use a TAR container as intermediate format
     */
    tar("tar",true),

    /**
     * Use a compressed TAR container as intermediate format
     */
    tgz("tgz",true);

    private final String extension;
    private boolean isArchive;

    AssemblyMode(String extension, boolean isArchive) {
        this.extension = extension;
        this.isArchive = isArchive;
    }

    /**
     * Get the extension as known by the Maven assembler
     *
     * @return extension
     */
    public String getExtension() {
        return extension;
    }

    public boolean isArchive() {
        return isArchive;
    }
}
