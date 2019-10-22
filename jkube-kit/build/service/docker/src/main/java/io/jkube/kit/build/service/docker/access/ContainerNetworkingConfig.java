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

import com.google.gson.JsonObject;
import io.jkube.kit.build.service.docker.config.NetworkConfig;
import io.jkube.kit.common.JsonFactory;


public class ContainerNetworkingConfig {

    private final JsonObject networkingConfig = new JsonObject();

    /**
     * Add networking aliases to a custom network
     *
     * @param config network config as configured in the pom.xml
     * @return this configuration
     */
    public ContainerNetworkingConfig aliases(NetworkConfig config) {
        JsonObject endPoints = new JsonObject();
        endPoints.add("Aliases", JsonFactory.newJsonArray(config.getAliases()));

        JsonObject endpointConfigMap = new JsonObject();
        endpointConfigMap.add(config.getCustomNetwork(), endPoints);

        networkingConfig.add("EndpointsConfig", endpointConfigMap);
        return this;
    }

    public String toJson() {
        return networkingConfig.toString();
    }

    public JsonObject toJsonObject() {
        return networkingConfig;
    }
}

