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
package org.eclipse.jkube.kit.build.api.helper;

import org.apache.commons.io.FileUtils;
import org.eclipse.jkube.kit.build.api.helper.DockerFileUtil;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * @author roland
 */
public class DockerFileUtilTest {

    @Test
    public void testSimple() throws Exception {
        File toTest = copyToTempDir("Dockerfile_from_simple");
        assertEquals("fabric8/s2i-java", DockerFileUtil.extractBaseImages(toTest, new Properties(), BuildConfiguration.DEFAULT_FILTER).get(0));
    }

    @Test
    public void testMultiStage() throws Exception {
        File toTest = copyToTempDir("Dockerfile_multi_stage");
        Iterator<String> fromClauses = DockerFileUtil.extractBaseImages(toTest, new Properties(), BuildConfiguration.DEFAULT_FILTER).iterator();

        assertEquals("fabric8/s2i-java", fromClauses.next());
        assertEquals("fabric8/s1i-java", fromClauses.next());
        assertFalse(fromClauses.hasNext());
    }

    @Test
    public void testMultiStageNamed() throws Exception {
        File toTest = copyToTempDir("Dockerfile_multi_stage_named_build_stages");
        Iterator<String> fromClauses = DockerFileUtil.extractBaseImages(toTest, new Properties(), BuildConfiguration.DEFAULT_FILTER).iterator();

        assertEquals("fabric8/s2i-java", fromClauses.next());
        assertFalse(fromClauses.hasNext());
    }

    @Test
    public void testMultiStageNamedWithDuplicates() throws Exception {
        File toTest = copyToTempDir("Dockerfile_multi_stage_named_redundant_build_stages");
        Iterator<String> fromClauses = DockerFileUtil.extractBaseImages(toTest, new Properties(), BuildConfiguration.DEFAULT_FILTER).iterator();

        assertEquals("centos", fromClauses.next());
        assertFalse(fromClauses.hasNext());

    }

    private File copyToTempDir(String resource) throws IOException {
        File dir = Files.createTempDirectory("d-m-p").toFile();
        File ret = new File(dir, "Dockerfile");
        try (OutputStream os = new FileOutputStream(ret)) {
            FileUtils.copyFile(new File(getClass().getResource(resource).getPath()), os);
        }
        return ret;
    }

    @Test
    public void interpolate() throws Exception {
        Properties projectProperties = new Properties();
        projectProperties.put("base", "java");
        projectProperties.put("name", "guenther");
        projectProperties.put("age", "42");
        projectProperties.put("ext", "png");
        projectProperties.put("project.artifactId", "docker-maven-plugin");
        projectProperties.put("cliOverride", "cliValue"); // Maven CLI override: -DcliOverride=cliValue
        projectProperties.put("user.name", "somebody"); // Java system property: -Duser.name=somebody
        File dockerFile = getDockerfilePath("interpolate");
        File expectedDockerFile = new File(dockerFile.getParent(), dockerFile.getName() + ".expected");
        File actualDockerFile = createTmpFile(dockerFile.getName());
        FileUtils.write(actualDockerFile, DockerFileUtil.interpolate(dockerFile, projectProperties, BuildConfiguration.DEFAULT_FILTER), "UTF-8");
        // Compare text lines without regard to EOL delimiters
        assertEquals(FileUtils.readLines(expectedDockerFile, StandardCharsets.UTF_8), FileUtils.readLines(actualDockerFile, StandardCharsets.UTF_8));
    }

    @Test
    public void testCreateSimpleDockerfileConfig() throws IOException {
        // Given
        File dockerFile = File.createTempFile("Dockerfile", "-test");
        // When
        ImageConfiguration imageConfiguration1 = DockerFileUtil.createSimpleDockerfileConfig(dockerFile, null);
        ImageConfiguration imageConfiguration2 = DockerFileUtil.createSimpleDockerfileConfig(dockerFile, "someImage:0.0.1");
        // Then
        assertNotNull(imageConfiguration1);
        assertEquals("%g/%a:%l", imageConfiguration1.getName());
        assertEquals(dockerFile.getPath(), imageConfiguration1.getBuild().getDockerFileRaw());
        assertNotNull(imageConfiguration2);
        assertEquals("someImage:0.0.1", imageConfiguration2.getName());
        assertEquals(dockerFile.getPath(), imageConfiguration2.getBuild().getDockerFileRaw());
    }

    @Test
    public void testAddSimpleDockerfileConfig() throws IOException {
        // Given
        ImageConfiguration imageConfiguration = ImageConfiguration.builder()
                .name("test-image")
                .build();
        File dockerFile = File.createTempFile("Dockerfile", "-test");

        // When
        ImageConfiguration result = DockerFileUtil.addSimpleDockerfileConfig(imageConfiguration, dockerFile);

        // Then
        assertNotNull(result.getBuild());
        assertEquals(dockerFile.getPath(), result.getBuild().getDockerFileRaw());
    }

    @Test
    public void testCustomInterpolation() throws IOException {
        // Given
        Map<File, String> input = new HashMap<>();
        input.put(new File(getClass().getResource("/interpolate/at/Dockerfile_1").getFile()), "@");
        input.put(new File(getClass().getResource("/interpolate/var/Dockerfile_1").getFile()), "${*}");
        input.put(new File(getClass().getResource("/interpolate/none/Dockerfile_1").getFile()), "false");
        Properties projectProperties = new Properties();
        projectProperties.put("base", "java");
        projectProperties.put("name", "guenther");
        projectProperties.put("age", "42");
        projectProperties.put("ext", "png");
        projectProperties.put("cliOverride", "cliValue"); // Maven CLI override: -DcliOverride=cliValue
        projectProperties.put("user.name", "somebody");
        projectProperties.put("project.artifactId", "eclipse-jkube");

        // When
        for (Map.Entry<File, String> e : input.entrySet()) {
            String value = DockerFileUtil.interpolate(e.getKey(), projectProperties, e.getValue());
            File expectedDockerfile = new File(e.getKey().getParent(), "Dockerfile_1.expected");
            String actualContents = new String(Files.readAllBytes(expectedDockerfile.toPath()));
            // Then
            assertEquals(actualContents, value);
        }
    }

    private File getDockerfilePath(String dir) {
        ClassLoader classLoader = getClass().getClassLoader();
        return new File(Objects.requireNonNull(classLoader.getResource(
                String.format("%s/Dockerfile_1", dir))).getFile());
    }
}
