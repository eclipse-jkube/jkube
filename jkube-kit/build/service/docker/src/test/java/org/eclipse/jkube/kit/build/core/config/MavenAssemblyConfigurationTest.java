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
import org.apache.maven.plugins.assembly.model.Assembly;
import org.eclipse.jkube.kit.common.JkubeProjectAssembly;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class MavenAssemblyConfigurationTest {

    @Test
    public void testBuilder() {
        // When
        final JkubeAssemblyConfiguration result = new JkubeAssemblyConfiguration.Builder()
                .assemblyDef(Collections.singletonList(new JkubeProjectAssembly(new File("target"), Arrays.asList("docker"), "755")))
                .user("super-user")
                .build();
        // Then
        assertThat(result.getUser(), equalTo("super-user"));
        //assertThat(result.getInline().getId(), equalTo("1337"));
    }
}
