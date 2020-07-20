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

import com.google.cloud.tools.jib.api.AbsoluteUnixPath;
import com.google.cloud.tools.jib.api.JibContainerBuilder;
import com.google.cloud.tools.jib.api.LayerConfiguration;
import com.google.cloud.tools.jib.api.Port;
import mockit.Mocked;
import mockit.Verifications;
import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.AssemblyFile;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.Arguments;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.eclipse.jkube.kit.service.jib.JibServiceUtil.BUSYBOX;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JibServiceUtilTest {
    @Test
    public void testGetBaseImageWithNullBuildConfig() {
        assertEquals(BUSYBOX, JibServiceUtil.getBaseImage(ImageConfiguration.builder().build()));
    }

    @Test
    public void testGetBaseImageWithNotNullBuildConfig() {
        assertEquals("quay.io/jkubeio/jkube-test-image:0.0.1", JibServiceUtil.getBaseImage(ImageConfiguration.builder()
                .build(BuildConfiguration.builder()
                        .from("quay.io/jkubeio/jkube-test-image:0.0.1")
                        .build())
                .build()));
    }

    @Test
    public void testContainerFromImageConfiguration(@Mocked JibContainerBuilder containerBuilder) {
        // Given
        ImageConfiguration imageConfiguration = getSampleImageConfiguration();

        // When
        JibContainerBuilder jibContainerBuilder = JibServiceUtil.populateContainerBuilderFromImageConfiguration(containerBuilder, imageConfiguration);

        // Then
        assertNotNull(jibContainerBuilder);
        new Verifications() {{
            jibContainerBuilder.addLabel("foo", "bar");
            times = 1;
            jibContainerBuilder.setEntrypoint(Arrays.asList("java", "-jar", "foo.jar"));
            times = 1;
            jibContainerBuilder.setExposedPorts(new HashSet<>(Collections.singletonList(Port.tcp(8080))));
            times = 1;
            jibContainerBuilder.setUser("root");
            times = 1;
            jibContainerBuilder.setWorkingDirectory(AbsoluteUnixPath.get("/home/foo"));
            times = 1;
            jibContainerBuilder.setVolumes(new HashSet<>(Collections.singletonList(AbsoluteUnixPath.get("/mnt/volume1"))));
            times = 1;
        }};
    }

    @Test
    public void testCopyToContainer(@Mocked JibContainerBuilder containerBuilder) throws IOException {
        // Given
        File temporaryDirectory = Files.createTempDirectory("jib-test").toFile();
        File temporaryFile = new File(temporaryDirectory, "foo.txt");
        boolean wasNewFileCreated = temporaryFile.createNewFile();

        // When
        JibServiceUtil.copyToContainer(containerBuilder, temporaryDirectory, "/tmp", Collections.emptyMap());

        // Then
        assertTrue(wasNewFileCreated);
        new Verifications() {{
            LayerConfiguration layerConfiguration;
            containerBuilder.addLayer(layerConfiguration = withCapture());

            assertNotNull(layerConfiguration);
            assertEquals(1, layerConfiguration.getLayerEntries().size());
            assertEquals(temporaryFile.toPath(), layerConfiguration.getLayerEntries().get(0).getSourceFile());
            assertEquals(AbsoluteUnixPath.get(temporaryFile.getAbsolutePath().substring(4)), layerConfiguration.getLayerEntries().get(0).getExtractionPath());
        }};
    }

    @Test
    public void testAppendOriginalImageNameTagIfApplicable() {
        // Given
        List<String> imageTagList = Arrays.asList("0.0.1", "0.0.1-SNAPSHOT");

        // When
        Set<String> result = JibServiceUtil.getAllImageTags(imageTagList, "test-project");

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertArrayEquals(new String[]{"0.0.1-SNAPSHOT", "0.0.1", "latest"}, result.toArray());
    }

    @Test
    public void testGetFullImageNameWithDefaultTag() {
        assertEquals("test/test-project:latest", JibServiceUtil.getFullImageName(getSampleImageConfiguration(), null));
    }

    @Test
    public void testGetFullImageNameWithProvidedTag() {
        assertEquals("test/test-project:0.0.1", JibServiceUtil.getFullImageName(getSampleImageConfiguration(), "0.0.1"));
    }

    private ImageConfiguration getSampleImageConfiguration() {
        return ImageConfiguration.builder()
                .name("test/test-project")
                .build(BuildConfiguration.builder()
                        .from("quay.io/test/testimage:testtag")
                        .assembly(AssemblyConfiguration.builder()
                                .inline(Assembly.builder()
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