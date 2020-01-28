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

import org.eclipse.jkube.kit.build.core.JkubeBuildContext;
import org.eclipse.jkube.kit.build.core.config.JkubeAssemblyConfiguration;
import org.eclipse.jkube.kit.common.JkubeProject;
import org.eclipse.jkube.kit.config.image.build.AssemblyConfiguration;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class DockerAssemblyConfigurationSourceTest {

    private JkubeAssemblyConfiguration assemblyConfig;

    @Before
    public void setup() {
        // set 'ignorePermissions' to something other then default
        this.assemblyConfig = new JkubeAssemblyConfiguration.Builder()
                .descriptor("assembly.xml")
                .descriptorRef("project")
                .permissions("keep")
                .build();
    }

    @Test
    public void permissionMode() {
        try {
            new AssemblyConfiguration.Builder().permissions("blub").build();
        } catch (IllegalArgumentException exp) {
            assertTrue(exp.getMessage().contains("blub"));
        }

        AssemblyConfiguration config = new AssemblyConfiguration.Builder().permissions("ignore").build();
        assertSame(AssemblyConfiguration.PermissionMode.ignore, config.getPermissions());;
    }

    @Test
    public void testCreateSourceAbsolute() {
        testCreateSource(buildBuildContetxt(".", "/src/docker".replace("/", File.separator), "/output/docker".replace("/", File.separator)));
    }

    @Test
    public void testCreateSourceRelative() {
        testCreateSource(buildBuildContetxt(".", "src/docker".replace("/", File.separator), "output/docker".replace("/", File.separator)));
    }

    @Test
    public void testOutputDirHasImage() {
        String image = "image";
        JkubeBuildContext context = buildBuildContetxt(".", "src/docker", "output/docker");
        DockerAssemblyConfigurationSource source = new DockerAssemblyConfigurationSource(context,
                new BuildDirs(image, context), assemblyConfig);

        assertTrue(containsDir(image, source.getOutputDirectory()));
        assertTrue(containsDir(image, source.getWorkingDirectory()));
        assertTrue(containsDir(image, source.getTemporaryRootDirectory()));
    }

    private JkubeBuildContext buildBuildContetxt(String projectDir, String sourceDir, String outputDir) {
        JkubeProject project = new JkubeProject.Builder().buildDirectory(projectDir).build();
        return new JkubeBuildContext.Builder()
                .project(project)
                .sourceDirectory(sourceDir)
                .outputDirectory(outputDir)
                .build();
    }

    @Test
    public void testEmptyAssemblyConfig() {
        JkubeBuildContext buildContext = new JkubeBuildContext.Builder()
                .sourceDirectory("/src/docker")
                .outputDirectory("/output/docker")
                .build();
        DockerAssemblyConfigurationSource source = new DockerAssemblyConfigurationSource(buildContext,null,null);
        assertEquals(0,source.getDescriptors().length);
    }

    private void testCreateSource(JkubeBuildContext context) {
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

        JkubeProject jkubeProject1 = new JkubeProject.Builder().build();
        JkubeProject jkubeProject2 = new JkubeProject.Builder().build();

        JkubeBuildContext buildContext = new JkubeBuildContext.Builder()
                .sourceDirectory("/src/docker")
                .outputDirectory("/output/docker")
                .reactorProjects(Arrays.asList(jkubeProject1, jkubeProject2))
                .build();
        DockerAssemblyConfigurationSource source = new DockerAssemblyConfigurationSource(buildContext,null,null);
        assertEquals(2,source.getReactorProjects().size());
    }
}

