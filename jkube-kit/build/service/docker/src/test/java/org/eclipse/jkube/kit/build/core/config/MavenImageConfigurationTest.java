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
package org.eclipse.jkube.kit.build.core.config;

import mockit.Expectations;
import mockit.Mocked;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class MavenImageConfigurationTest {

    @Test
    public void testBuilder(@Mocked JkubeBuildConfiguration mockJkubeBuildConfiguration) {
        // Given
        new Expectations() {{
            mockJkubeBuildConfiguration.getUser();
            result = "super-user";
        }};
        // When
        final MavenImageConfiguration result = new MavenImageConfiguration.Builder()
                .name("1337")
                .buildConfig(mockJkubeBuildConfiguration)
                .build();
        // Then
        assertThat(result.getName(), equalTo("1337"));
        assertThat(result.getBuildConfiguration().getUser(), equalTo("super-user"));
    }
}
