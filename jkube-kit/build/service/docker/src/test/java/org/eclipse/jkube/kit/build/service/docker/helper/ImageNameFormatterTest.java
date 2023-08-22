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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Date;
import java.util.Properties;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
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
          .artifactId("kubernetes-maven-plugin")
          .groupId("org.eclipse.jkube")
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

    @ParameterizedTest(name = "with groupId = ''{0}'' should return ''{1}''")
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
        project.setArtifactId("Kubernetes....Maven.....Plugin");

        assertThat(formatter.format("--> %a <--")).isEqualTo("--> kubernetes.maven.plugin <--");
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

    @ParameterizedTest(name = "version = {0}, format = {1} should generate image name {2}")
    @CsvSource({
        "1.2.3-SNAPSHOT,%g/%a:%l,jkube/kubernetes-maven-plugin:latest",
        "1.2.3-SNAPSHOT,%g/%a:%v,jkube/kubernetes-maven-plugin:1.2.3-SNAPSHOT",
        "1.2.3-SNAPSHOT+semver.build_meta-data,%g/%a:%l,jkube/kubernetes-maven-plugin:latest-semver.build_meta-data",
        "1.2.3-SNAPSHOT+semver.build_meta-data,%g/%a:%v,jkube/kubernetes-maven-plugin:1.2.3-SNAPSHOT-semver.build_meta-data",
        "1.2.3,%g/%a:%l,jkube/kubernetes-maven-plugin:1.2.3",
        "1.2.3,%g/%a:%v,jkube/kubernetes-maven-plugin:1.2.3",
        "1.2.3,%g/%a:%t,jkube/kubernetes-maven-plugin:1.2.3"
    })
    void format_whenVersionAndImageFormatProvided_shouldGenerateImageName(String version, String imageFormat, String result) {
        project.setVersion(version);
        assertThat(formatter.format(imageFormat))
            .isEqualTo(result)
            .satisfies(i -> assertThat(new ImageName(i)).isNotNull());
    }

    @Test
    void format_whenVersionAndImageTagInTimestampFormat_thenShouldGenerateImageName() {
        // Given
        project.setVersion("1.2.3-SNAPSHOT");
        // When + Then
        assertThat(formatter.format("%g/%a:%t"))
            .matches("^jkube/kubernetes-maven-plugin:snapshot-\\d{6}-\\d{6}-\\d{4}$")
            .satisfies(i -> assertThat(new ImageName(i)).isNotNull());
    }

    @Test
    void snapshotVersionWithSemVerBuildmetadata() {
        // Given
        project.setVersion("1.2.3-SNAPSHOT+semver.build_meta-data");
        // When + Then
        assertThat(formatter.format("%g/%a:%t"))
            .matches("^jkube/kubernetes-maven-plugin:snapshot-\\d{6}-\\d{6}-\\d{4}-semver\\.build_meta-data$")
            .satisfies(i -> assertThat(new ImageName(i)).isNotNull());
    }

    @ParameterizedTest(name = "given jkube.image.tag.semver_plus_substitution=_ image format = {0}, then + replaced with _ in image name")
    @ValueSource(strings = {"%g/%a:%l", "%g/%a:%v", "%g/%a:%t"})
    void plusSubstitute(String imageFormat) {
        // Given
        project.getProperties().put("jkube.image.tag.semver_plus_substitution", "_");
        project.setVersion("1.2.3+semver.build_meta-data");
        // When + Then
        assertThat(formatter.format(imageFormat))
            .isEqualTo("jkube/kubernetes-maven-plugin:1.2.3_semver.build_meta-data")
            .satisfies(i -> assertThat(new ImageName(i)).isNotNull());
    }

    @ParameterizedTest(name = "given jkube.image.tag.semver_plus_substitution=+ image format = {0}, then property ignored and + replaced with _ in image name")
    @ValueSource(strings = {"%g/%a:%l", "%g/%a:%v", "%g/%a:%t"})
    void plusSubstituteIsPlus(String imageFormat) {
        // Given
        project.getProperties().put("jkube.image.tag.semver_plus_substitution", "+");
        project.setVersion("1.2.3+semver.build_meta-data");
        // When + Then
        assertThat(formatter.format(imageFormat))
            .isEqualTo("jkube/kubernetes-maven-plugin:1.2.3-semver.build_meta-data")
            .satisfies(i -> assertThat(new ImageName(i)).isNotNull());
    }

    @Test
    void groupIdWithProperty() {
        // Given
        project.getProperties().put("jkube.image.user", "this.it..is");
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
}
