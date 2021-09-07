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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jkube.kit.common.RegistryServerConfiguration;

import org.junit.Test;

public class RegistryServerConfigurationTest {
    @Test
    public void testParsingFromMap() {
        Map<String, AbstractMap.SimpleEntry<AbstractMap.SimpleEntry<String, String>, Map<String, Object>>> registryServerAsMap = new HashMap<>();

        registryServerAsMap.put("docker.io", new AbstractMap.SimpleEntry(new AbstractMap.SimpleEntry<>("username", "password"), Collections.emptyMap()));
        registryServerAsMap.put("quay.io", new AbstractMap.SimpleEntry(new AbstractMap.SimpleEntry("quayUsername", "quayPassword"), null));
        List<RegistryServerConfiguration> registryServerConfiguration = RegistryServerConfiguration.fetchListFromMap(registryServerAsMap);

        assertEquals(2, registryServerConfiguration.size());
        assertEquals("docker.io", registryServerConfiguration.get(0).getId());
        assertEquals("username", registryServerConfiguration.get(0).getUsername());
        assertEquals("password", registryServerConfiguration.get(0).getPassword());
        assertNotNull(registryServerConfiguration.get(0).getConfiguration());
        assertEquals("quay.io", registryServerConfiguration.get(1).getId());
        assertEquals("quayUsername", registryServerConfiguration.get(1).getUsername());
        assertEquals("quayPassword", registryServerConfiguration.get(1).getPassword());
        assertNull(registryServerConfiguration.get(1).getConfiguration());
    }
}
