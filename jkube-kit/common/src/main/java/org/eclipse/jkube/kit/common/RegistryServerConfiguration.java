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

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RegistryServerConfiguration implements Serializable {

    private String id;
    private String username;
    private String password;
    private Map<String, Object> configuration;

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    private RegistryServerConfiguration() { }

    public RegistryServerConfiguration(String id, String username, String decryptedPassword, Map<String, Object> configurationAsMap) {
        this.id = id;
        this.username = username;
        this.password = decryptedPassword;
        this.configuration = configurationAsMap;
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

    public static RegistryServerConfiguration getServer(final List<RegistryServerConfiguration> settings, final String serverId) {
        if (settings != null && !StringUtils.isBlank(serverId)) {
            for (RegistryServerConfiguration registryServerConfiguration : settings) {
                if (registryServerConfiguration.getId().equalsIgnoreCase(serverId)) {
                    return registryServerConfiguration;
                }
            }
        }

        return null;
    }

    public static List<RegistryServerConfiguration> fetchListFromMap(Map<String, AbstractMap.SimpleEntry<AbstractMap.SimpleEntry<String, String>, Map<String, Object>>> registryServerConfigurationMap) {
        List<RegistryServerConfiguration> registryServerConfigurationList = new ArrayList<>();
        for (Map.Entry<String, AbstractMap.SimpleEntry<AbstractMap.SimpleEntry<String, String>, Map<String, Object>>> entry : registryServerConfigurationMap.entrySet()) {
            registryServerConfigurationList.add(new RegistryServerConfiguration(entry.getKey(),
                    entry.getValue().getKey().getKey(), entry.getValue().getKey().getValue(), entry.getValue().getValue()));
        }
        return registryServerConfigurationList;
    }

    public static class Builder {
        private RegistryServerConfiguration registryServerConfiguration;

        public Builder() {
            this.registryServerConfiguration = new RegistryServerConfiguration();
        }

        public Builder(RegistryServerConfiguration registryServerConfiguration) {
            if (registryServerConfiguration != null) {
                this.registryServerConfiguration = registryServerConfiguration;
            }
        }

        public Builder id(String id) {
            this.registryServerConfiguration.id = id;
            return this;
        }

        public Builder username(String username) {
            this.registryServerConfiguration.username = username;
            return this;
        }

        public Builder password(String password) {
            this.registryServerConfiguration.password = password;
            return this;
        }

        public Builder configuration(Map<String, Object> configuration) {
            this.registryServerConfiguration.configuration = configuration;
            return this;
        }

        public RegistryServerConfiguration build() {
            return this.registryServerConfiguration;
        }
    }

}
