/*
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class KindFilenameMapperUtilTest {
    @Test
    void shouldLoadMappings() {
        // given

        // source: test/resources/**/kind-file-type-mappings-default.ascii
        // source: test/resources/**/kind-file-type-mappings-default.properties

        // when
        Map<String, List<String>> defaultMappings = KindFilenameMapperUtil.loadMappings();

        // then
        assertThat(defaultMappings)
          .containsOnly(
            entry("BuildConfig", Arrays.asList("bc", "buildconfig")),
            entry("ClusterRole", Arrays.asList("cr", "crole", "clusterrole")),
            entry("ConfigMap", Arrays.asList("cm", "configmap")),
            entry("CronJob", Arrays.asList("cj", "cronjob")),
            entry("Pod", Arrays.asList("pd", "pod"))
          );
    }
}
