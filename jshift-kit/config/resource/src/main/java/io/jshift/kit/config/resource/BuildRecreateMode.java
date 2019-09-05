/**
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.jshift.kit.config.resource;

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

    private boolean isBuildConfig, isImageStream;

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
