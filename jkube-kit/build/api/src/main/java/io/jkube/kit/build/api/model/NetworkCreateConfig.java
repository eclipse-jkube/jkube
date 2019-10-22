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
package io.jkube.kit.build.api.model;

import com.google.gson.JsonObject;

public class NetworkCreateConfig {
    final JsonObject createConfig = new JsonObject();
    final String name;

    public NetworkCreateConfig(String name) {
        this.name = name;
        createConfig.addProperty("Name", name);
    }

    public String getName() {
        return name;
    }

    /**
     * Get JSON which is used for <em>creating</em> a network
     *
     * @return string representation for JSON representing creating a network
     */
    public String toJson() {
        return createConfig.toString();
    }
}

