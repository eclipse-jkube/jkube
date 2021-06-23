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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author roland
 */
public class DockerFileUtilTest {

    @Test
    public void testSimple() throws Exception {
        File toTest = copyToTempDir("Dockerfile_from_simple");
        assertThat(DockerFileUtil.extractBaseImages(toTest, new Properties(), BuildConfiguration.DEFAULT_FILTER, Collections.emptyMap()).get(0)).isEqualTo("fabric8/s2i-java");
    }

    @Test
    public void testMultiStage() throws Exception {
        File toTest = copyToTempDir("Dockerfile_multi_stage");
        Iterator<String> fromClauses = DockerFileUtil.extractBaseImages(toTest, new Properties(), BuildConfiguration.DEFAULT_FILTER, Collections.emptyMap()).iterator();

        assertThat(fromClauses.next()).isEqualTo("fabric8/s2i-java");
        assertThat(fromClauses.next()).isEqualTo("fabric8/s1i-java");
        assertThat(fromClauses.hasNext()).isFalse();
    }

    @Test
    public void testMultiStageNamed() throws Exception {
        File toTest = copyToTempDir("Dockerfile_multi_stage_named_build_stages");
        Iterator<String> fromClauses = DockerFileUtil.extractBaseImages(toTest, new Properties(), BuildConfiguration.DEFAULT_FILTER, Collections.emptyMap()).iterator();

        assertThat(fromClauses.next()).isEqualTo("fabric8/s2i-java");
        assertThat(fromClauses.hasNext()).isFalse();
    }

    @Test
    public void testMultiStageNamedWithDuplicates() throws Exception {
        File toTest = copyToTempDir("Dockerfile_multi_stage_named_redundant_build_stages");
        Iterator<String> fromClauses = DockerFileUtil.extractBaseImages(toTest, new Properties(), BuildConfiguration.DEFAULT_FILTER, Collections.emptyMap()).iterator();

        assertThat(fromClauses.next()).isEqualTo("centos");
        assertThat(fromClauses.hasNext()).isFalse();
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
        assertThat(FileUtils.readLines(actualDockerFile, StandardCharsets.UTF_8)).isEqualTo(FileUtils.readLines(expectedDockerFile, StandardCharsets.UTF_8));
    }

    @Test
    public void interpolateWithNullFilterShouldPickDefaultFilter() throws IOException {
        // Given
        Properties properties = new Properties();
        properties.put("project.base-image.uri", "openjdk:latest");
        File givenDockerfile = new File(getClass().getResource("/interpolate/Dockerfile_with_params").getFile());

        // When
        String result = DockerFileUtil.interpolate(givenDockerfile, properties, null);

        // Then
        String[] lines = result.split("\n");
        assertThat(result).isNotNull();
        assertThat(lines.length).isEqualTo(2);
        assertThat(lines[0]).isEqualTo("FROM openjdk:latest");
        assertThat(lines[1]).isEqualTo("ENTRYPOINT [\"java\", \"-jar\", \"target/docker-file-simple.jar\"]");
    }

