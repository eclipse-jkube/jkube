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

import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class JsonFactory {
    private static final Gson GSON = new Gson();

    private JsonFactory() {
        // Empty Constructor
    }

    public static JsonObject newJsonObject(String json) {
        return GSON.fromJson(json, JsonObject.class);
    }

    public static JsonArray newJsonArray(String json) {
        return GSON.fromJson(json, JsonArray.class);
    }

    public static JsonArray newJsonArray(List<String> list) {
        final JsonArray jsonArray = new JsonArray();

        for(String element : list)
        {
            jsonArray.add(element);
        }

        return jsonArray;
    }

    public static JsonObject newJsonObject(Map<String,String> map) {
        final JsonObject jsonObject = new JsonObject();

        for (Map.Entry<String, String> entry : map.entrySet()) {
            jsonObject.addProperty(entry.getKey(), entry.getValue());
        }

        return jsonObject;
    }
}
