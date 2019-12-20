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
package org.eclipse.jkube.kit.build.maven;

import mockit.Capturing;
import mockit.Expectations;
import mockit.Mocked;
import org.apache.maven.project.MavenProject;
import org.eclipse.jkube.kit.common.util.ProjectClassLoaders;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ResourceMojoUtilTest {

    @Capturing
    private ProjectClassLoaders projectClassLoaders;
    @Mocked
    private MavenProject mockProject;

    @Test
    public void useDekorateHasDekorateInClassPathShouldReturnTrue() throws Exception {
        withMockProject();
        new Expectations() {{
            projectClassLoaders.isClassInCompileClasspath(true, "io.dekorate.annotation.Dekorate");
            result = true;
        }};
        final boolean result = ResourceMojoUtil.useDekorate(mockProject);
        assertTrue(result);
    }

    @Test
    public void useDekorateHasNotDekorateInClassPathShouldReturnFalse() throws Exception {
        withMockProject();
        new Expectations() {{
            projectClassLoaders.isClassInCompileClasspath(true, "io.dekorate.annotation.Dekorate");
            result = false;
        }};
        final boolean result = ResourceMojoUtil.useDekorate(mockProject);
        assertFalse(result);
    }

    private void withMockProject() throws Exception {
        new Expectations() {{
            mockProject.getCompileClasspathElements();
            result = Collections.emptyList();
            mockProject.getBuild().getOutputDirectory();
            result = "/";
        }};
    }
}
