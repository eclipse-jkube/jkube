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
package org.eclipse.jkube.kit.config.resource;

import org.apache.commons.lang3.StringUtils;

/**
 * How to recreate the build config and/or image stream created by the build.
 * Only in effect when <code>mode == openshift</code>. If not set, existing
 * build config will not be recreated.
 *
 * The possible values are:
 *
 * <ul>
 *   <li><strong>buildConfig</strong> or <strong>bc</strong> :
 *       Only the build config is recreated</li>
 *   <li><strong>imageStream</strong> or <strong>is</strong> :
 *       Only the image stream is recreated</li>
 *   <li><strong>all</strong> : Both, build config and image stream are recreated</li>
 *   <li><strong>none</strong> : Neither build config nor image stream is recreated</li>
 * </ul>
 *
 * @author roland
 * @since 23/07/16
 */
public enum BuildRecreateMode {

    bc(true, false),
    buildConfig(true, false),

    is(false, true),
    imageStream(false, true),

    all(true, true),

    none(false, false);

    // ==============================================================

    private final boolean isBuildConfig;
    private final boolean isImageStream;

    public static BuildRecreateMode fromParameter(String param) {
        if (StringUtils.isBlank(param)) {
            return none;
        } else if (param.equalsIgnoreCase("true")) {
            return all;
        }
        return valueOf(param.toLowerCase());
    }

    private BuildRecreateMode(boolean bc, boolean is) {
        this.isBuildConfig = bc;
        this.isImageStream = is;
    }

    public boolean isImageStream() {
        return isImageStream;
    }

    public boolean isBuildConfig() {
        return isBuildConfig;
    }
}
