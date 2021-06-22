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
package org.eclipse.jkube.kit.config.resource;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MappingConfigTest {
    @Test
    public void testGetKindFilenameMappingsWithEmptyMappingConfigList() {
        // Given
        List<MappingConfig> mappingConfigList = Collections.emptyList();

        // When
        Map<String, List<String>> kindFilenameMap = MappingConfig.getKindFilenameMappings(mappingConfigList);

        // Then
        assertNotNull(kindFilenameMap);
        assertTrue(kindFilenameMap.isEmpty());
    }

    @Test
    public void testGetKindFilenameMappings() {
        // Given
        List<MappingConfig> mappingConfigList = new ArrayList<>();
        MappingConfig mc1 = new MappingConfig();
        mc1.setFilenameTypes("deployment");
        mc1.setKind("Deployment");
        MappingConfig mc2 = new MappingConfig();
        mc2.setFilenameTypes("ns, namespace");
        mc2.setKind("Namespace");
        mappingConfigList.add(mc1);
        mappingConfigList.add(mc2);

        // When
        Map<String, List<String>> kindFilenameMap = MappingConfig.getKindFilenameMappings(mappingConfigList);

        // Then
        assertNotNull(kindFilenameMap);
        assertFalse(kindFilenameMap.isEmpty());
        assertEquals(2, kindFilenameMap.size());
        assertEquals(Collections.singletonList("deployment"), kindFilenameMap.get("Deployment"));
        assertEquals(Arrays.asList("ns", "namespace"), kindFilenameMap.get("Namespace"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetKindFilenameMappingsWithInvalid() {
        // Given
        MappingConfig mc1 = new MappingConfig();
        mc1.setFilenameTypes("");
        mc1.setKind("Pod");
        List<MappingConfig> mappingConfigList = Collections.singletonList(mc1);

        // When
        MappingConfig.getKindFilenameMappings(mappingConfigList);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetKindFilenameMappingsWithNull() {
        // Given
        MappingConfig mc1 = new MappingConfig();
        mc1.setFilenameTypes("Pod");
        mc1.setKind(null);
        List<MappingConfig> mappingConfigList = Collections.singletonList(mc1);

        // When
        MappingConfig.getKindFilenameMappings(mappingConfigList);
    }
}
