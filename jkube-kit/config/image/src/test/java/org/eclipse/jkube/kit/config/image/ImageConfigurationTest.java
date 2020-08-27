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
package org.eclipse.jkube.kit.config.image;

import mockit.Expectations;
import mockit.Mocked;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class ImageConfigurationTest {

    @Test
    public void testBuilder(@Mocked BuildConfiguration mockJKubeBuildConfiguration) {
        // Given
        new Expectations() {{
            mockJKubeBuildConfiguration.getUser();
            result = "super-user";
        }};
        // When
        final ImageConfiguration result = ImageConfiguration.builder()
                .name("1337")
                .build(mockJKubeBuildConfiguration)
                .build();
        // Then
        assertThat(result.getName(), equalTo("1337"));
        assertThat(result.getBuildConfiguration().getUser(), equalTo("super-user"));
    }
}
