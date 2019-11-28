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
package org.eclipse.jkube.kit.build.api.model;

import com.google.gson.JsonObject;

public class NetworksListElement implements Network {

    static final String NAME = "Name";
    static final String ID = "Id";
    static final String SCOPE = "Scope";
    static final String DRIVER = "Driver";

    private final JsonObject json;

    public NetworksListElement(JsonObject json) {
        this.json = json;
    }

    @Override
    public String getName() {
        return json.get(NAME).getAsString();
    }

    @Override
    public String getDriver() {
        return json.get(DRIVER).getAsString();
    }

    @Override
    public String getScope() {
        return json.get(SCOPE).getAsString();
    }

    @Override
    public String getId() {
        // only need first 12 to id a network
        return json.get(ID).getAsString().substring(0, 12);
    }

}
