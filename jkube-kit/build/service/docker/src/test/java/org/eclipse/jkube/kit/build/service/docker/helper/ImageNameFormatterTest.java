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
package org.eclipse.jkube.kit.build.service.docker.helper;
import org.eclipse.jkube.kit.common.JavaProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.eclipse.jkube.kit.config.image.ImageName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Date;
import java.util.Properties;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * @author roland
 */
class ImageNameFormatterTest {
    private JavaProject project;

    private ImageNameFormatter formatter;

    @BeforeEach
    void setUp(){
        project = JavaProject.builder()
          .properties(new Properties())
          .build();
        formatter = new ImageNameFormatter(project, new Date());
    }
    @Test
    void simple() {
        assertThat(formatter.format("bla")).isEqualTo("bla");
    }

    @Test
    void invalidFormatChar() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> formatter.format("bla %z"))
                .withMessage("No image name format element '%z' known");
    }

    @ParameterizedTest(name = "with groupId = ''{0}'' shoud return ''{1}''")
    @DisplayName("format with %g")
    @MethodSource("formatWithPercentGData")
    void formatWithPercentG(String groupId, String expectedName) {
        project.setGroupId(groupId);
        final String result = formatter.format("%g");
        assertThat(result).isEqualTo(expectedName);
    }

    static Stream<Arguments> formatWithPercentGData() {
        return Stream.of(
          arguments("io.fabric8", "fabric8"),
          arguments("io.FABRIC8", "fabric8"),
          arguments("io.fabric8.", "fabric8"),
          arguments("io.fabric8", "fabric8"),
          arguments("fabric8....", "fabric8"),
          arguments("io.fabric8___", "fabric8__")
        );
    }

    @Test
    void artifact() {
        project.setArtifactId("Docker....Maven.....Plugin");
        project.setProperties(new Properties());

        assertThat(formatter.format("--> %a <--")).isEqualTo("--> docker.maven.plugin <--");
    }

    @Test
    void tagWithProperty() {
        // Given
        project.getProperties().put("jkube.image.tag", "1.2.3");
        // When
        final String result = formatter.format("%t");
        // Then
        assertThat(result).isEqualTo("1.2.3");
    }

    @Test
    void tagWithSnapshot() {
        project.setArtifactId("docker-maven-plugin");
        project.setGroupId("io.fabric8");
        project.setVersion("1.2.3-SNAPSHOT");
        assertThat(formatter.format("%g/%a:%l")).isEqualTo("fabric8/docker-maven-plugin:latest");
        assertThat(formatter.format("%g/%a:%v")).isEqualTo("fabric8/docker-maven-plugin:1.2.3-SNAPSHOT");
        assertThat(formatter.format("%g/%a:%t")).matches(".*snapshot-[\\d-]+$");
    }

    @Test
    void tagWithNonSnapshotArtifact() {
        project.setArtifactId("docker-maven-plugin");
        project.setGroupId("io.fabric8");
        project.setVersion("1.2.3");
        assertThat(formatter.format("%g/%a:%l")).isEqualTo("fabric8/docker-maven-plugin:1.2.3");
        assertThat(formatter.format("%g/%a:%v")).isEqualTo("fabric8/docker-maven-plugin:1.2.3");
        assertThat(formatter.format("%g/%a:%t")).isEqualTo("fabric8/docker-maven-plugin:1.2.3");
    }

    @Test
    void snapshotVersion() {
        project.setArtifactId("kubernetes-maven-plugin");
        project.setGroupId("org.eclipse.jkube");
        project.setVersion("1.2.3-SNAPSHOT");
        project.setProperties(new Properties());
        assertThat(formatter.format("%g/%a:%l"))
            .isEqualTo("jkube/kubernetes-maven-plugin:latest")
            .satisfies(ImageNameFormatterTest::validImageName);
        assertThat(formatter.format("%g/%a:%v"))
            .isEqualTo("jkube/kubernetes-maven-plugin:1.2.3-SNAPSHOT")
            .satisfies(ImageNameFormatterTest::validImageName);
        assertThat(formatter.format("%g/%a:%t"))
            .matches("^jkube/kubernetes-maven-plugin:snapshot-\\d{6}-\\d{6}-\\d{4}$")
            .satisfies(ImageNameFormatterTest::validImageName);
    }

    @Test
    void snapshotVersionWithSemVerBuildmetadata() {
        project.setArtifactId("kubernetes-maven-plugin");
        project.setGroupId("org.eclipse.jkube");
        project.setVersion("1.2.3-SNAPSHOT+semver.build_meta-data");
        project.setProperties(new Properties());
        assertThat(formatter.format("%g/%a:%l"))
            .isEqualTo("jkube/kubernetes-maven-plugin:latest-semver.build_meta-data")
            .satisfies(ImageNameFormatterTest::validImageName);
        assertThat(formatter.format("%g/%a:%v"))
            .isEqualTo("jkube/kubernetes-maven-plugin:1.2.3-SNAPSHOT-semver.build_meta-data")
            .satisfies(ImageNameFormatterTest::validImageName);
        assertThat(formatter.format("%g/%a:%t"))
            .matches("^jkube/kubernetes-maven-plugin:snapshot-\\d{6}-\\d{6}-\\d{4}-semver\\.build_meta-data$")
            .satisfies(ImageNameFormatterTest::validImageName);
    }

    @Test
    void plusSubstitute() {
        final Properties properties = new Properties();
        properties.put(ImageNameFormatter.SEMVER_PLUS_SUBSTITUTION, "_");
        project.setArtifactId("kubernetes-maven-plugin");
        project.setGroupId("org.eclipse.jkube");
        project.setVersion("1.2.3+semver.build_meta-data");
        project.setProperties(properties);
        assertThat(formatter.format("%g/%a:%l"))
            .isEqualTo("jkube/kubernetes-maven-plugin:1.2.3_semver.build_meta-data")
            .satisfies(ImageNameFormatterTest::validImageName);
        assertThat(formatter.format("%g/%a:%v"))
            .isEqualTo("jkube/kubernetes-maven-plugin:1.2.3_semver.build_meta-data")
            .satisfies(ImageNameFormatterTest::validImageName);
        assertThat(formatter.format("%g/%a:%t"))
            .isEqualTo("jkube/kubernetes-maven-plugin:1.2.3_semver.build_meta-data")
            .satisfies(ImageNameFormatterTest::validImageName);
    }

    @Test
    void plusSubstituteIsPlus() {
        final Properties properties = new Properties();
        properties.put(ImageNameFormatter.SEMVER_PLUS_SUBSTITUTION, "+");
        project.setArtifactId("kubernetes-maven-plugin");
        project.setGroupId("org.eclipse.jkube");
        project.setVersion("1.2.3+semver.build_meta-data");
        project.setProperties(properties);
        assertThat(formatter.format("%g/%a:%l"))
            .isEqualTo("jkube/kubernetes-maven-plugin:1.2.3-semver.build_meta-data")
            .satisfies(ImageNameFormatterTest::validImageName);
        assertThat(formatter.format("%g/%a:%v"))
            .isEqualTo("jkube/kubernetes-maven-plugin:1.2.3-semver.build_meta-data")
            .satisfies(ImageNameFormatterTest::validImageName);
        assertThat(formatter.format("%g/%a:%t"))
            .isEqualTo("jkube/kubernetes-maven-plugin:1.2.3-semver.build_meta-data")
            .satisfies(ImageNameFormatterTest::validImageName);
    }

    @Test
    void releaseVersion() {
        project.setArtifactId("kubernetes-maven-plugin");
        project.setGroupId("org.eclipse.jkube");
        project.setVersion("1.2.3");
        project.setProperties(new Properties());
        assertThat(formatter.format("%g/%a:%l"))
            .isEqualTo("jkube/kubernetes-maven-plugin:1.2.3")
            .satisfies(ImageNameFormatterTest::validImageName);
        assertThat(formatter.format("%g/%a:%v"))
            .isEqualTo("jkube/kubernetes-maven-plugin:1.2.3")
            .satisfies(ImageNameFormatterTest::validImageName);
        assertThat(formatter.format("%g/%a:%t"))
            .isEqualTo("jkube/kubernetes-maven-plugin:1.2.3")
            .satisfies(ImageNameFormatterTest::validImageName);
    }

    @Test
    void groupIdWithProperty() {
        // Given
        project.getProperties().put("jkube.image.user","this.it..is");
        // When
        final String result = formatter.format("%g/name");
        // Then
        assertThat(result).isEqualTo("this.it..is/name");
    }

    @Test
    void format_whenPropertyInImageName_thenResolveProperty() {
        // Given
        project.getProperties().put("git.commit.id.abbrev", "der12");
        // When
        final String result = formatter.format("registry.gitlab.com/myproject/myrepo/mycontainer:${git.commit.id.abbrev}");
        // Then
        assertThat(result).isEqualTo("registry.gitlab.com/myproject/myrepo/mycontainer:der12");
    }

    private static void validImageName(String v) {
        assertThatCode(() -> ImageName.validate(v)).doesNotThrowAnyException();
    }
}
