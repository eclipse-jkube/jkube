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
package org.eclipse.jkube.kit.build.api.assembly;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;

import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class AssemblyConfigurationSourceTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    private File buildDirectory;

    private AssemblyConfiguration assemblyConfig;

    @Before
    public void setup() throws Exception {
        buildDirectory = temporaryFolder.newFolder("build");
        // set 'ignorePermissions' to something other then default
        this.assemblyConfig = AssemblyConfiguration.builder()
                .permissionsString("keep")
                .build();
    }

    @Test
    public void permissionMode() {
        try {
            AssemblyConfiguration.builder().permissionsString("blub").build();
        } catch (IllegalArgumentException exp) {
            assertTrue(exp.getMessage().contains("blub"));
        }

        AssemblyConfiguration config = AssemblyConfiguration.builder().permissionsString("ignore").build();
        assertSame(AssemblyConfiguration.PermissionMode.ignore, config.getPermissions());
    }

    @Test
    public void testCreateSourceAbsolute() {
        testCreateSource(buildBuildContext("/src/docker".replace("/", File.separator), "/output/docker".replace("/", File.separator)));
    }

    @Test
    public void testCreateSourceRelative() {
        testCreateSource(buildBuildContext("src/docker".replace("/", File.separator), "output/docker".replace("/", File.separator)));
    }

    @Test
    public void testOutputDirHasImage() {
        String image = "image";
        JKubeConfiguration context = buildBuildContext("src/docker", "output/docker");
        AssemblyConfigurationSource source = new AssemblyConfigurationSource(context,
                new BuildDirs(image, context), assemblyConfig);

        assertTrue(containsDir(image, source.getOutputDirectory()));
        assertTrue(containsDir(image, source.getWorkingDirectory()));
        assertTrue(containsDir(image, source.getTemporaryRootDirectory()));
    }

    private JKubeConfiguration buildBuildContext(String sourceDir, String outputDir) {
        return JKubeConfiguration.builder()
                .project(JavaProject.builder().buildDirectory(buildDirectory).build())
                .sourceDirectory(sourceDir)
                .outputDirectory(outputDir)
                .build();
    }

    private void testCreateSource(JKubeConfiguration context) {
        AssemblyConfigurationSource source =
                new AssemblyConfigurationSource(context, new BuildDirs("image", context), assemblyConfig);

        assertFalse("we must not ignore permissions when creating the archive", source.isIgnorePermissions());

        String outputDir = context.getOutputDirectory();

        assertStartsWithDir(outputDir, source.getOutputDirectory());
        assertStartsWithDir(outputDir, source.getWorkingDirectory());
        assertStartsWithDir(outputDir, source.getTemporaryRootDirectory());
    }

    private boolean containsDir(String outputDir, File path) {
        return path.toString().contains(outputDir + File.separator);
    }

    private void assertStartsWithDir(String outputDir, File path) {
        String expectedStartsWith = outputDir + File.separator;
        int length = expectedStartsWith.length();
        assertEquals(expectedStartsWith, path.toString().substring(0, length));
    }

    @Test
    public void testReactorProjects() {

        JavaProject project1 = JavaProject.builder().build();
        JavaProject project2 = JavaProject.builder().build();

        JKubeConfiguration buildContext = JKubeConfiguration.builder()
                .sourceDirectory("/src/docker")
                .outputDirectory("/output/docker")
                .reactorProjects(Arrays.asList(project1, project2))
                .build();
        AssemblyConfigurationSource source = new AssemblyConfigurationSource(buildContext,null,null);
        assertEquals(2, source.getReactorProjects().size());
    }
}

