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

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class RegistryServerConfiguration implements Serializable {

    private static final long serialVersionUID = -5916500916284810117L;

    private String id;
    private String username;
    private String password;
    private Map<String, Object> configuration;


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

}
