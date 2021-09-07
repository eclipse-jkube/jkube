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
package org.eclipse.jkube.kit.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class PropertiesMappingParserTest {

    private static final String MAPPING_PROPERTIES = "ConfigMap=cm, configmap";

    @Test
    public void should_read_mappings_from_properties_file() {

        // Given

        final PropertiesMappingParser propertiesMappingParser = new PropertiesMappingParser();

        // When

        final Map<String, List<String>> serializedContent =
            propertiesMappingParser.parse(new ByteArrayInputStream(MAPPING_PROPERTIES.getBytes()));

        // Then

        final Map<String, List<String>> expectedSerlializedContent = new HashMap<>();
        expectedSerlializedContent.put("ConfigMap", Arrays.asList("cm", "configmap"));

        assertThat(serializedContent)
            .containsAllEntriesOf(expectedSerlializedContent);
    }

}
