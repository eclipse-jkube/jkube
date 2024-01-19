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
package org.eclipse.jkube.kit.config.image;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImageNameTest {

    @Test
    void simple() {

        Object[] data = {
                "jolokia/jolokia_demo",
                r().repository("jolokia/jolokia_demo")
                   .fullName("jolokia/jolokia_demo").fullNameWithTag("jolokia/jolokia_demo:latest").simpleName("jolokia_demo").tag("latest"),

                "jolokia/jolokia_demo:0.9.6",
                r().repository("jolokia/jolokia_demo").tag("0.9.6")
                        .fullName("jolokia/jolokia_demo").fullNameWithTag("jolokia/jolokia_demo:0.9.6").simpleName("jolokia_demo"),

                "test.org/jolokia/jolokia_demo:0.9.6",
                r().registry("test.org").repository("jolokia/jolokia_demo").tag("0.9.6")
                        .fullName("test.org/jolokia/jolokia_demo").fullNameWithTag("test.org/jolokia/jolokia_demo:0.9.6").simpleName("jolokia_demo"),

                "test.org/jolokia/jolokia_demo",
                r().registry("test.org").repository("jolokia/jolokia_demo")
                        .fullName("test.org/jolokia/jolokia_demo").fullNameWithTag("test.org/jolokia/jolokia_demo:latest").simpleName("jolokia_demo").tag("latest"),

                "test.org:8000/jolokia/jolokia_demo:8.0",
                r().registry("test.org:8000").repository("jolokia/jolokia_demo").tag("8.0")
                        .fullName("test.org:8000/jolokia/jolokia_demo").fullNameWithTag("test.org:8000/jolokia/jolokia_demo:8.0").simpleName("jolokia_demo"),

                "jolokia_demo",
                r().repository("jolokia_demo")
                        .fullName("jolokia_demo").fullNameWithTag("jolokia_demo:latest").simpleName("jolokia_demo").tag("latest"),

                "jolokia_demo:0.9.6",
                r().repository("jolokia_demo").tag("0.9.6")
                        .fullName("jolokia_demo").fullNameWithTag("jolokia_demo:0.9.6").simpleName("jolokia_demo"),

                "consol/tomcat-8.0:8.0.9",
                r().repository("consol/tomcat-8.0").tag("8.0.9")
                        .fullName("consol/tomcat-8.0").fullNameWithTag("consol/tomcat-8.0:8.0.9").simpleName("tomcat-8.0"),

                "test.org/user/subproject/image:latest",
                r().registry("test.org").repository("user/subproject/image").tag("latest")
                        .fullName("test.org/user/subproject/image").fullNameWithTag("test.org/user/subproject/image:latest").simpleName("subproject/image")

        };

        verifyData(data);
    }

    @Test
    void testMultipleSubComponents() {
        Object[] data = {
                "org/jolokia/jolokia_demo",
                r().repository("org/jolokia/jolokia_demo")
                        .fullName("org/jolokia/jolokia_demo").fullNameWithTag("org/jolokia/jolokia_demo:latest").simpleName("jolokia/jolokia_demo").tag("latest"),

                "org/jolokia/jolokia_demo:0.9.6",
                r().repository("org/jolokia/jolokia_demo").tag("0.9.6")
                        .fullName("org/jolokia/jolokia_demo").fullNameWithTag("org/jolokia/jolokia_demo:0.9.6").simpleName("jolokia/jolokia_demo"),

                "repo.example.com/org/jolokia/jolokia_demo:0.9.6",
                r().registry("repo.example.com").repository("org/jolokia/jolokia_demo").tag("0.9.6")
                        .fullName("repo.example.com/org/jolokia/jolokia_demo").fullNameWithTag("repo.example.com/org/jolokia/jolokia_demo:0.9.6").simpleName("jolokia/jolokia_demo"),

                "repo.example.com/org/jolokia/jolokia_demo",
                r().registry("repo.example.com").repository("org/jolokia/jolokia_demo")
                        .fullName("repo.example.com/org/jolokia/jolokia_demo").fullNameWithTag("repo.example.com/org/jolokia/jolokia_demo:latest").simpleName("jolokia/jolokia_demo").tag("latest"),

                "repo.example.com:8000/org/jolokia/jolokia_demo:8.0",
                r().registry("repo.example.com:8000").repository("org/jolokia/jolokia_demo").tag("8.0")
                        .fullName("repo.example.com:8000/org/jolokia/jolokia_demo").fullNameWithTag("repo.example.com:8000/org/jolokia/jolokia_demo:8.0").simpleName("jolokia/jolokia_demo"),
                "org/jolokia_demo",
                r().repository("org/jolokia_demo")
                        .fullName("org/jolokia_demo").fullNameWithTag("org/jolokia_demo:latest").simpleName("jolokia_demo").tag("latest"),

                "org/jolokia_demo:0.9.6",
                r().repository("org/jolokia_demo").tag("0.9.6")
                        .fullName("org/jolokia_demo").fullNameWithTag("org/jolokia_demo:0.9.6").simpleName("jolokia_demo"),
        };

        verifyData(data);
    }

    private void verifyData(Object[] data) {
        for (int i = 0; i < data.length; i += 2) {
            ImageName name = new ImageName((String) data[i]);
            Res res = (Res) data[i + 1];
            assertThat(name.getRegistry()).as("Registry " + i).isEqualTo(res.registry);
            assertThat(name.getRepository()).as("Repository " + i).isEqualTo(res.repository);
            assertThat(name.getTag()).as("Tag " + i).isEqualTo(res.tag);
            assertThat(name.getNameWithoutTag(null)).as("RepoWithRegistry " + i).isEqualTo(res.fullName);
            assertThat(name.getFullName(null)).as("FullName " + i).isEqualTo(res.fullNameWithTag);
            assertThat(name.getSimpleName()).as("Simple Name " + i).isEqualTo(res.simpleName);
        }
    }

    @Test
    void testIllegalFormat() {
        assertThrows(IllegalArgumentException.class, () -> new ImageName(""));

        // New test for too long repository name
        String tooLongName = generateTooLongImageName();
        assertThatIllegalArgumentException()
            .as("Too long image name should fail")
            .isThrownBy(() -> new ImageName(tooLongName))
            .withMessageContaining("Repository name must not be more than 255 characters");
    }

    private String generateTooLongImageName() {
        StringBuilder tooLongName = new StringBuilder();
        int maxLength = 255 + 1; // exceeding the maximum length
        for (int i = 0; i < maxLength; i++) {
            tooLongName.append("a");
        }
        return tooLongName.toString();
    }

    @Test
    void namesUsedByDockerTests() {
        StringBuilder longTag = new StringBuilder();
        for (int i = 0; i < 130; i++) {
            longTag.append("a");
        }
        String[] illegal = {
            "fo$z$", "Foo@3cc", "Foo$3", "Foo*3", "Fo^3", "Foo!3", "F)xcz(", "fo%asd", "FOO/bar",
            "repo:fo$z$", "repo:Foo@3cc", "repo:Foo$3", "repo:Foo*3", "repo:Fo^3", "repo:Foo!3",
            "repo:%goodbye", "repo:#hashtagit", "repo:F)xcz(", "repo:-foo", "repo:..","repo:" + longTag.toString(),
            "-busybox:test", "-test/busybox:test", "-index:5000/busybox:test"

        };

        for (String i : illegal) {
            assertThatIllegalArgumentException()
                    .as("Name '%s' should fail", i)
                    .isThrownBy(() -> new ImageName(i));
        }

        String[] legal = {
            "fooo/bar", "fooaa/test", "foooo:t", "HOSTNAME.DOMAIN.COM:443/foo/bar"
        };

        for (String l : legal) {
            new ImageName(l);
        }
    }

    @Test
    void testImageNameWithUsernameHavingPeriods() {
        // Given
        final String name = "quay.io/roman.gordill/customer-service-cache:latest";
        // When
        final ImageName result = new ImageName(name);
        // Then
        assertThat(result)
          .isNotNull()
          .hasFieldOrPropertyWithValue("repository", "roman.gordill/customer-service-cache")
          .hasFieldOrPropertyWithValue("tag", "latest")
          .hasFieldOrPropertyWithValue("registry", "quay.io")
          .returns("roman.gordill", ImageName::inferUser);
    }

    @ParameterizedTest
    @CsvSource({
        "foo.com/customer-service-cache:latest,foo.com,customer-service-cache,latest",
        "myregistry.127.0.0.1.nip.io/eclipse-temurin:11.0.18_10-jdk-alpine,myregistry.127.0.0.1.nip.io,eclipse-temurin,11.0.18_10-jdk-alpine"
    })
    void testImageNameWithRegistry(String name, String registry, String repository, String tag) {
        // Given
        // When
        ImageName imageName = new ImageName(name);

        // Then
        assertThat(imageName).isNotNull();
        assertThat(imageName.inferUser()).isNull();
        assertThat(imageName.getRegistry()).isEqualTo(registry);
        assertThat(imageName.getRepository()).isEqualTo(repository);
        assertThat(imageName.getTag()).isEqualTo(tag);
    }

    @Test
    void testImageNameWithNameContainingRegistryAndName() {
        // Given
        String name = "foo.com:5000/customer-service-cache:latest";

        // When
        ImageName imageName = new ImageName(name);

        // Then
        assertThat(imageName).isNotNull();
        assertThat(imageName.inferUser()).isNull();
        assertThat(imageName.getRegistry()).isEqualTo("foo.com:5000");
        assertThat(imageName.getRepository()).isEqualTo("customer-service-cache");
        assertThat(imageName.getTag()).isEqualTo("latest");
    }

    @DisplayName("getFullName should add registry to image name when valid registry is provided")
    @ParameterizedTest(name = "With Image  \"{0}\" and Registry  \"{1}\" should Return  \"{2}\"")
    @MethodSource("registryNamingTestData")
    void getFullName_whenRegistryProvided_shouldReturnImageNameWithRegistry(String imageName, String registry, String expected) {
        assertThat(new ImageName(imageName).getFullName(registry)).isEqualTo(expected);
    }

    public static Stream<Arguments> registryNamingTestData() {
        return Stream.of(
                Arguments.arguments("jolokia/jolokia_demo:0.18", "docker.jolokia.org", "docker.jolokia.org/jolokia/jolokia_demo:0.18"),
                Arguments.arguments("jolokia/jolokia_demo", "docker.jolokia.org", "docker.jolokia.org/jolokia/jolokia_demo:latest"),
                Arguments.arguments("jolokia/jolokia_demo", null, "jolokia/jolokia_demo:latest"),
                Arguments.arguments("docker.jolokia.org/jolokia/jolokia_demo", "another.registry.org", "docker.jolokia.org/jolokia/jolokia_demo:latest"),
                Arguments.arguments("docker.jolokia.org/jolokia/jolokia_demo", null, "docker.jolokia.org/jolokia/jolokia_demo:latest"),
                Arguments.arguments("org/jolokia/jolokia_demo:0.18", "docker.jolokia.org", "docker.jolokia.org/org/jolokia/jolokia_demo:0.18"),
                Arguments.arguments("org/jolokia/jolokia_demo", "docker.jolokia.org", "docker.jolokia.org/org/jolokia/jolokia_demo:latest"),
                Arguments.arguments("org/jolokia/jolokia_demo", null, "org/jolokia/jolokia_demo:latest"),
                Arguments.arguments("docker.jolokia.org/org/jolokia/jolokia_demo", "another.registry.org", "docker.jolokia.org/org/jolokia/jolokia_demo:latest"),
                Arguments.arguments("docker.jolokia.org/org/jolokia/jolokia_demo", null, "docker.jolokia.org/org/jolokia/jolokia_demo:latest")

        );
    }

    // =======================================================================================
    private static Res r() {
        return new Res();
    }

    private static class Res {
        private String registry,repository,tag, fullName, fullNameWithTag, simpleName;
        boolean hasRegistry = false;

        Res registry(String registry) {
            this.registry = registry;
            this.hasRegistry = registry != null;
            return this;
        }

        Res repository(String repository) {
            this.repository = repository;
            return this;
        }

        Res tag(String tag) {
            this.tag = tag;
            return this;
        }

        Res fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        Res fullNameWithTag(String fullNameWithTag) {
            this.fullNameWithTag = fullNameWithTag;
            return this;
        }

        Res simpleName(String simpleName) {
            this.simpleName = simpleName;
            return this;
        }
    }
}
