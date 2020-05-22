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
package org.eclipse.jkube.kit.build.service.docker.helper;

import org.apache.commons.io.FileUtils;
import org.eclipse.jkube.kit.build.service.docker.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.Properties;

import static org.eclipse.jkube.kit.build.service.docker.helper.PathTestUtil.createTmpFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author roland
 * @since 21/01/16
 */
public class DockerFileUtilTest {

    @Test
    public void testSimple() throws Exception {
        File toTest = copyToTempDir("Dockerfile_from_simple");
        assertEquals("fabric8/s2i-java", DockerFileUtil.extractBaseImages(toTest, new Properties()).get(0));
    }

    @Test
    public void testMultiStage() throws Exception {
        File toTest = copyToTempDir("Dockerfile_multi_stage");
        Iterator<String> fromClauses = DockerFileUtil.extractBaseImages(toTest, new Properties()).iterator();

        assertEquals("fabric8/s2i-java", fromClauses.next());
        assertEquals("fabric8/s1i-java", fromClauses.next());
        assertEquals(false, fromClauses.hasNext());
    }

    @Test
    public void testMultiStageNamed() throws Exception {
        File toTest = copyToTempDir("Dockerfile_multi_stage_named_build_stages");
        Iterator<String> fromClauses = DockerFileUtil.extractBaseImages(toTest, new Properties()).iterator();

        assertEquals("fabric8/s2i-java", fromClauses.next());
        assertEquals(false, fromClauses.hasNext());
    }

    @Test
    public void testMultiStageNamedWithDuplicates() throws Exception {
        File toTest = copyToTempDir("Dockerfile_multi_stage_named_redundant_build_stages");
        Iterator<String> fromClauses = DockerFileUtil.extractBaseImages(toTest, new Properties()).iterator();

        assertEquals("centos", fromClauses.next());
        assertEquals(false, fromClauses.hasNext());

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
        FileUtils.write(actualDockerFile, DockerFileUtil.interpolate(dockerFile, projectProperties), "UTF-8");
        // Compare text lines without regard to EOL delimiters
        assertEquals(FileUtils.readLines(expectedDockerFile), FileUtils.readLines(actualDockerFile));
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

    private File getDockerfilePath(String dir) {
        ClassLoader classLoader = getClass().getClassLoader();
        return new File(classLoader.getResource(
            String.format("%s/Dockerfile_1", dir)).getFile());
    }
}
