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
package org.eclipse.jkube.kit.build.maven.config;

import mockit.Expectations;
import mockit.Mocked;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class MavenAssemblyConfigurationTest {

    @Test
    public void testBuilder(@Mocked Assembly mockAssembly) {
        // Given
        new Expectations() {{
           mockAssembly.getId();
           result = "1337";
        }};
        // When
        final MavenAssemblyConfiguration result = new MavenAssemblyConfiguration.Builder()
                .assemblyDef(mockAssembly)
                .user("super-user")
                .build();
        // Then
        assertThat(result.getUser(), equalTo("super-user"));
        assertThat(result.getInline().getId(), equalTo("1337"));
    }
}
