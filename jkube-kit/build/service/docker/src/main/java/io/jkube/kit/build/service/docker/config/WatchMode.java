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
package io.jkube.kit.build.service.docker.config;

/**
 * How to watch for image changes
 * @author roland
 * @since 16/06/15
 */
public enum WatchMode {

    /**
     * Copy watched artefacts into contaienr
     */
    copy(false, false, true, "build"),

    /**
     * Build only images
     */
    build(true, false, false, "build"),

    /**
     * Run images
     */
    run(false, true, false, "run"),

    /**
     * Build and run images
     */
    both(true, true, false, "build and run"),

    /**
     * Neither build nor run
     */
    none(false, false, false, "no build and no run");

    private final boolean doRun;
    private final boolean doBuild;
    private final boolean doCopy;
    private final String description;

    WatchMode(boolean doBuild, boolean doRun, boolean doCopy, String description) {
        this.doBuild = doBuild;
        this.doRun = doRun;
        this.doCopy = doCopy;
        this.description = description;
    }

    public boolean isRun() {
        return doRun;
    }

    public boolean isBuild() {
        return doBuild;
    }

    public boolean isCopy() {
        return doCopy;
    }

    public String getDescription() {
        return description;
    }
}
