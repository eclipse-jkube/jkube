/*
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
package org.eclipse.jkube.kit.build.api.model;

import com.google.gson.JsonObject;
import org.eclipse.jkube.kit.common.JsonFactory;

import java.util.Map;


public class VolumeCreateConfig
{
    private final JsonObject createConfig = new JsonObject();

    public VolumeCreateConfig(String name) {
        add("Name", name);
    }

    public VolumeCreateConfig driver(String driver) {
        return add("Driver", driver);
    }

    public VolumeCreateConfig opts(Map<String, String> opts) {
        if (opts != null && opts.size() > 0) {
            add("DriverOpts", JsonFactory.newJsonObject(opts));
        }
        return this;
    }

    public VolumeCreateConfig labels(Map<String,String> labels) {
        if (labels != null && labels.size() > 0) {
            add("Labels", JsonFactory.newJsonObject(labels));
        }
        return this;
    }

    public String getName() {
        return createConfig.get("Name").getAsString();
    }

    /**
     * Get JSON which is used for <em>creating</em> a volume
     *
     * @return string representation for JSON representing creating a volume
     */
    public String toJson() {
        return createConfig.toString();
    }

    // =======================================================================

    private VolumeCreateConfig add(String name, JsonObject value) {
        if (value != null) {
            createConfig.add(name, value);
        }
        return this;
    }

    private VolumeCreateConfig add(String name, String value) {
        if (value != null) {
            createConfig.addProperty(name, value);
        }
        return this;
    }
}

