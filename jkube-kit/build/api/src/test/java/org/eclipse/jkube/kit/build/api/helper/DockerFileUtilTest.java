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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * @author roland
 */
class DockerFileUtilTest {

    @Test
    void simple() throws Exception {
        File toTest = copyToTempDir("Dockerfile_from_simple");
        assertThat(DockerFileUtil.extractBaseImages(toTest, new Properties(), BuildConfiguration.DEFAULT_FILTER, Collections.emptyMap()).get(0))
            .isEqualTo("fabric8/s2i-java");
    }

    @DisplayName("extract base images")
    @ParameterizedTest(name = "from ''{0}'' dockerfile")
    @MethodSource("dockerFiles")
    void extractBaseImages(String testDesc, String resource, int expectedSize, List<String> expectedImages)
        throws IOException {
      // Given
      File toTest = copyToTempDir(resource);
      // When
      List<String> fromClauses = DockerFileUtil.extractBaseImages(toTest, new Properties(), BuildConfiguration.DEFAULT_FILTER,
          Collections.emptyMap());
      // Then
      assertThat(fromClauses).hasSize(expectedSize)
          .isEqualTo(expectedImages);
    }

    static Stream<Arguments> dockerFiles() {
      return Stream.of(
          Arguments.of("multi stage", "Dockerfile_multi_stage", 2, Arrays.asList("fabric8/s2i-java", "fabric8/s1i-java")),
          Arguments.of("multi stage named", "Dockerfile_multi_stage_named_build_stages", 1, Collections.singletonList("fabric8/s2i-java")),
          Arguments.of("multi stage name with duplicates", "Dockerfile_multi_stage_named_redundant_build_stages", 1, Collections.singletonList("centos")));
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
    void interpolate() throws Exception {
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
        assertThat(FileUtils.readLines(actualDockerFile, StandardCharsets.UTF_8))
            .isEqualTo(FileUtils.readLines(expectedDockerFile, StandardCharsets.UTF_8));
    }

    @Test
    void interpolate_withNullFilter_shouldPickDefaultFilter() throws IOException {
        // Given
        Properties properties = new Properties();
        properties.put("project.base-image.uri", "openjdk:latest");
        File givenDockerfile = new File(getClass().getResource("/interpolate/Dockerfile_with_params").getFile());

        // When
        String result = DockerFileUtil.interpolate(givenDockerfile, properties, null);

        // Then
        String[] lines = result.split("\n");
        assertThat(result).isNotNull();
        assertThat(lines).hasSize(2)
            .containsExactly("FROM openjdk:latest",
                "ENTRYPOINT [\"java\", \"-jar\", \"target/docker-file-simple.jar\"]");
    }

    @Test
    void createSimpleDockerfileConfig() throws IOException {
        // Given
        File dockerFile = Files.createTempFile("Dockerfile", "-test").toFile();
        // When
        ImageConfiguration imageConfiguration1 = DockerFileUtil.createSimpleDockerfileConfig(dockerFile, null);
        ImageConfiguration imageConfiguration2 = DockerFileUtil.createSimpleDockerfileConfig(dockerFile, "someImage:0.0.1");
        // Then
        assertThat(imageConfiguration1).isNotNull()
            .hasFieldOrPropertyWithValue("name", "%g/%a:%l")
            .extracting(ImageConfiguration::getBuild)
            .extracting(BuildConfiguration::getDockerFileRaw)
            .isEqualTo(dockerFile.getPath());
        assertThat(imageConfiguration2).isNotNull()
            .hasFieldOrPropertyWithValue("name", "someImage:0.0.1")
            .extracting(ImageConfiguration::getBuild)
            .extracting(BuildConfiguration::getDockerFileRaw)
            .isEqualTo(dockerFile.getPath());
    }

    @Test
    void addSimpleDockerfileConfig() throws IOException {
        // Given
        ImageConfiguration imageConfiguration = ImageConfiguration.builder()
                .name("test-image")
                .build();
        File dockerFile = Files.createTempFile("Dockerfile", "-test").toFile();

        // When
        ImageConfiguration result = DockerFileUtil.addSimpleDockerfileConfig(imageConfiguration, dockerFile);

        // Then
        assertThat(result.getBuild()).isNotNull()
            .extracting(BuildConfiguration::getDockerFileRaw)
            .isEqualTo(dockerFile.getPath());
    }

    @Test
    void customInterpolation() throws IOException {
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
    void extractBaseImages_withMultiStageWithArgs() throws Exception {
        File toTest = copyToTempDir("Dockerfile_multi_stage_with_args");
        assertThat(DockerFileUtil.extractBaseImages(toTest, new Properties(), "${*}", Collections.emptyMap()))
            .containsExactly("fabric8/s2i-java:latest", "busybox:latest", "docker.io/library/openjdk:latest");
    }

    @Test
    void extractArgsFromDockerfile() {
      assertAll(
          () -> assertThat(DockerFileUtil.extractArgsFromLines(Arrays.asList(new String[] { "ARG", "VERSION:latest" }, new String[] { "ARG", "FULL_IMAGE=busybox:latest" }), Collections.emptyMap())).hasToString("{VERSION=latest, FULL_IMAGE=busybox:latest}"),
          () -> assertThat(DockerFileUtil.extractArgsFromLines(Arrays.asList(new String[] { "ARG", "user1=someuser" }, new String[] { "ARG", "buildno=1" }), Collections.emptyMap())).hasToString("{user1=someuser, buildno=1}"),
          () -> assertThat(DockerFileUtil.extractArgsFromLines(Arrays.asList(new String[] { "ARG", "NODE_VERSION=\"latest\"" }, new String[] { "ARG", "NPM_VERSION=\"latest\"" }), Collections.emptyMap())).hasToString("{NPM_VERSION=latest, NODE_VERSION=latest}"),
          () -> assertThat(DockerFileUtil.extractArgsFromLines(Arrays.asList(new String[] { "ARG", "NODE_VERSION='latest'" }, new String[] { "ARG", "NPM_VERSION='latest'" }), Collections.emptyMap())).hasToString("{NPM_VERSION=latest, NODE_VERSION=latest}"),
          () -> assertThat(DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[] { "ARG", "MESSAGE='argument with spaces'" }), Collections.emptyMap())).hasToString("{MESSAGE=argument with spaces}"),
          () -> assertThat(DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[] { "ARG", "MESSAGE=\"argument with spaces\"" }), Collections.emptyMap())).hasToString("{MESSAGE=argument with spaces}"),
          () -> assertThat(DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[] { "ARG", "TARGETPLATFORM" }), Collections.emptyMap())).hasToString("{TARGETPLATFORM=}"),
          () -> assertThat(DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[] { "ARG", "TARGETPLATFORM=" }), Collections.emptyMap())).hasToString("{TARGETPLATFORM=}"),
          () -> assertThat(DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[] { "ARG", "TARGETPLATFORM:" }), Collections.emptyMap())).hasToString("{TARGETPLATFORM=}"),
          () -> assertThat(DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[] { "ARG", "MESSAGE=argument:two" }), Collections.emptyMap())).hasToString("{MESSAGE=argument:two}"),
          () -> assertThat(DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[] { "ARG", "MESSAGE2=argument=two" }), Collections.emptyMap())).hasToString("{MESSAGE2=argument=two}"),
          () -> assertThat(DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[] { "ARG", "VER=0.0.3" }), Collections.emptyMap())).hasToString("{VER=0.0.3}"),
          () -> assertThat(DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[] { "ARG", "VER={0.0.3}" }), Collections.emptyMap())).hasToString("{VER={0.0.3}}"),
          () -> assertThat(DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[] { "ARG", "VER=[0.0.3]" }), Collections.emptyMap())).hasToString("{VER=[0.0.3]}"),
          () -> assertThat(DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[] { "ARG", "VER={5,6}" }), Collections.emptyMap())).hasToString("{VER={5,6}}"),
          () -> assertThat(DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[] { "ARG", "VER={5,6}" }), Collections.emptyMap())).hasToString("{VER={5,6}}"),
          () -> assertThat(DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[] { "ARG", "VER={}" }), Collections.emptyMap())).hasToString("{VER={}}"),
          () -> assertThat(DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[] { "ARG", "VER=====" }), Collections.emptyMap())).hasToString("{VER=====}"),
          () -> assertThat(DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[] { "ARG", "MESSAGE=:message" }), Collections.emptyMap())).hasToString("{MESSAGE=:message}"),
          () -> assertThat(DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[] { "ARG", "MYAPP_IMAGE=myorg/myapp:latest" }), Collections.emptyMap())).hasToString("{MYAPP_IMAGE=myorg/myapp:latest}"),
          () -> assertThat(DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[] { "ARG", "busyboxVersion" }), Collections.singletonMap("busyboxVersion", "latest"))).hasToString("{busyboxVersion=latest}"),
          () -> assertThat(DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[] { "ARG", "busyboxVersion=latest" }), Collections.singletonMap("busyboxVersion", "slim"))).hasToString("{busyboxVersion=slim}"),
          () -> assertThat(DockerFileUtil.extractArgsFromLines(Collections.singletonList(new String[] { "ARG", "busyboxVersion=latest" }), null)).hasToString("{busyboxVersion=latest}")
      );
    }

    @DisplayName("extract arguments from lines")
    @ParameterizedTest(name = "invalid Argument having {0} in dockerfile, should throw exception")
    @MethodSource("invalidArguments")
    void extractArgsFromLines(String testDesc, String[] args) {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> DockerFileUtil.extractArgsFromLines(Collections.singletonList(args), Collections.emptyMap()))
          .withMessageContaining("Dockerfile parse error: ARG requires exactly one argument");
    }

    static Stream<Arguments> invalidArguments() {
      return Stream.of(
          arguments("spaces", new String[] { "ARG", "MY_IMAGE image with spaces" }),
          arguments("trailing argument", new String[] { "ARG", "MESSAGE=foo bar" }),
          arguments("array with space", new String[] { "ARG", "MESSAGE=[5, 6]" }));
    }

    @DisplayName("resolve argument value from image tag string containing key")
    @ParameterizedTest(name = "from image tag ''{0}'', should return ''{2}''")
    @MethodSource("args")
    void resolveArgValueFromStrContainingArgKey(String imageTagString, Map<String, String> args, String expected) {
      assertThat(DockerFileUtil.resolveImageTagFromArgs(imageTagString, args))
          .isEqualTo(expected);
    }

    static Stream<Arguments> args() {
      return Stream.of(
          arguments("$VERSION", Collections.singletonMap("VERSION", "latest"), "latest"),
          arguments("${project.scope}", Collections.singletonMap("project.scope", "test"), "test"),
          arguments("$ad", Collections.singletonMap("ad", "test"), "test"),
          arguments("bla$ad", Collections.singletonMap("ad", "test"), "blatest"),
          arguments("${foo}bar", Collections.singletonMap("foo", "test"), "testbar"),
          arguments("bar${foo}", Collections.singletonMap("foo", "test"), "bartest"),
          arguments("$ad", Collections.emptyMap(), "$ad")
      );
    }

    @Test
    void findAllArgs_definedInString() {
        assertThat(DockerFileUtil.findAllArgs("$REPO_1/bar${IMAGE-1}foo:$VERSION"))
            .containsExactlyInAnyOrder("REPO_1", "IMAGE-1", "VERSION");
    }

    @Test
    void findAllArgs_fromInvalidArg_shouldBeEmpty() {
        assertThat(DockerFileUtil.findAllArgs("${invalidArg")).isEmpty();
    }

    @Test
    void createSimpleDockerfileConfig_withPorts() {
        // Given
        File dockerFile = new File(getClass().getResource("/docker/Dockerfile_expose_ports").getFile());
        // When
        ImageConfiguration imageConfiguration1 = DockerFileUtil.createSimpleDockerfileConfig(dockerFile, null);
        // Then
        assertThat(imageConfiguration1.getBuild().getDockerFileRaw()).isEqualTo(dockerFile.getPath());
        assertThat(imageConfiguration1).isNotNull()
            .hasFieldOrPropertyWithValue("name", "%g/%a:%l")
            .extracting(ImageConfiguration::getBuild)
            .extracting(BuildConfiguration::getPorts).isNotNull()
            .asList()
            .hasSize(5)
            .containsExactly("80/tcp", "8080/udp", "80", "8080", "99/udp");
    }

    @Test
    void extractPorts_fromInvalidDockerFile_shouldThrowException() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> DockerFileUtil.extractPorts(new File("iDoNotExist")))
          .withMessage("Error in reading Dockerfile");
    }

    @Test
    void extractPorts_fromDockerFileLines() {
        // Given
        List<String[]> input1 = Arrays.asList(new String[]{"EXPOSE", "8080", "9090", "9999"} , new String[]{"EXPOSE", "9010"});
        List<String[]> input2 = Arrays.asList(new String[]{"EXPOSE", "9001"}, new String[]{"EXPOSE", null});
        List<String[]> input3 = Arrays.asList(new String[]{"EXPOSE", ""}, new String[]{"EXPOSE", "8001"});

        // When
        List<String> result1 = DockerFileUtil.extractPorts(input1);
        List<String> result2 = DockerFileUtil.extractPorts(input2);
        List<String> result3 = DockerFileUtil.extractPorts(input3);

        // Then
        assertThat(result1).containsExactly("9090", "8080", "9999", "9010");
        assertThat(result2).containsExactly("9001");
        assertThat(result3).containsExactly("8001");
    }

    @Test
    void resolveDockerfileFilter() {
        assertThat(DockerFileUtil.resolveDockerfileFilter(null)).isEqualTo(BuildConfiguration.DEFAULT_FILTER);
        assertThat(DockerFileUtil.resolveDockerfileFilter("@*@")).isEqualTo("@*@");
    }

    private File getDockerfilePath(String dir) {
        ClassLoader classLoader = getClass().getClassLoader();
        return new File(Objects.requireNonNull(classLoader.getResource(
                String.format("%s/Dockerfile_1", dir))).getFile());
    }
}
