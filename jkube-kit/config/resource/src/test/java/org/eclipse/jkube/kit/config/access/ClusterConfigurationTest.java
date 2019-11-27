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
package org.eclipse.jkube.kit.config.access;

import org.junit.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class ClusterConfigurationTest {

    @Test
    public void should_lod_coniguration_from_properties() {

        // Given
        final ClusterConfiguration.Builder clusterConfigurationBuilder = new ClusterConfiguration.Builder();
        final Properties properties = new Properties();
        properties.put("jkube.username", "aaa");
        properties.put("jkube.password", "bbb");

        // When
        final ClusterConfiguration clusterConfiguration = clusterConfigurationBuilder.from(properties).build();

        // Then
        assertThat(clusterConfiguration.getConfig().getUsername()).isEqualTo("aaa");
        assertThat(clusterConfiguration.getConfig().getPassword()).isEqualTo("bbb");
    }

}