    @Test
    public void testCreateSimpleDockerfileConfig() throws IOException {
        // Given
        File dockerFile = File.createTempFile("Dockerfile", "-test");
        // When
        ImageConfiguration imageConfiguration1 = DockerFileUtil.createSimpleDockerfileConfig(dockerFile, null);
        ImageConfiguration imageConfiguration2 = DockerFileUtil.createSimpleDockerfileConfig(dockerFile, "someImage:0.0.1");
        // Then
        assertThat(imageConfiguration1).isNotNull();
        assertThat(imageConfiguration1.getName()).isEqualTo("%g/%a:%l");
        assertThat(imageConfiguration1.getBuild().getDockerFileRaw()).isEqualTo(dockerFile.getPath());
        assertThat(imageConfiguration2).isNotNull();
        assertThat(imageConfiguration2.getName()).isEqualTo("someImage:0.0.1");
        assertThat(imageConfiguration2.getBuild().getDockerFileRaw()).isEqualTo(dockerFile.getPath());
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
        assertThat(result.getBuild()).isNotNull();
        assertThat(result.getBuild().getDockerFileRaw()).isEqualTo(dockerFile.getPath());
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
            assertThat(value).isEqualTo(actualContents);
        }
    }

    @Test
    public void testMultiStageWithArgs() throws Exception {
        File toTest = copyToTempDir("Dockerfile_multi_stage_with_args");
        Iterator<String> fromClauses = DockerFileUtil.extractBaseImages(toTest, new Properties(), BuildConfiguration.DEFAULT_FILTER, Collections.emptyMap())
                .iterator();

        assertThat(fromClauses.next()).isEqualTo("fabric8/s2i-java:latest");
        assertThat(fromClauses.next()).isEqualTo("busybox:latest");
        assertThat(fromClauses.hasNext()).isFalse();
    }

    @Test
    public void testExtractArgsFromDockerfile() {
        assertThat(DockerFileUtil.extractArgsFromLines(Arrays.asList(new String[]{"ARG", "VERSION:latest"}, new String[] {"ARG", "FULL_IMAGE=busybox:latest"}), Collections.emptyMap()).toString()).isEqualTo("{VERSION=latest, FULL_IMAGE=busybox:latest}");
        assertThat(DockerFileUtil.extractArgsFromLines(Arrays.asList(new String[]{"ARG", "user1=someuser"}, new String[]{"ARG", "buildno=1"}), Collections.emptyMap()).toString()).isEqualTo("{user1=someuser, buildno=1}");
        assertThat(DockerFileUtil.extractArgsFromLines(Arrays.asList(new String[]{"ARG","NODE_VERSION=\"latest\""}, new String[]{"ARG",  "NPM_VERSION=\"latest\""}), Collections.emptyMap()).toString()).isEqualTo("{NPM_VERSION=latest, NODE_VERSION=latest}");
        assertThat(DockerFileUtil.extractArgsFromLines(Arrays.asList(new String[]{"ARG","NODE_VERSION='latest'"}, new String[]{"ARG",  "NPM_VERSION='latest'"}), Collections.emptyMap()).toString()).isEqualTo("{NPM_VERSION=latest, NODE_VERSION=latest}");
        assertThat(DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[] {"ARG", "MESSAGE='argument with spaces'"}), Collections.emptyMap()).toString()).isEqualTo("{MESSAGE=argument with spaces}");
        assertThat(DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[] {"ARG", "MESSAGE=\"argument with spaces\""}), Collections.emptyMap()).toString()).isEqualTo("{MESSAGE=argument with spaces}");
        assertThat(DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[]{"ARG", "TARGETPLATFORM"}), Collections.emptyMap()).toString()).isEqualTo("{TARGETPLATFORM=}");
        assertThat(DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[]{"ARG", "TARGETPLATFORM="}), Collections.emptyMap()).toString()).isEqualTo("{TARGETPLATFORM=}");
        assertThat(DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[]{"ARG", "TARGETPLATFORM:"}), Collections.emptyMap()).toString()).isEqualTo("{TARGETPLATFORM=}");
        assertThat(DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[]{"ARG", "MESSAGE=argument:two"}), Collections.emptyMap()).toString()).isEqualTo("{MESSAGE=argument:two}");
        assertThat(DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[]{"ARG", "MESSAGE2=argument=two"}), Collections.emptyMap()).toString()).isEqualTo("{MESSAGE2=argument=two}");
        assertThat(DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[]{"ARG", "VER=0.0.3"}), Collections.emptyMap()).toString()).isEqualTo("{VER=0.0.3}");
        assertThat(DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[]{"ARG", "VER={0.0.3}"}), Collections.emptyMap()).toString()).isEqualTo("{VER={0.0.3}}");
        assertThat(DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[]{"ARG", "VER=[0.0.3]"}), Collections.emptyMap()).toString()).isEqualTo("{VER=[0.0.3]}");
        assertThat(DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[]{"ARG", "VER={5,6}"}), Collections.emptyMap()).toString()).isEqualTo("{VER={5,6}}");
        assertThat(DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[]{"ARG", "VER={5,6}"}), Collections.emptyMap()).toString()).isEqualTo("{VER={5,6}}");
        assertThat(DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[]{"ARG", "VER={}"}), Collections.emptyMap()).toString()).isEqualTo("{VER={}}");
        assertThat(DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[]{"ARG", "VER====="}), Collections.emptyMap()).toString()).isEqualTo("{VER=====}");
        assertThat(DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[]{"ARG", "MESSAGE=:message"}), Collections.emptyMap()).toString()).isEqualTo("{MESSAGE=:message}");
        assertThat(DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[]{"ARG", "MYAPP_IMAGE=myorg/myapp:latest"}), Collections.emptyMap()).toString()).isEqualTo("{MYAPP_IMAGE=myorg/myapp:latest}");
        assertThat(DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[]{"ARG", "busyboxVersion"}), Collections.singletonMap("busyboxVersion", "latest")).toString()).isEqualTo("{busyboxVersion=latest}");
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
        assertThat(DockerFileUtil.resolveArgValueFromStrContainingArgKey("$VERSION", Collections.singletonMap("VERSION", "latest"))).isEqualTo("latest");
        assertThat(DockerFileUtil.resolveArgValueFromStrContainingArgKey("${project.scope}", Collections.singletonMap("project.scope", "test"))).isEqualTo("test");
    }

    @Test
    public void testCreateSimpleDockerfileConfigWithPorts() {
        // Given
        File dockerFile = new File(getClass().getResource("/docker/Dockerfile_expose_ports").getFile());
        // When
        ImageConfiguration imageConfiguration1 = DockerFileUtil.createSimpleDockerfileConfig(dockerFile, null);
        // Then
        assertThat(imageConfiguration1).isNotNull();
        assertThat(imageConfiguration1.getName()).isEqualTo("%g/%a:%l");
        assertThat(imageConfiguration1.getBuild().getDockerFileRaw()).isEqualTo(dockerFile.getPath());
        assertThat(imageConfiguration1.getBuild().getPorts()).isNotNull();
        assertThat(imageConfiguration1.getBuild().getPorts().size()).isEqualTo(5);
        assertThat(imageConfiguration1.getBuild().getPorts().get(0)).isEqualTo("80/tcp");
        assertThat(imageConfiguration1.getBuild().getPorts().get(1)).isEqualTo("8080/udp");
        assertThat(imageConfiguration1.getBuild().getPorts().get(2)).isEqualTo("80");
        assertThat(imageConfiguration1.getBuild().getPorts().get(3)).isEqualTo("8080");
        assertThat(imageConfiguration1.getBuild().getPorts().get(4)).isEqualTo("99/udp");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetPortsFromDockerFileShouldThrowExceptionOnInvalidFile() {
        DockerFileUtil.extractPorts(new File("iDoNotExist"));
    }

    @Test
    public void testGetPortsFromDockerFileLines() {
        // Given
        List<String[]> input1 = Arrays.asList(new String[]{"EXPOSE", "8080", "9090", "9999"} , new String[]{"EXPOSE", "9010"});
        List<String[]> input2 = Arrays.asList(new String[]{"EXPOSE", "9001"}, new String[]{"EXPOSE", null});
        List<String[]> input3 = Arrays.asList(new String[]{"EXPOSE", ""}, new String[]{"EXPOSE", "8001"});

        // When
        List<String> result1 = DockerFileUtil.extractPorts(input1);
        List<String> result2 = DockerFileUtil.extractPorts(input2);
        List<String> result3 = DockerFileUtil.extractPorts(input3);

        // Then
        assertThat(result1).isEqualTo(Arrays.asList("9090", "8080", "9999", "9010"));
        assertThat(result2).isEqualTo(Collections.singletonList("9001"));
        assertThat(result3).isEqualTo(Collections.singletonList("8001"));
    }

    @Test
    public void testResolveDockerfileFilter() {
        assertThat(DockerFileUtil.resolveDockerfileFilter(null)).isEqualTo(BuildConfiguration.DEFAULT_FILTER);
        assertThat(DockerFileUtil.resolveDockerfileFilter("@*@")).isEqualTo("@*@");
    }

    private File getDockerfilePath(String dir) {
        ClassLoader classLoader = getClass().getClassLoader();
        return new File(Objects.requireNonNull(classLoader.getResource(
                String.format("%s/Dockerfile_1", dir))).getFile());
    }
}
