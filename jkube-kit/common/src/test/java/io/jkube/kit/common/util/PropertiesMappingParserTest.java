/**
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.jkube.kit.common.util;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

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
