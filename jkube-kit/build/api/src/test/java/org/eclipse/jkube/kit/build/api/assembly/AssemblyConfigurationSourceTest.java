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
package org.eclipse.jkube.kit.build.api.assembly;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class AssemblyConfigurationSourceTest {

    private File buildDirectory;

    private AssemblyConfiguration assemblyConfig;

    @BeforeEach
    void setup(@TempDir Path temporaryFolder) throws Exception {
        buildDirectory = Files.createDirectory(temporaryFolder.resolve("build")).toFile();
        // set 'ignorePermissions' to something other than default
        this.assemblyConfig = AssemblyConfiguration.builder()
            .permissionsString("keep")
            .build();
    }

    @Test
    void permissionMode_invalid() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> AssemblyConfiguration.builder().permissionsString("blub").build())
          .withMessageContaining("blub");
    }

    @Test
    void permissionsMode_ignore() {
      AssemblyConfiguration config = AssemblyConfiguration.builder().permissionsString("ignore").build();
      assertThat(config.getPermissions()).isSameAs(AssemblyConfiguration.PermissionMode.ignore);
    }

    @Test
    void testCreateSourceAbsolute() {
        testCreateSource(buildBuildContext("/src/docker".replace("/", File.separator), "/output/docker".replace("/", File.separator)));
    }

    @Test
    void testCreateSourceRelative() {
        testCreateSource(buildBuildContext("src/docker".replace("/", File.separator), "output/docker".replace("/", File.separator)));
    }

    @Test
    void testOutputDirHasImage() {
        String image = "image";
        JKubeConfiguration context = buildBuildContext("src/docker", "output/docker");
        AssemblyConfigurationSource source = new AssemblyConfigurationSource(context,
                new BuildDirs(image, context), assemblyConfig);

        assertThat(containsDir(image, source.getOutputDirectory())).isTrue();
        assertThat(containsDir(image, source.getWorkingDirectory())).isTrue();
        assertThat(containsDir(image, source.getTemporaryRootDirectory())).isTrue();
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

        assertThat(source.isIgnorePermissions()).isFalse();

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
        assertThat(path.toString().substring(0, length)).isEqualTo(expectedStartsWith);
    }

    @Test
    void testReactorProjects() {

        JavaProject project1 = JavaProject.builder().build();
        JavaProject project2 = JavaProject.builder().build();

        JKubeConfiguration buildContext = JKubeConfiguration.builder()
                .sourceDirectory("/src/docker")
                .outputDirectory("/output/docker")
                .reactorProjects(Arrays.asList(project1, project2))
                .build();
        AssemblyConfigurationSource source = new AssemblyConfigurationSource(buildContext,null,null);
        assertThat(source.getReactorProjects()).hasSize(2);
    }
}

