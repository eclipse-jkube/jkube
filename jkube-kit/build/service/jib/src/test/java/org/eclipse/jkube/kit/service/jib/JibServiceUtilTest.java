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
package org.eclipse.jkube.kit.service.jib;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jkube.kit.build.api.assembly.BuildDirs;
import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.AssemblyFile;
import org.eclipse.jkube.kit.common.AssemblyFileEntry;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.Arguments;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;

import com.google.cloud.tools.jib.api.JibContainerBuilder;
import com.google.cloud.tools.jib.api.buildplan.AbsoluteUnixPath;
import com.google.cloud.tools.jib.api.buildplan.FileEntriesLayer;
import com.google.cloud.tools.jib.api.buildplan.ImageFormat;
import com.google.cloud.tools.jib.api.buildplan.Port;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedConstruction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.kit.service.jib.JibServiceUtil.containerFromImageConfiguration;
import static org.mockito.Answers.RETURNS_SELF;
import static org.mockito.Mockito.mockConstructionWithAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class JibServiceUtilTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testGetBaseImageWithNullBuildConfig() {
        assertThat(JibServiceUtil.getBaseImage(ImageConfiguration.builder().build())).isEqualTo("busybox:latest");
    }

    @Test
    public void testGetBaseImageWithNotNullBuildConfig() {
        // Given
        final ImageConfiguration imageConfiguration = ImageConfiguration.builder()
            .build(BuildConfiguration.builder()
                .from("quay.io/jkubeio/jkube-test-image:0.0.1")
                .build())
            .build();
        // When
        final String result = JibServiceUtil.getBaseImage(imageConfiguration);
        // Then
        assertThat(result).isEqualTo("quay.io/jkubeio/jkube-test-image:0.0.1");
    }

    @Test
    public void testContainerFromImageConfiguration()  throws Exception {
          try (MockedConstruction<JibContainerBuilder> ignore = mockConstructionWithAnswer(JibContainerBuilder.class, RETURNS_SELF)) {
            // Given
            ImageConfiguration imageConfiguration = getSampleImageConfiguration();
            // When
            JibContainerBuilder jibContainerBuilder = containerFromImageConfiguration(imageConfiguration, null);
            // Then
            verify(jibContainerBuilder, times(1)).addLabel("foo", "bar");
            verify(jibContainerBuilder, times(1)).setEntrypoint(Arrays.asList("java", "-jar", "foo.jar"));
            verify(jibContainerBuilder, times(1)).setExposedPorts(new HashSet<>(Collections.singletonList(Port.tcp(8080))));
            verify(jibContainerBuilder, times(1)).setUser("root");
            verify(jibContainerBuilder, times(1)).setWorkingDirectory(AbsoluteUnixPath.get("/home/foo"));
            verify(jibContainerBuilder, times(1)).setVolumes(new HashSet<>(Collections.singletonList(AbsoluteUnixPath.get("/mnt/volume1"))));
            verify(jibContainerBuilder, times(1)).setFormat(ImageFormat.Docker);
        }

    }

    @Test
    public void testAppendOriginalImageNameTagIfApplicable() {
        // Given
        List<String> imageTagList = Arrays.asList("0.0.1", "0.0.1-SNAPSHOT");
        // When
        Set<String> result = JibServiceUtil.getAllImageTags(imageTagList, "test-project");
        // Then
        assertThat(result)
            .isNotNull()
            .hasSize(3)
            .containsExactlyInAnyOrder("0.0.1-SNAPSHOT", "0.0.1", "latest");
    }

    @Test
    public void testGetFullImageNameWithDefaultTag() {
        assertThat(JibServiceUtil.getFullImageName(getSampleImageConfiguration(), null))
            .isEqualTo("test/test-project:latest");
    }

    @Test
    public void testGetFullImageNameWithProvidedTag() {
        assertThat(JibServiceUtil.getFullImageName(getSampleImageConfiguration(), "0.0.1"))
            .isEqualTo("test/test-project:0.0.1");
    }

    @Test
    public void layers_withEmptyLayers_shouldReturnEmpty() {
        // When
        final List<FileEntriesLayer> result = JibServiceUtil.layers(null, Collections.emptyMap());
        // Then
        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    public void layers_withMultipleLayers_shouldReturnTransformedLayers() throws IOException {
        // Given
        final BuildDirs buildDirs = new BuildDirs("layers-test", JKubeConfiguration.builder()
            .outputDirectory("target/docker")
            .project(JavaProject.builder().baseDirectory(temporaryFolder.getRoot()).build())
            .build());
        final Map<Assembly, List<AssemblyFileEntry>> originalLayers = new LinkedHashMap<>();
        originalLayers.put(Assembly.builder().id("layer-1").build(), Arrays.asList(
            AssemblyFileEntry.builder().source(temporaryFolder.newFile())
                .dest(buildDirs.getOutputDirectory().toPath().resolve("layer-1").resolve("l1.1.txt").toFile()).build(),
            AssemblyFileEntry.builder().source(temporaryFolder.newFile())
                .dest(buildDirs.getOutputDirectory().toPath().resolve("layer-1").resolve("l1.2.txt").toFile()).build()
        ));
        originalLayers.put(Assembly.builder().build(), Arrays.asList(
            AssemblyFileEntry.builder().source(temporaryFolder.newFile())
                .dest(new File(buildDirs.getOutputDirectory(),"l2.1.txt")).build(),
            AssemblyFileEntry.builder().source(temporaryFolder.newFile())
                .dest(new File(buildDirs.getOutputDirectory(),"l2.2.txt")).build()
        ));
        // Creates a denormalized path in JDK 8
        originalLayers.put(Assembly.builder().id("jkube-generated-layer-final-artifact").build(), Collections.singletonList(
            AssemblyFileEntry.builder().source(temporaryFolder.newFile())
                    .dest(buildDirs.getOutputDirectory().toPath().resolve("jkube-generated-layer-final-artifact")
                        .resolve("deployments").resolve(".").resolve("edge.case").toFile()).build()
        ));
        // When
        final List<FileEntriesLayer> result = JibServiceUtil.layers(buildDirs, originalLayers);
        // Then
        assertThat(result).hasSize(3)
            .anySatisfy(fel -> assertThat(fel)
                .hasFieldOrPropertyWithValue("name", "layer-1")
                .extracting(FileEntriesLayer::getEntries).asList().extracting("extractionPath.unixPath")
                .containsExactly("/l1.1.txt", "/l1.2.txt")
            )
            .anySatisfy(fel -> assertThat(fel)
                .hasFieldOrPropertyWithValue("name", "")
                .extracting(FileEntriesLayer::getEntries).asList().extracting("extractionPath.unixPath")
                .containsExactly("/l2.1.txt", "/l2.2.txt")
            )
            .anySatisfy(fel -> assertThat(fel)
                .hasFieldOrPropertyWithValue("name", "jkube-generated-layer-final-artifact")
                .extracting(FileEntriesLayer::getEntries).asList().extracting("extractionPath.unixPath")
                .containsExactly("/deployments/edge.case")
            )
            .extracting(FileEntriesLayer::getName)
            .containsExactly("layer-1", "", "jkube-generated-layer-final-artifact");
    }

    private ImageConfiguration getSampleImageConfiguration() {
        return ImageConfiguration.builder()
                .name("test/test-project")
                .build(BuildConfiguration.builder()
                        .from("quay.io/test/testimage:testtag")
                        .assembly(AssemblyConfiguration.builder()
                                .layer(Assembly.builder()
                                        .files(Collections.singletonList(AssemblyFile.builder()
                                                .source(new File("${project.basedir}/foo"))
                                                .outputDirectory(new File("targetDir"))
                                                .build()))
                                        .build())
                                .build())
                        .entryPoint(Arguments.builder().exec(Arrays.asList("java", "-jar", "foo.jar")).build())
                        .labels(Collections.singletonMap("foo", "bar"))
                        .user("root")
                        .workdir("/home/foo")
                        .ports(Collections.singletonList("8080"))
                        .volume("/mnt/volume1")
                        .build())
                .build();
    }
}
