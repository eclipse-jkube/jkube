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
package org.eclipse.jkube.kit.build.service.docker.config.handler;

import org.eclipse.jkube.kit.common.RegistryServerConfiguration;
import org.junit.jupiter.api.Test;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@SuppressWarnings({"rawtypes", "unchecked"})
class RegistryServerConfigurationTest {

    @Test
    void testParsingFromMap() {
        Map<String, AbstractMap.SimpleEntry<AbstractMap.SimpleEntry<String, String>, Map<String, Object>>> registryServerAsMap = new HashMap<>();

        registryServerAsMap.put("docker.io", new AbstractMap.SimpleEntry(new AbstractMap.SimpleEntry<>("username", "password"), Collections.emptyMap()));
        registryServerAsMap.put("quay.io", new AbstractMap.SimpleEntry(new AbstractMap.SimpleEntry("quayUsername", "quayPassword"), null));
        List<RegistryServerConfiguration> registryServerConfiguration = RegistryServerConfiguration.fetchListFromMap(registryServerAsMap);
        assertThat(registryServerConfiguration)
                .hasSize(2)
                .extracting(RegistryServerConfiguration::getId, RegistryServerConfiguration::getUsername, RegistryServerConfiguration::getPassword, RegistryServerConfiguration::getConfiguration)
                .containsExactlyInAnyOrder(
                        tuple("docker.io", "username", "password", Collections.emptyMap()),
                        tuple("quay.io", "quayUsername", "quayPassword", null)
                );
    }
}
