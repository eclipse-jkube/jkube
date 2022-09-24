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

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class KindFilenameMapperUtilTest {
    @Test
    void shouldLoadMappings() {
        // given
        // source: test/resources/**/kind-file-type-mappings-default.ascii
        Map<String, List<String>> expectedSerializedMappigs = new HashMap<String, List<String>>() {{
            put("BuildConfig", Arrays.asList("bc", "buildconfig"));
            put("ClusterRole", Arrays.asList("cr", "crole", "clusterrole"));
            put("ConfigMap", Arrays.asList("cm", "configmap"));
            put("CronJob", Arrays.asList("cj", "cronjob"));
        }};

        // source: test/resources/**/kind-file-type-mappings-default.properties
        Map<String, List<String>> expectedPropertiesMappings =  new HashMap<String, List<String>>() {{
            put("Pod", Arrays.asList("pd", "pod"));
        }};

        // when
        Map<String, List<String>> defaultMappings = KindFilenameMapperUtil.loadMappings();

        // then
        assertThat(defaultMappings)
                .containsAllEntriesOf(expectedSerializedMappigs)
                .containsAllEntriesOf(expectedPropertiesMappings);
    }
}