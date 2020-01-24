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
package org.eclipse.jkube.kit.build.service.docker.config;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RegistryServerConfiguration {

    private String id;
    private String username;
    private String password;
    private Object configuration;

    public Object getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Object configuration) {
        this.configuration = configuration;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public RegistryServerConfiguration(String id, String username, String encryptedPassword, Object configuration) {
        this.id = id;
        this.username = username;
        this.password = encryptedPassword;
        this.configuration = configuration;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public static List<RegistryServerConfiguration> fetchListFromMap(Map<String, AbstractMap.SimpleEntry<AbstractMap.SimpleEntry<String, String>, Object>> registryServerConfigurationMap) {
        List<RegistryServerConfiguration> registryServerConfigurationList = new ArrayList<>();
        for (Map.Entry<String, AbstractMap.SimpleEntry<AbstractMap.SimpleEntry<String, String>, Object>> entry : registryServerConfigurationMap.entrySet()) {
            registryServerConfigurationList.add(new RegistryServerConfiguration(entry.getKey(),
                    entry.getValue().getKey().getKey(), entry.getValue().getKey().getValue(), entry.getValue().getValue()));
        }
        return registryServerConfigurationList;
    }

}
