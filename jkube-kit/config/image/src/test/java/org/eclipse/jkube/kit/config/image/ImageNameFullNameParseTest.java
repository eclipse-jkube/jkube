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
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageNameFullNameParseTest {

  @ParameterizedTest(name ="{index}: With full name ''{0}'' should return same or complete full name (''{1}'')")
  @DisplayName("getFullName")
  @MethodSource("getFullNameData")
  void getFullName(String providedFullName, String expectedFullName) {
    assertThat(new ImageName(providedFullName).getFullName()).isEqualTo(expectedFullName);
  }

  static Stream<Arguments> getFullNameData() {
    return Stream.of(
      Arguments.of("eclipse/eclipse_jkube:latest", "eclipse/eclipse_jkube:latest"),
      Arguments.of("eclipse/eclipse_jkube:1.3.3.7", "eclipse/eclipse_jkube:1.3.3.7"),
      Arguments.of("docker.io/eclipse/eclipse_jkube", "docker.io/eclipse/eclipse_jkube:latest"),
      Arguments.of("long.registry.example.com:8080/valid.name--with__separators/eclipse_jkube", "long.registry.example.com:8080/valid.name--with__separators/eclipse_jkube:latest"),
      Arguments.of("docker.io/eclipse/eclipse_jkube:1.33.7", "docker.io/eclipse/eclipse_jkube:1.33.7"),
      Arguments.of("long.registry.example.com:8080/valid.name--with__separators/eclipse_jkube:very--special__tag.2.A", "long.registry.example.com:8080/valid.name--with__separators/eclipse_jkube:very--special__tag.2.A"),
      Arguments.of("eclipse-temurin:11", "eclipse-temurin:11"),
      Arguments.of("user/my-repo_z:11", "user/my-repo_z:11"),
      Arguments.of("foo-bar-registry.jfrog.io/java:jre-17", "foo-bar-registry.jfrog.io/java:jre-17"),
      Arguments.of("user.jfrog.io/my-jkube/openjdk:jre-17", "user.jfrog.io/my-jkube/openjdk:jre-17"),
      Arguments.of("quay.io/jkube/jkube-java:0.0.19@sha256:b7d8650e04b282b9d7b94daedf38321512f9910bce896cd40ffa15b1b92bab17", "quay.io/jkube/jkube-java:0.0.19@sha256:b7d8650e04b282b9d7b94daedf38321512f9910bce896cd40ffa15b1b92bab17")
    );
  }

  @ParameterizedTest(name ="{index}: With full name ''{0}'' should return ''{1}'' as repository")
  @DisplayName("getRepository")
  @MethodSource("getRepositoryData")
  void getRepository(String providedFullName, String expectedRepository) {
    assertThat(new ImageName(providedFullName).getRepository()).isEqualTo(expectedRepository);
  }

  static Stream<Arguments> getRepositoryData() {
    return Stream.of(
      Arguments.of("eclipse/eclipse_jkube:latest", "eclipse/eclipse_jkube"),
      Arguments.of("eclipse/eclipse_jkube:1.3.3.7", "eclipse/eclipse_jkube"),
      Arguments.of("docker.io/eclipse/eclipse_jkube", "eclipse/eclipse_jkube"),
      Arguments.of("long.registry.example.com:8080/valid.name--with__separators/eclipse_jkube", "valid.name--with__separators/eclipse_jkube"),
      Arguments.of("docker.io/eclipse/eclipse_jkube:1.33.7", "eclipse/eclipse_jkube"),
      Arguments.of("long.registry.example.com:8080/valid.name--with__separators/eclipse_jkube:very--special__tag.2.A", "valid.name--with__separators/eclipse_jkube"),
      Arguments.of("eclipse-temurin:11", "eclipse-temurin"),
      Arguments.of("user/my-repo_z:11", "user/my-repo_z"),
      Arguments.of("foo-bar-registry.jfrog.io/java:jre-17", "java"),
      Arguments.of("user.jfrog.io/my-jkube/openjdk:jre-17", "my-jkube/openjdk"),
      Arguments.of("quay.io/jkube/jkube-java:0.0.19@sha256:b7d8650e04b282b9d7b94daedf38321512f9910bce896cd40ffa15b1b92bab17", "jkube/jkube-java")
    );
  }

  @ParameterizedTest(name ="{index}: With full name ''{0}'' should return ''{1}'' as registry")
  @DisplayName("getRegistry")
  @MethodSource("getRegistryData")
  void getRegistry(String providedFullName, String expectedRegistry) {
    assertThat(new ImageName(providedFullName).getRegistry()).isEqualTo(expectedRegistry);
  }

  static Stream<Arguments> getRegistryData() {
    return Stream.of(
      Arguments.of("eclipse/eclipse_jkube:latest", null),
      Arguments.of("eclipse/eclipse_jkube:1.3.3.7", null),
      Arguments.of("docker.io/eclipse/eclipse_jkube", "docker.io"),
      Arguments.of("long.registry.example.com:8080/valid.name--with__separators/eclipse_jkube", "long.registry.example.com:8080"),
      Arguments.of("docker.io/eclipse/eclipse_jkube:1.33.7", "docker.io"),
      Arguments.of("long.registry.example.com:8080/valid.name--with__separators/eclipse_jkube:very--special__tag.2.A", "long.registry.example.com:8080"),
      Arguments.of("eclipse-temurin:11", null),
      Arguments.of("user/my-repo_z:11", null),
      Arguments.of("foo-bar-registry.jfrog.io/java:jre-17", "foo-bar-registry.jfrog.io"),
      Arguments.of("user.jfrog.io/my-jkube/openjdk:jre-17", "user.jfrog.io"),
      Arguments.of("quay.io/jkube/jkube-java:0.0.19@sha256:b7d8650e04b282b9d7b94daedf38321512f9910bce896cd40ffa15b1b92bab17", "quay.io")
    );
  }

  @ParameterizedTest(name ="{index}: With full name ''{0}'' should return ''{1}'' as tag")
  @DisplayName("getTag")
  @MethodSource("getTagData")
  void getTag(String providedFullName, String expectedTag) {
    assertThat(new ImageName(providedFullName).getTag()).isEqualTo(expectedTag);
  }

  static Stream<Arguments> getTagData() {
    return Stream.of(
      Arguments.of("eclipse/eclipse_jkube:latest", "latest"),
      Arguments.of("eclipse/eclipse_jkube:1.3.3.7", "1.3.3.7"),
      Arguments.of("docker.io/eclipse/eclipse_jkube", "latest"),
      Arguments.of("long.registry.example.com:8080/valid.name--with__separators/eclipse_jkube", "latest"),
      Arguments.of("docker.io/eclipse/eclipse_jkube:1.33.7", "1.33.7"),
      Arguments.of("long.registry.example.com:8080/valid.name--with__separators/eclipse_jkube:very--special__tag.2.A", "very--special__tag.2.A"),
      Arguments.of("eclipse-temurin:11", "11"),
      Arguments.of("user/my-repo_z:11", "11"),
      Arguments.of("foo-bar-registry.jfrog.io/java:jre-17", "jre-17"),
      Arguments.of("user.jfrog.io/my-jkube/openjdk:jre-17", "jre-17"),
      Arguments.of("quay.io/jkube/jkube-java:0.0.19@sha256:b7d8650e04b282b9d7b94daedf38321512f9910bce896cd40ffa15b1b92bab17", "0.0.19")
    );
  }

  @ParameterizedTest(name ="{index}: With full name ''{0}'' should return ''{1}'' as digest")
  @DisplayName("getDigest")
  @MethodSource("getDigestData")
  void getDigest(String providedFullName, String expectedDigest) {
    assertThat(new ImageName(providedFullName).getDigest()).isEqualTo(expectedDigest);
  }

  static Stream<Arguments> getDigestData() {
    return Stream.of(
      Arguments.of("eclipse/eclipse_jkube:latest", null),
      Arguments.of("eclipse/eclipse_jkube:1.3.3.7", null),
      Arguments.of("docker.io/eclipse/eclipse_jkube", null),
      Arguments.of("long.registry.example.com:8080/valid.name--with__separators/eclipse_jkube", null),
      Arguments.of("docker.io/eclipse/eclipse_jkube:1.33.7", null),
      Arguments.of("long.registry.example.com:8080/valid.name--with__separators/eclipse_jkube:very--special__tag.2.A", null),
      Arguments.of("eclipse-temurin:11", null),
      Arguments.of("user/my-repo_z:11", null),
      Arguments.of("foo-bar-registry.jfrog.io/java:jre-17", null),
      Arguments.of("user.jfrog.io/my-jkube/openjdk:jre-17", null),
      Arguments.of("quay.io/jkube/jkube-java:0.0.19@sha256:b7d8650e04b282b9d7b94daedf38321512f9910bce896cd40ffa15b1b92bab17", "sha256:b7d8650e04b282b9d7b94daedf38321512f9910bce896cd40ffa15b1b92bab17")
    );
  }

  @ParameterizedTest(name ="{index}: With full name ''{0}'' should return ''{1}'' as inferred user")
  @DisplayName("inferUser")
  @MethodSource("inferUserData")
  void getUser(String providedFullName, String expectedUser) {
    assertThat(new ImageName(providedFullName).inferUser()).isEqualTo(expectedUser);
  }

  static Stream<Arguments> inferUserData() {
    return Stream.of(
      Arguments.of("eclipse/eclipse_jkube:latest", "eclipse"),
      Arguments.of("eclipse/eclipse_jkube:1.3.3.7", "eclipse"),
      Arguments.of("docker.io/eclipse/eclipse_jkube", "eclipse"),
      Arguments.of("long.registry.example.com:8080/valid.name--with__separators/eclipse_jkube", "valid.name--with__separators"),
      Arguments.of("docker.io/eclipse/eclipse_jkube:1.33.7", "eclipse"),
      Arguments.of("long.registry.example.com:8080/valid.name--with__separators/eclipse_jkube:very--special__tag.2.A", "valid.name--with__separators"),
      Arguments.of("eclipse-temurin:11", null),
      Arguments.of("user/my-repo_z:11", "user"),
      Arguments.of("foo-bar-registry.jfrog.io/java:jre-17", null),
      Arguments.of("user.jfrog.io/my-jkube/openjdk:jre-17", "my-jkube"),
      Arguments.of("quay.io/jkube/jkube-java:0.0.19@sha256:b7d8650e04b282b9d7b94daedf38321512f9910bce896cd40ffa15b1b92bab17", "jkube")
    );
  }

  @ParameterizedTest(name ="{index}: With full name ''{0}'' should return ''{1}'' as simple name")
  @DisplayName("getSimpleName")
  @MethodSource("getSimpleNameData")
  void getSimpleName(String providedFullName, String expectedSimpleName) {
    assertThat(new ImageName(providedFullName).getSimpleName()).isEqualTo(expectedSimpleName);
  }

  static Stream<Arguments> getSimpleNameData() {
    return Stream.of(
      Arguments.of("eclipse/eclipse_jkube:latest", "eclipse_jkube"),
      Arguments.of("eclipse/eclipse_jkube:1.3.3.7", "eclipse_jkube"),
      Arguments.of("docker.io/eclipse/eclipse_jkube", "eclipse_jkube"),
      Arguments.of("long.registry.example.com:8080/valid.name--with__separators/eclipse_jkube", "eclipse_jkube"),
      Arguments.of("docker.io/eclipse/eclipse_jkube:1.33.7", "eclipse_jkube"),
      Arguments.of("long.registry.example.com:8080/valid.name--with__separators/eclipse_jkube:very--special__tag.2.A", "eclipse_jkube"),
      Arguments.of("eclipse-temurin:11", "eclipse-temurin"),
      Arguments.of("user/my-repo_z:11", "my-repo_z"),
      Arguments.of("foo-bar-registry.jfrog.io/java:jre-17", "java"),
      Arguments.of("user.jfrog.io/my-jkube/openjdk:jre-17", "openjdk"),
      Arguments.of("quay.io/jkube/jkube-java:0.0.19@sha256:b7d8650e04b282b9d7b94daedf38321512f9910bce896cd40ffa15b1b92bab17", "jkube-java")
    );
  }
  @Test
  @DisplayName("With special characters in name, should throw Exception")
  void withSpecialCharactersInName() {
    assertThatThrownBy(() -> new ImageName("invalid.name-with__separators/eclipse_-jkube"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageStartingWith("Given Docker name 'invalid.name-with__separators/eclipse_-jkube:latest' is invalid");
  }
  @Test
  @DisplayName("With special characters in tag, should throw Exception")
  void withSpecialCharactersInTag() {
    assertThatThrownBy(() -> new ImageName("invalid.name-with__separators/eclipse_jkube:valid!__tag-4.2"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageStartingWith("Given Docker name 'invalid.name-with__separators/eclipse_jkube:valid!__tag-4.2' is invalid");
  }
  @Test
  @DisplayName("With null full name, should throw Exception")
  void withNullFullName() {
    assertThatThrownBy(() -> new ImageName(null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Image name must not be null");
  }

}
