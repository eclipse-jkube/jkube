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
package org.eclipse.jkube.kit.build.core.assembly;

import java.io.File;
import java.util.Arrays;

import org.eclipse.jkube.kit.config.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.image.build.AssemblyConfiguration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class DockerAssemblyConfigurationSourceTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    private File buildDirectory;

    private AssemblyConfiguration assemblyConfig;

    @Before
    public void setup() throws Exception {
        buildDirectory = temporaryFolder.newFolder("build");
        // set 'ignorePermissions' to something other then default
        this.assemblyConfig = AssemblyConfiguration.builder()
                .descriptor("assembly.xml")
                .descriptorRef("project")
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
        DockerAssemblyConfigurationSource source = new DockerAssemblyConfigurationSource(context,
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

    @Test
    public void testEmptyAssemblyConfig() {
        JKubeConfiguration buildContext = JKubeConfiguration.builder()
                .sourceDirectory("/src/docker")
                .outputDirectory("/output/docker")
                .build();
        DockerAssemblyConfigurationSource source = new DockerAssemblyConfigurationSource(buildContext,null,null);
        assertEquals(0,source.getDescriptors().length);
    }

    private void testCreateSource(JKubeConfiguration context) {
        DockerAssemblyConfigurationSource source =
                new DockerAssemblyConfigurationSource(context, new BuildDirs("image", context), assemblyConfig);

        String[] descriptors = source.getDescriptors();
        String[] descriptorRefs = source.getDescriptorReferences();

        assertEquals("count of descriptors", 1, descriptors.length);
        assertEquals("directory of assembly", context.inSourceDir("assembly.xml").getAbsolutePath(), descriptors[0]);

        assertEquals("count of descriptors references", 1, descriptorRefs.length);
        assertEquals("reference must be project", "project", descriptorRefs[0]);

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
        DockerAssemblyConfigurationSource source = new DockerAssemblyConfigurationSource(buildContext,null,null);
        assertEquals(2, source.getReactorProjects().size());
    }
}

