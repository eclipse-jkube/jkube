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
package org.eclipse.jkube.kit.build.service.docker.access;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jkube.kit.common.JsonFactory;

/**
 * https://docs.docker.com/engine/api/v1.41/#operation/ImageBuild
 * @author roland
 */
public class BuildOptions {

    private final Map<String, String> options;

    public BuildOptions() {
        this(new HashMap<>());
    }

    public BuildOptions(Map<String, String> options) {
        this.options = options != null ? new HashMap<>(options) : new HashMap<>();
    }

    public BuildOptions addOption(String key, String value) {
        options.put(key,value);
        return this;
    }

    public BuildOptions dockerfile(String name) {
        if (name != null) {
            options.put("dockerfile", name);
        }
        return this;
    }

    public BuildOptions forceRemove(boolean forceRm) {
        if (forceRm) {
            options.put("forcerm", "1");
        }
        return this;
    }

    public BuildOptions noCache(boolean noCache) {
        options.put("nocache", noCache ? "1" : "0");
        return this;
    }

    public BuildOptions cacheFrom(List<String> cacheFrom) {
        if (cacheFrom == null || cacheFrom.isEmpty()) {
            options.remove("cachefrom");
        } else {
            options.put("cachefrom", JsonFactory.newJsonArray(cacheFrom).toString());
        }
        return this;
    }

    public BuildOptions buildArgs(Map<String, String> buildArgs) {
        if (buildArgs != null && buildArgs.size() > 0) {
            options.put("buildargs", JsonFactory.newJsonObject(buildArgs).toString());
        }
        return this;
    }

    public Map<String, String> getOptions() {
        return options;
    }
}

