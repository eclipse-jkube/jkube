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
package org.eclipse.jkube.kit.config.image;

import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class ImageConfigurationTest {

    @Test
    void testBuilder() {
        // Given
        BuildConfiguration mockJKubeBuildConfiguration = mock(BuildConfiguration.class);
        when(mockJKubeBuildConfiguration.getUser()).thenReturn("super-user");
        // When
        final ImageConfiguration result = ImageConfiguration.builder()
                .name("1337")
                .build(mockJKubeBuildConfiguration)
                .build();
        // Then
        assertThat(result.getName()).isEqualTo("1337");
        assertThat(result.getBuildConfiguration().getUser()).isEqualTo("super-user");
    }
}
