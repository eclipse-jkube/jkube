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
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;

public class DockerFileBuilderTest {

    @Test
    public void testBuildDockerFile() throws Exception {
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
        assertEquals(expected, stripCR(dockerfileContent));
    }

    @Test
    public void testBuildDockerFileMultilineLabel() throws Exception {
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
        assertEquals(expected, stripCR(dockerfileContent));
    }

    @Test
    public void testBuildLabelWithSpace() {
        String dockerfileContent = new DockerFileBuilder()
                .labels(Collections.singletonMap("key", "label with space"))
                .content();
        assertTrue(stripCR(dockerfileContent).contains("LABEL key=\"label with space\""));
    }

    @Test
    public void testBuildDockerFileUDPPort() throws Exception {
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
        assertEquals(expected, stripCR(dockerfileContent));
    }

    @Test
    public void testBuildDockerFileExplicitTCPPort() throws Exception {
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
        assertEquals(expected, stripCR(dockerfileContent));
    }

    @Test(expected= IllegalArgumentException.class)
    public void testBuildDockerFileBadPort() {
        Arguments a = Arguments.builder().execArgument("c1").execArgument("c2").build();
        new DockerFileBuilder().add("/src", "/dest")
                .baseImage("image")
                .cmd(a)
                .env(Collections.singletonMap("foo", "bar"))
                .basedir("/export")
                .expose(Collections.singletonList("8080aaa/udp"))
                .maintainer("maintainer@example.com")
                .workdir("/tmp")
                .labels(Collections.singletonMap("com.acme.foobar", "How are \"you\" ?"))
                .volumes(Collections.singletonList("/vol1"))
                .run(Arrays.asList("echo something", "echo second"))
                .content();
    }

    @Test(expected= IllegalArgumentException.class)
    public void testBuildDockerFileBadProtocol() {
        Arguments a = Arguments.builder().execArgument("c1").execArgument("c2").build();
        new DockerFileBuilder().add("/src", "/dest")
                .baseImage("image")
                .cmd(a)
                .env(Collections.singletonMap("foo", "bar"))
                .basedir("/export")
                .expose(Collections.singletonList("8080/bogusdatagram"))
                .maintainer("maintainer@example.com")
                .workdir("/tmp")
                .labels(Collections.singletonMap("com.acme.foobar", "How are \"you\" ?"))
                .volumes(Collections.singletonList("/vol1"))
                .run(Arrays.asList("echo something", "echo second"))
                .content();
    }

    @Test
    public void testDockerFileOptimisation() throws Exception {
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
        assertEquals(expected, stripCR(dockerfileContent));
    }

    @Test
    public void testMaintainer() {
        String dockerfileContent = new DockerFileBuilder().maintainer("maintainer@example.com").content();
        assertThat(dockerfileToMap(dockerfileContent)).containsEntry("MAINTAINER", "maintainer@example.com");
    }

    @Test
    public void testOptimise() {
        String dockerfileContent = new DockerFileBuilder().optimise().run(Arrays.asList("echo something", "echo two")).content();
        assertThat(dockerfileToMap(dockerfileContent)).containsEntry("RUN", "echo something && echo two");
    }

    @Test
    public void testOptimiseOnEmptyRunCommandListDoesNotThrowException() {
        final String result = new DockerFileBuilder().optimise().content();
        assertThat(result).isNotNull();;
    }

    @Test
    public void testEntryPointShell() {
        Arguments a = Arguments.builder().shell("java -jar /my-app-1.1.1.jar server").build();
        String dockerfileContent = new DockerFileBuilder().entryPoint(a).content();
        assertThat(dockerfileToMap(dockerfileContent)).containsEntry("ENTRYPOINT", "java -jar /my-app-1.1.1.jar server");
    }

    @Test
    public void testEntryPointParams() {
        Arguments a = Arguments.builder().execArgument("java").execArgument("-jar").execArgument("/my-app-1.1.1.jar").execArgument("server").build();
        String dockerfileContent = new DockerFileBuilder().entryPoint(a).content();
        assertThat(dockerfileToMap(dockerfileContent)).containsEntry("ENTRYPOINT", "[\"java\",\"-jar\",\"/my-app-1.1.1.jar\",\"server\"]");
    }

    @Test
    public void testHealthCheckCmdParams() {
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
    public void testHealthCheckNone() {
        HealthCheckConfiguration hc = HealthCheckConfiguration.builder().mode(HealthCheckMode.none).build();
        String dockerfileContent = new DockerFileBuilder().healthCheck(hc).content();
        assertThat(dockerfileToMap(dockerfileContent)).containsEntry("HEALTHCHECK", "NONE");
    }

    @Test
    public void testNoRootExport() {
        assertFalse(new DockerFileBuilder().add("/src", "/dest").basedir("/").content().contains("VOLUME"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void illegalNonAbsoluteBaseDir() {
        new DockerFileBuilder().basedir("blub").content();
    }

    @Test
    public void testAssemblyUserWithChown() {
        String dockerFile = new DockerFileBuilder().assemblyUser("jboss:jboss:jboss")
                                                   .add("a","a/nested").add("b","b/deeper/nested").content();
        String EXPECTED_REGEXP = "chown\\s+-R\\s+jboss:jboss\\s+([^\\s]+)"
                                 + "\\s+&&\\s+cp\\s+-rp\\s+\\1/\\*\\s+/\\s+&&\\s+rm\\s+-rf\\s+\\1";
        Pattern pattern = Pattern.compile(EXPECTED_REGEXP);
        assertTrue(pattern.matcher(dockerFile).find());
    }

    @Test
    public void testUser() {
        String dockerFile = new DockerFileBuilder().assemblyUser("jboss:jboss:jboss").user("bob")
                                                   .add("a","a/nested").add("b","b/deeper/nested").content();
        String EXPECTED_REGEXP = "USER bob$";
        Pattern pattern = Pattern.compile(EXPECTED_REGEXP);
        assertTrue(pattern.matcher(dockerFile).find());
    }


    @Test
    public void testExportBaseDir() {
        assertTrue(new DockerFileBuilder().basedir("/export").content().contains("/export"));
        assertFalse(new DockerFileBuilder().baseImage("java").basedir("/export").content().contains("/export"));
        assertTrue(new DockerFileBuilder().baseImage("java").exportTargetDir(true).basedir("/export").content().contains("/export"));
        assertFalse(new DockerFileBuilder().baseImage("java").exportTargetDir(false).basedir("/export").content().contains("/export"));
    }

    @Test
    public void testDockerFileKeywords() {
        StringBuilder b = new StringBuilder();
        DockerFileKeyword.RUN.addTo(b, "apt-get", "update");
        assertEquals("RUN apt-get update\n", b.toString());

        b = new StringBuilder();
        DockerFileKeyword.EXPOSE.addTo(b, "1010", "2020");
        assertEquals("EXPOSE 1010 2020\n",b.toString());

        b = new StringBuilder();
        DockerFileKeyword.USER.addTo(b, "roland");
        assertEquals("USER roland\n",b.toString());
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
