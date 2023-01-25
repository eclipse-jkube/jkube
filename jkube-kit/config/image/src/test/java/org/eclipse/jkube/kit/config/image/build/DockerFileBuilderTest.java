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
package org.eclipse.jkube.kit.config.image.build;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class DockerFileBuilderTest {

    @Test
    void testBuildDockerFile() throws Exception {
        Arguments a = Arguments.builder().execArgument("c1").execArgument("c2").build();
        String dockerfileContent = new DockerFileBuilder().add("/src", "/dest")
                                                          .baseImage("image")
                                                          .cmd(a)
                                                          .env(Collections.singletonMap("foo", "bar"))
                                                          .basedir("/export")
                                                          .expose(Collections.singletonList("8080"))
                                                          .maintainer("maintainer@example.com")
                                                          .workdir("/tmp")
                                                          .labels(Collections.singletonMap("com.acme.foobar", "How are \"you\" ?"))
                                                          .volumes(Collections.singletonList("/vol1"))
                                                          .run(Arrays.asList("echo something", "echo second"))
                                                          .content();
        String expected = loadFile("docker/Dockerfile.test");
        assertThat(stripCR(dockerfileContent)).isEqualTo(expected);
    }

    @Test
    void testBuildDockerFileMultilineLabel() throws Exception {
        Arguments a = Arguments.builder().execArgument("c1").execArgument("c2").build();
        String dockerfileContent = new DockerFileBuilder()
                .add("/src", "/dest")
                .baseImage("image")
                .cmd(a)
                .labels(new LinkedHashMap<String, String>() {{
                    put("key","unquoted");
                    put("flag","");
                    put("with_space","1.fc nuremberg");
                    put("some-json","{\n  \"key\": \"value\"\n}\n");
                }})
                .content();
        String expected = loadFile("docker/Dockerfile.multiline_label.test");
        assertThat(stripCR(dockerfileContent)).isEqualTo(expected);
    }

    @Test
    void testBuildLabelWithSpace() {
        String dockerfileContent = new DockerFileBuilder()
                .labels(Collections.singletonMap("key", "label with space"))
                .content();
        assertThat(stripCR(dockerfileContent)).contains("LABEL key=\"label with space\"");
    }

    @Test
    void testBuildDockerFileUDPPort() throws Exception {
        Arguments a = Arguments.builder().execArgument("c1").execArgument("c2").build();
        String dockerfileContent = new DockerFileBuilder().add("/src", "/dest")
                                                          .baseImage("image")
                                                          .cmd(a)
                                                          .basedir("/export")
                                                          .expose(Collections.singletonList("8080/udp"))
                                                          .maintainer("maintainer@example.com")
                                                          .workdir("/tmp")
                                                          .volumes(Collections.singletonList("/vol1"))
                                                          .run(Arrays.asList("echo something", "echo second"))
                                                          .content();
        String expected = loadFile("docker/Dockerfile_udp.test");
        assertThat(stripCR(dockerfileContent)).isEqualTo(expected);
    }

    @Test
    void testBuildDockerFileExplicitTCPPort() throws Exception {
        Arguments a = Arguments.builder().execArgument("c1").execArgument("c2").build();
        String dockerfileContent = new DockerFileBuilder().add("/src", "/dest")
                                                          .baseImage("image")
                                                          .cmd(a)
                                                          .basedir("/export")
                                                          .expose(Collections.singletonList("8080/tcp"))
                                                          .maintainer("maintainer@example.com")
                                                          .workdir("/tmp")
                                                          .volumes(Collections.singletonList("/vol1"))
                                                          .run(Arrays.asList("echo something", "echo second"))
                                                          .content();
        String expected = loadFile("docker/Dockerfile_tcp.test");
        assertThat(stripCR(dockerfileContent)).isEqualTo(expected);
    }

    @Test
    void testBuildDockerFileBadPort() {
        Arguments a = Arguments.builder().execArgument("c1").execArgument("c2").build();
        DockerFileBuilder fileBuilder = new DockerFileBuilder().add("/src", "/dest")
                .baseImage("image")
                .cmd(a)
                .env(Collections.singletonMap("foo", "bar"))
                .basedir("/export")
                .expose(Collections.singletonList("8080aaa/udp"))
                .maintainer("maintainer@example.com")
                .workdir("/tmp")
                .labels(Collections.singletonMap("com.acme.foobar", "How are \"you\" ?"))
                .volumes(Collections.singletonList("/vol1"))
                .run(Arrays.asList("echo something", "echo second"));
        assertThatIllegalArgumentException().isThrownBy(fileBuilder::content);
    }

    @Test
    void testBuildDockerFileBadProtocol() {
        Arguments a = Arguments.builder().execArgument("c1").execArgument("c2").build();
        DockerFileBuilder fileBuilder = new DockerFileBuilder().add("/src", "/dest")
                .baseImage("image")
                .cmd(a)
                .env(Collections.singletonMap("foo", "bar"))
                .basedir("/export")
                .expose(Collections.singletonList("8080/bogusdatagram"))
                .maintainer("maintainer@example.com")
                .workdir("/tmp")
                .labels(Collections.singletonMap("com.acme.foobar", "How are \"you\" ?"))
                .volumes(Collections.singletonList("/vol1"))
                .run(Arrays.asList("echo something", "echo second"));
        assertThatIllegalArgumentException().isThrownBy(fileBuilder::content);
    }

    @Test
    void testDockerFileOptimisation() throws Exception {
        Arguments a = Arguments.builder().execArgument("c1").execArgument("c2").build();
        String dockerfileContent = new DockerFileBuilder().add("/src", "/dest")
                                                          .baseImage("image")
                                                          .cmd(a)
                                                          .env(Collections.singletonMap("foo", "bar"))
                                                          .basedir("/export")
                                                          .expose(Collections.singletonList("8080"))
                                                          .maintainer("maintainer@example.com")
                                                          .workdir("/tmp")
                                                          .labels(Collections.singletonMap("com.acme.foobar", "How are \"you\" ?"))
                                                          .volumes(Collections.singletonList("/vol1"))
                                                          .run(Arrays.asList("echo something", "echo second", "echo third", "echo fourth", "echo fifth"))
                                                          .optimise()
                                                          .content();
        String expected = loadFile("docker/Dockerfile_optimised.test");
        assertThat(stripCR(dockerfileContent)).isEqualTo(expected);
    }

    @Test
    void testMaintainer() {
        String dockerfileContent = new DockerFileBuilder().maintainer("maintainer@example.com").content();
        assertThat(dockerfileToMap(dockerfileContent)).containsEntry("MAINTAINER", "maintainer@example.com");
    }

    @Test
    void testOptimise() {
        String dockerfileContent = new DockerFileBuilder().optimise().run(Arrays.asList("echo something", "echo two")).content();
        assertThat(dockerfileToMap(dockerfileContent)).containsEntry("RUN", "echo something && echo two");
    }

    @Test
    void testOptimiseOnEmptyRunCommandListDoesNotThrowException() {
        final String result = new DockerFileBuilder().optimise().content();
        assertThat(result).isNotNull();
    }

    @Test
    void testEntryPointShell() {
        Arguments a = Arguments.builder().shell("java -jar /my-app-1.1.1.jar server").build();
        String dockerfileContent = new DockerFileBuilder().entryPoint(a).content();
        assertThat(dockerfileToMap(dockerfileContent)).containsEntry("ENTRYPOINT", "java -jar /my-app-1.1.1.jar server");
    }

    @Test
    void testEntryPointParams() {
        Arguments a = Arguments.builder().execArgument("java").execArgument("-jar").execArgument("/my-app-1.1.1.jar").execArgument("server").build();
        String dockerfileContent = new DockerFileBuilder().entryPoint(a).content();
        assertThat(dockerfileToMap(dockerfileContent)).containsEntry("ENTRYPOINT", "[\"java\",\"-jar\",\"/my-app-1.1.1.jar\",\"server\"]");
    }

    @Test
    void testHealthCheckCmdParams() {
        HealthCheckConfiguration hc = HealthCheckConfiguration.builder()
            .cmd(Arguments.builder().shell("echo hello").build())
            .interval("5s").timeout("3s")
            .startPeriod("30s")
            .retries(4)
            .build();
        String dockerfileContent = new DockerFileBuilder().healthCheck(hc).content();
        assertThat(dockerfileToMap(dockerfileContent)).containsEntry("HEALTHCHECK", "--interval=5s --timeout=3s --start-period=30s --retries=4 CMD echo hello");
    }

    @Test
    void testHealthCheckNone() {
        HealthCheckConfiguration hc = HealthCheckConfiguration.builder().mode(HealthCheckMode.none).build();
        String dockerfileContent = new DockerFileBuilder().healthCheck(hc).content();
        assertThat(dockerfileToMap(dockerfileContent)).containsEntry("HEALTHCHECK", "NONE");
    }

    @Test
    void testNoRootExport() {
      assertThat(new DockerFileBuilder().add("/src", "/dest").basedir("/").content()).doesNotContain("VOLUME");
    }

    @Test
    void illegalNonAbsoluteBaseDir() {
        assertThatIllegalArgumentException().isThrownBy(() -> new DockerFileBuilder().basedir("blub").content());
    }

    @Test
    void testAssemblyUserWithChown() {
        String dockerFile = new DockerFileBuilder().assemblyUser("jboss:jboss:jboss")
                                                   .add("a","a/nested").add("b","b/deeper/nested").content();
        String EXPECTED_REGEXP = "chown\\s+-R\\s+jboss:jboss\\s+([^\\s]+)"
                                 + "\\s+&&\\s+cp\\s+-rp\\s+\\1/\\*\\s+/\\s+&&\\s+rm\\s+-rf\\s+\\1";
        Pattern pattern = Pattern.compile(EXPECTED_REGEXP);
        assertThat(pattern.matcher(dockerFile).find()).isTrue();
    }

    @Test
    void testUser() {
        String dockerFile = new DockerFileBuilder().assemblyUser("jboss:jboss:jboss").user("bob")
                                                   .add("a","a/nested").add("b","b/deeper/nested").content();
        String EXPECTED_REGEXP = "USER bob$";
        Pattern pattern = Pattern.compile(EXPECTED_REGEXP);
        assertThat(pattern.matcher(dockerFile).find()).isTrue();
    }


    @Test
    void testExportBaseDir() {
      assertThat(new DockerFileBuilder().basedir("/export").content()).contains("/export");
      assertThat(new DockerFileBuilder().baseImage("java").basedir("/export").content()).doesNotContain("/export");
      assertThat(new DockerFileBuilder().baseImage("java").exportTargetDir(true).basedir("/export").content())
              .contains("/export");
      assertThat(new DockerFileBuilder().baseImage("java").exportTargetDir(false).basedir("/export").content())
              .doesNotContain("/export");
    }

    @Test
    void testDockerFileKeywords() {
        StringBuilder b = new StringBuilder();
        DockerFileKeyword.RUN.addTo(b, "apt-get", "update");
        assertThat(b).hasToString("RUN apt-get update\n");

        b = new StringBuilder();
        DockerFileKeyword.EXPOSE.addTo(b, "1010", "2020");
        assertThat(b).hasToString("EXPOSE 1010 2020\n");

        b = new StringBuilder();
        DockerFileKeyword.USER.addTo(b, "roland");
        assertThat(b).hasToString("USER roland\n");
    }

    private String stripCR(String input){
    	return input.replaceAll("\r", "");
    }

    private String loadFile(String fileName) throws IOException {
        return stripCR(IOUtils.toString(Objects.requireNonNull(getClass().getClassLoader().getResource(fileName)), Charset.defaultCharset()));
    }

    private static Map<String, String> dockerfileToMap(String dockerFile) {
        final Map<String, String> dockerfileMap = new HashMap<>();
        final Scanner scanner = new Scanner(dockerFile);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.trim().length() == 0) {
                continue;
            }
            String[] commandAndArguments = line.trim().split("\\s+", 2);
            if (commandAndArguments.length < 2) {
                continue;
            }
            dockerfileMap.put(commandAndArguments[0], commandAndArguments[1]);
        }
        scanner.close();
        return dockerfileMap;
    }
}
