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
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.junit.Test;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
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
        assertEquals("fabric8/s2i-java", DockerFileUtil.extractBaseImages(toTest, new Properties(), BuildConfiguration.DEFAULT_FILTER, Collections.emptyMap()).get(0));
    }

    @Test
    public void testMultiStage() throws Exception {
        File toTest = copyToTempDir("Dockerfile_multi_stage");
        Iterator<String> fromClauses = DockerFileUtil.extractBaseImages(toTest, new Properties(), BuildConfiguration.DEFAULT_FILTER, Collections.emptyMap()).iterator();

        assertEquals("fabric8/s2i-java", fromClauses.next());
        assertEquals("fabric8/s1i-java", fromClauses.next());
        assertFalse(fromClauses.hasNext());
    }

    @Test
    public void testMultiStageNamed() throws Exception {
        File toTest = copyToTempDir("Dockerfile_multi_stage_named_build_stages");
        Iterator<String> fromClauses = DockerFileUtil.extractBaseImages(toTest, new Properties(), BuildConfiguration.DEFAULT_FILTER, Collections.emptyMap()).iterator();

        assertEquals("fabric8/s2i-java", fromClauses.next());
        assertFalse(fromClauses.hasNext());
    }

    @Test
    public void testMultiStageNamedWithDuplicates() throws Exception {
        File toTest = copyToTempDir("Dockerfile_multi_stage_named_redundant_build_stages");
        Iterator<String> fromClauses = DockerFileUtil.extractBaseImages(toTest, new Properties(), BuildConfiguration.DEFAULT_FILTER, Collections.emptyMap()).iterator();

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
        File actualDockerFile = PathTestUtil.createTmpFile(dockerFile.getName());
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

    @Test
    public void testMultiStageWithArgs() throws Exception {
        File toTest = copyToTempDir("Dockerfile_multi_stage_with_args");
        Iterator<String> fromClauses = DockerFileUtil.extractBaseImages(toTest, new Properties(), BuildConfiguration.DEFAULT_FILTER, Collections.emptyMap())
                .iterator();

        assertEquals("fabric8/s2i-java:latest", fromClauses.next());
        assertEquals("busybox:latest", fromClauses.next());
        assertFalse(fromClauses.hasNext());
    }

    @Test
    public void testExtractArgsFromDockerfile() {
        assertEquals("{VERSION=latest, FULL_IMAGE=busybox:latest}", DockerFileUtil.extractArgsFromLines(Arrays.asList(new String[]{"ARG", "VERSION:latest"}, new String[] {"ARG", "FULL_IMAGE=busybox:latest"}), Collections.emptyMap()).toString());
        assertEquals("{user1=someuser, buildno=1}", DockerFileUtil.extractArgsFromLines(Arrays.asList(new String[]{"ARG", "user1=someuser"}, new String[]{"ARG", "buildno=1"}), Collections.emptyMap()).toString());
        assertEquals("{NPM_VERSION=latest, NODE_VERSION=latest}", DockerFileUtil.extractArgsFromLines(Arrays.asList(new String[]{"ARG","NODE_VERSION=\"latest\""}, new String[]{"ARG",  "NPM_VERSION=\"latest\""}), Collections.emptyMap()).toString());
        assertEquals("{NPM_VERSION=latest, NODE_VERSION=latest}", DockerFileUtil.extractArgsFromLines(Arrays.asList(new String[]{"ARG","NODE_VERSION='latest'"}, new String[]{"ARG",  "NPM_VERSION='latest'"}), Collections.emptyMap()).toString());
        assertEquals("{MESSAGE=argument with spaces}", DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[] {"ARG", "MESSAGE='argument with spaces'"}), Collections.emptyMap()).toString());
        assertEquals("{MESSAGE=argument with spaces}", DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[] {"ARG", "MESSAGE=\"argument with spaces\""}), Collections.emptyMap()).toString());
        assertEquals("{TARGETPLATFORM=}", DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[]{"ARG", "TARGETPLATFORM"}), Collections.emptyMap()).toString());
        assertEquals("{TARGETPLATFORM=}", DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[]{"ARG", "TARGETPLATFORM="}), Collections.emptyMap()).toString());
        assertEquals("{TARGETPLATFORM=}", DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[]{"ARG", "TARGETPLATFORM:"}), Collections.emptyMap()).toString());
        assertEquals("{MESSAGE=argument:two}", DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[]{"ARG", "MESSAGE=argument:two"}), Collections.emptyMap()).toString());
        assertEquals("{MESSAGE2=argument=two}", DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[]{"ARG", "MESSAGE2=argument=two"}), Collections.emptyMap()).toString());
        assertEquals("{VER=0.0.3}", DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[]{"ARG", "VER=0.0.3"}), Collections.emptyMap()).toString());
        assertEquals("{VER={0.0.3}}", DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[]{"ARG", "VER={0.0.3}"}), Collections.emptyMap()).toString());
        assertEquals("{VER=[0.0.3]}", DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[]{"ARG", "VER=[0.0.3]"}), Collections.emptyMap()).toString());
        assertEquals("{VER={5,6}}", DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[]{"ARG", "VER={5,6}"}), Collections.emptyMap()).toString());
        assertEquals("{VER={5,6}}", DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[]{"ARG", "VER={5,6}"}), Collections.emptyMap()).toString());
        assertEquals("{VER={}}", DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[]{"ARG", "VER={}"}), Collections.emptyMap()).toString());
        assertEquals("{VER=====}", DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[]{"ARG", "VER====="}), Collections.emptyMap()).toString());
        assertEquals("{MESSAGE=:message}", DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[]{"ARG", "MESSAGE=:message"}), Collections.emptyMap()).toString());
        assertEquals("{MYAPP_IMAGE=myorg/myapp:latest}", DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[]{"ARG", "MYAPP_IMAGE=myorg/myapp:latest"}), Collections.emptyMap()).toString());
        assertEquals("{busyboxVersion=latest}", DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[]{"ARG", "busyboxVersion"}), Collections.singletonMap("busyboxVersion", "latest")).toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidArgWithSpacesFromDockerfile() {
        DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[]{"ARG", "MY_IMAGE image with spaces"}), Collections.emptyMap());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidArgWithTrailingArgumentFromDockerfile() {
        DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[]{"ARG", "MESSAGE=foo bar"}), Collections.emptyMap());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidArgWithArrayWithSpaceFromDockerfile() {
        DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[]{"ARG", "MESSAGE=[5, 6]"}), Collections.emptyMap());
    }

    @Test
    public void testResolveArgValueFromStrContainingArgKey() {
        assertEquals("latest", DockerFileUtil.resolveArgValueFromStrContainingArgKey("$VERSION", Collections.singletonMap("VERSION", "latest")));
        assertEquals("test", DockerFileUtil.resolveArgValueFromStrContainingArgKey("${project.scope}", Collections.singletonMap("project.scope", "test")));
    }

    private File getDockerfilePath(String dir) {
        ClassLoader classLoader = getClass().getClassLoader();
        return new File(Objects.requireNonNull(classLoader.getResource(
                String.format("%s/Dockerfile_1", dir))).getFile());
    }
}
