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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import com.google.cloud.tools.jib.api.CacheDirectoryCreationException;
import com.google.cloud.tools.jib.api.Containerizer;
import com.google.cloud.tools.jib.api.Credential;
import com.google.cloud.tools.jib.api.Jib;
import com.google.cloud.tools.jib.api.RegistryException;
import com.google.cloud.tools.jib.api.RegistryImage;
import com.google.cloud.tools.jib.api.TarImage;
import org.eclipse.jkube.kit.build.api.assembly.BuildDirs;
import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.AssemblyFile;
import org.eclipse.jkube.kit.common.AssemblyFileEntry;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.common.Arguments;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;

import com.google.cloud.tools.jib.api.JibContainerBuilder;
import com.google.cloud.tools.jib.api.buildplan.AbsoluteUnixPath;
import com.google.cloud.tools.jib.api.buildplan.FileEntriesLayer;
import com.google.cloud.tools.jib.api.buildplan.ImageFormat;
import com.google.cloud.tools.jib.api.buildplan.Port;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.eclipse.jkube.kit.service.jib.JibServiceUtil.containerFromImageConfiguration;
import static org.mockito.Answers.RETURNS_SELF;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstructionWithAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JibServiceUtilTest {
    private KitLogger kitLogger;

    @BeforeEach
    void setUp() {
        kitLogger = new KitLogger.SilentLogger();
    }

    @Test
    void testGetBaseImageWithNullBuildConfig() {
        assertThat(JibServiceUtil.getBaseImage(ImageConfiguration.builder().build(), null)).isEqualTo("busybox:latest");
    }

    @Test
    void testGetBaseImageWithNotNullBuildConfig() {
        // Given
        final ImageConfiguration imageConfiguration = ImageConfiguration.builder()
            .build(BuildConfiguration.builder()
                .from("quay.io/jkubeio/jkube-test-image:0.0.1")
                .build())
            .build();
        // When
        final String result = JibServiceUtil.getBaseImage(imageConfiguration, null);
        // Then
        assertThat(result).isEqualTo("quay.io/jkubeio/jkube-test-image:0.0.1");
    }

    @Test
    void testContainerFromImageConfiguration()  throws Exception {
          try (MockedConstruction<JibContainerBuilder> ignore = mockConstructionWithAnswer(JibContainerBuilder.class, RETURNS_SELF)) {
            // Given
            ImageConfiguration imageConfiguration = getSampleImageConfiguration();
            // When
            JibContainerBuilder jibContainerBuilder = containerFromImageConfiguration(imageConfiguration, null, null);
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
    void testAppendOriginalImageNameTagIfApplicable() {
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
    void testGetFullImageNameWithDefaultTag() {
        assertThat(JibServiceUtil.getFullImageName(getSampleImageConfiguration(), null))
            .isEqualTo("test/test-project:latest");
    }

    @Test
    void testGetFullImageNameWithProvidedTag() {
        assertThat(JibServiceUtil.getFullImageName(getSampleImageConfiguration(), "0.0.1"))
            .isEqualTo("test/test-project:0.0.1");
    }

    @Test
    void layers_withEmptyLayers_shouldReturnEmpty() {
        // When
        final List<FileEntriesLayer> result = JibServiceUtil.layers(null, Collections.emptyMap());
        // Then
        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    void layers_withMultipleLayers_shouldReturnTransformedLayers(@TempDir File temporaryFolder) throws IOException {
        // Given
        final BuildDirs buildDirs = new BuildDirs("layers-test", JKubeConfiguration.builder()
            .outputDirectory("target/docker")
            .project(JavaProject.builder().baseDirectory(temporaryFolder).build())
            .build());
        final Map<Assembly, List<AssemblyFileEntry>> originalLayers = new LinkedHashMap<>();
        originalLayers.put(Assembly.builder().id("layer-1").build(), Arrays.asList(
            AssemblyFileEntry.builder().source(File.createTempFile("junit", "ext", temporaryFolder))
                .dest(buildDirs.getOutputDirectory().toPath().resolve("layer-1").resolve("l1.1.txt").toFile()).build(),
            AssemblyFileEntry.builder().source(File.createTempFile("junit", "ext", temporaryFolder))
                .dest(buildDirs.getOutputDirectory().toPath().resolve("layer-1").resolve("l1.2.txt").toFile()).build()
        ));
        originalLayers.put(Assembly.builder().build(), Arrays.asList(
            AssemblyFileEntry.builder().source(File.createTempFile("junit", "ext", temporaryFolder))
                .dest(new File(buildDirs.getOutputDirectory(),"l2.1.txt")).build(),
            AssemblyFileEntry.builder().source(File.createTempFile("junit", "ext", temporaryFolder))
                .dest(new File(buildDirs.getOutputDirectory(),"l2.2.txt")).build()
        ));
        // Creates a denormalized path in JDK 8
        originalLayers.put(Assembly.builder().id("jkube-generated-layer-final-artifact").build(), Collections.singletonList(
            AssemblyFileEntry.builder().source(File.createTempFile("junit", "ext", temporaryFolder))
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

    @Test
    void buildContainer_whenBuildSuccessful_thenDelegateToJibContainerize() throws InterruptedException, CacheDirectoryCreationException, IOException, ExecutionException, RegistryException {
        try (MockedStatic<Containerizer> containerizerMockedStatic = mockStatic(Containerizer.class)) {
            // Given
            JibContainerBuilder jibContainerBuilder = mock(JibContainerBuilder.class, RETURNS_SELF);
            Containerizer containerizer = mock(Containerizer.class, RETURNS_SELF);
            TarImage tarImage = TarImage.at(new File("docker-build.tar").toPath());
            containerizerMockedStatic.when(() -> Containerizer.to(tarImage)).thenReturn(containerizer);

            // When
            JibServiceUtil.buildContainer(jibContainerBuilder, tarImage, kitLogger);

            // Then
            verify(containerizer).setAllowInsecureRegistries(true);
            verify(containerizer).setExecutorService(any(ExecutorService.class));
            verify(containerizer, times(2)).addEventHandler(any(), any());
            verify(jibContainerBuilder).containerize(containerizer);
        }
    }

    @Test
    void buildContainer_whenBuildFailure_thenThrowException() throws InterruptedException, CacheDirectoryCreationException, IOException, ExecutionException, RegistryException {
        try (MockedStatic<Containerizer> containerizerMockedStatic = mockStatic(Containerizer.class)) {
            // Given
            JibContainerBuilder jibContainerBuilder = mock(JibContainerBuilder.class, RETURNS_SELF);
            Containerizer containerizer = mock(Containerizer.class, RETURNS_SELF);
            TarImage tarImage = TarImage.at(new File("docker-build.tar").toPath());
            containerizerMockedStatic.when(() -> Containerizer.to(tarImage)).thenReturn(containerizer);
            when(jibContainerBuilder.containerize(containerizer)).thenThrow(new RegistryException("Unable to pull base image"));

            // When
            assertThatIllegalStateException()
                .isThrownBy(() -> JibServiceUtil.buildContainer(jibContainerBuilder, tarImage, kitLogger))
                .withMessageContaining("Unable to pull base image");

            // Then
            verify(containerizer).setAllowInsecureRegistries(true);
            verify(containerizer).setExecutorService(any(ExecutorService.class));
            verify(containerizer, times(2)).addEventHandler(any(), any());
            verify(jibContainerBuilder).containerize(containerizer);
        }
    }

    @Test
    void jibPush_whenPushFailed_thenThrowException() throws CacheDirectoryCreationException, IOException, ExecutionException, InterruptedException, RegistryException {
        try (MockedStatic<Containerizer> containerizerMockedStatic = mockStatic(Containerizer.class);
             MockedStatic<Jib> jibMockedStatic = mockStatic(Jib.class)) {
            // Given
            JibContainerBuilder jibContainerBuilder = mock(JibContainerBuilder.class, RETURNS_SELF);
            Containerizer containerizer = mock(Containerizer.class, RETURNS_SELF);
            jibMockedStatic.when(() -> Jib.from(any(TarImage.class))).thenReturn(jibContainerBuilder);
            containerizerMockedStatic.when(() -> Containerizer.to(any(RegistryImage.class))).thenReturn(containerizer);
            when(jibContainerBuilder.containerize(containerizer)).thenThrow(new RegistryException("Unauthorized"));
            ImageConfiguration imageConfiguration = getSampleImageConfiguration();
            Credential credential = Credential.from("testuser", "secret");
            File tarArchive = new File("docker-build.tar");

            // When + Then
            assertThatIllegalStateException()
                .isThrownBy(() -> JibServiceUtil.jibPush(imageConfiguration, credential, tarArchive, kitLogger))
                .withMessage("Unauthorized");
        }
    }

    @Test
    void jibPush_whenNoTagsInBuildConfig_thenNoAdditionalTagsAddedToContainerizer() throws CacheDirectoryCreationException, IOException, ExecutionException, InterruptedException, RegistryException {
        try (MockedStatic<Containerizer> containerizerMockedStatic = mockStatic(Containerizer.class);
             MockedStatic<Jib> jibMockedStatic = mockStatic(Jib.class)) {
            // Given
            JibContainerBuilder jibContainerBuilder = mock(JibContainerBuilder.class, RETURNS_SELF);
            Containerizer containerizer = mock(Containerizer.class, RETURNS_SELF);
            jibMockedStatic.when(() -> Jib.from(any(TarImage.class))).thenReturn(jibContainerBuilder);
            containerizerMockedStatic.when(() -> Containerizer.to(any(RegistryImage.class))).thenReturn(containerizer);
            ImageConfiguration imageConfiguration = getSampleImageConfiguration();
            Credential credential = Credential.from("testuser", "secret");
            File tarArchive = new File("docker-build.tar");

            // When
            JibServiceUtil.jibPush(imageConfiguration, credential, tarArchive, kitLogger);

            // Then
            verify(containerizer, times(0)).withAdditionalTag(anyString());
            verify(jibContainerBuilder).containerize(containerizer);
        }
    }

    @Test
    void jibPush_whenAdditionalTagsInBuildConfig_thenAdditionalTagsAddedToContainerizer() throws CacheDirectoryCreationException, IOException, ExecutionException, InterruptedException, RegistryException {
        try (MockedStatic<Containerizer> containerizerMockedStatic = mockStatic(Containerizer.class);
             MockedStatic<Jib> jibMockedStatic = mockStatic(Jib.class)) {
            // Given
            JibContainerBuilder jibContainerBuilder = mock(JibContainerBuilder.class, RETURNS_SELF);
            Containerizer containerizer = mock(Containerizer.class, RETURNS_SELF);
            jibMockedStatic.when(() -> Jib.from(any(TarImage.class))).thenReturn(jibContainerBuilder);
            containerizerMockedStatic.when(() -> Containerizer.to(any(RegistryImage.class))).thenReturn(containerizer);
            ImageConfiguration imageConfiguration = getSampleImageConfiguration();
            imageConfiguration = imageConfiguration.toBuilder()
                .build(imageConfiguration.getBuild().toBuilder()
                    .tags(Arrays.asList("t1", "t2", "t3"))
                    .build())
                .build();
            Credential credential = Credential.from("testuser", "secret");
            File tarArchive = new File("docker-build.tar");

            // When
            JibServiceUtil.jibPush(imageConfiguration, credential, tarArchive, kitLogger);

            // Then
            verify(containerizer).withAdditionalTag("t1");
            verify(containerizer).withAdditionalTag("t2");
            verify(containerizer).withAdditionalTag("t3");
            verify(jibContainerBuilder).containerize(containerizer);
        }
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
