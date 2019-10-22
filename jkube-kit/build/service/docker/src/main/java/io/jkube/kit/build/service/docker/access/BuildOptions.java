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
package io.jkube.kit.build.service.docker.access;

import java.util.HashMap;
import java.util.Map;

import io.jkube.kit.common.JsonFactory;

/**
 * @author roland
 * @since 03/01/17
 */
public class BuildOptions {

    private Map<String, String> options;

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

