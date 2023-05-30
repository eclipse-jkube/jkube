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
package org.eclipse.jkube.kit.config.image;

import lombok.Builder;
import lombok.Value;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class ImageNameFullNameParseTest {

  public static Stream<Object[]> validData() {
    return Stream.of(
        new Object[][] {
            {
                "Repo and Name", "eclipse/eclipse_jkube",
                SimpleImageName.builder()
                    .fullName("eclipse/eclipse_jkube:latest")
                    .registry(null)
                    .user("eclipse")
                    .repository("eclipse/eclipse_jkube")
                    .tag("latest")
                    .digest(null)
                    .build()
            },
            {
                "Repo, Name and Tag", "eclipse/eclipse_jkube:1.3.3.7",
                SimpleImageName.builder()
                    .fullName("eclipse/eclipse_jkube:1.3.3.7")
                    .registry(null)
                    .user("eclipse")
                    .repository("eclipse/eclipse_jkube")
                    .tag("1.3.3.7")
                    .digest(null)
                    .build()
            },
            {
                "Registry, Repo and Name", "docker.io/eclipse/eclipse_jkube",
                SimpleImageName.builder()
                    .fullName("docker.io/eclipse/eclipse_jkube:latest")
                    .registry("docker.io")
                    .user("eclipse")
                    .repository("eclipse/eclipse_jkube")
                    .tag("latest")
                    .digest(null)
                    .build()
            },
            { "Registry, Repo and Name with special characters",
                "long.registry.example.com:8080/valid.name--with__separators/eclipse_jkube",
                SimpleImageName.builder()
                    .fullName("long.registry.example.com:8080/valid.name--with__separators/eclipse_jkube:latest")
                    .registry("long.registry.example.com:8080")
                    .user("valid.name--with__separators")
                    .repository("valid.name--with__separators/eclipse_jkube")
                    .tag("latest")
                    .digest(null)
                    .build()
            },
            { "Registry, Repo, Name and Tag", "docker.io/eclipse/eclipse_jkube:1.33.7",
                SimpleImageName.builder()
                    .fullName("docker.io/eclipse/eclipse_jkube:1.33.7")
                    .registry("docker.io")
                    .user("eclipse")
                    .repository("eclipse/eclipse_jkube")
                    .tag("1.33.7")
                    .digest(null)
                    .build()
            },
            {
                "Registry, Repo, Name and Tag with special characters",
                "long.registry.example.com:8080/valid.name--with__separators/eclipse_jkube:very--special__tag.2.A",
                SimpleImageName.builder()
                    .fullName(
                        "long.registry.example.com:8080/valid.name--with__separators/eclipse_jkube:very--special__tag.2.A")
                    .registry("long.registry.example.com:8080")
                    .user("valid.name--with__separators")
                    .repository("valid.name--with__separators/eclipse_jkube")
                    .tag("very--special__tag.2.A")
                    .digest(null)
                    .build()
            },
            {
                "Repo only", "eclipse-temurin:11",
                SimpleImageName.builder()
                    .fullName("eclipse-temurin:11")
                    .registry(null)
                    .user(null)
                    .repository("eclipse-temurin")
                    .tag("11")
                    .digest(null)
                    .build()
            },
            {
                "User and repo", "user/my-repo_z:11",
                SimpleImageName.builder()
                    .fullName("user/my-repo_z:11")
                    .registry(null)
                    .user("user")
                    .repository("user/my-repo_z")
                    .tag("11")
                    .digest(null)
                    .build()
            },
            {
                "Registry and repo", "foo-bar-registry.jfrog.io/java:jre-17",
                SimpleImageName.builder()
                    .fullName("foo-bar-registry.jfrog.io/java:jre-17")
                    .registry("foo-bar-registry.jfrog.io")
                    .user(null)
                    .repository("java")
                    .tag("jre-17")
                    .digest(null)
                    .build()
            },
            {
                "JFrog repo", "user.jfrog.io/my-jkube/openjdk:jre-17",
                SimpleImageName.builder()
                    .fullName("user.jfrog.io/my-jkube/openjdk:jre-17")
                    .registry("user.jfrog.io")
                    .user("my-jkube")
                    .repository("my-jkube/openjdk")
                    .tag("jre-17")
                    .digest(null)
                    .build()
            },
        });
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("validData")
  void shouldParseImageName(String testName, String providedImageName, SimpleImageName expectedImageName) {
    ImageName imageName = new ImageName(providedImageName);
    SimpleImageName currentImageName = SimpleImageName.toSimpleImageName(imageName);
    assertThat(currentImageName).isEqualTo(expectedImageName);
  }

  public static Stream<Object[]> invalidData() {
    return Stream.of(
        new Object[][] {
            {
                "Repo and Name with special characters", "invalid.name-with__separators/eclipse_-jkube"
            },
            {
                "Repo, Name and Tag with special characters", "invalid.name-with__separators/eclipse_jkube:valid!__tag-4.2"
            },
        });
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidData")
  void shouldFailedParseImageName(String testName, String providedImageName) {
    assertThatCode(() -> new ImageName(providedImageName))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Builder
  @Value
  public static class SimpleImageName {
    String fullName;
    String registry;
    String user;
    String repository;
    String tag;
    String digest;

    public static SimpleImageName toSimpleImageName(ImageName imageName) {
      return new SimpleImageName(imageName.getFullName(),
          imageName.getRegistry(),
          imageName.getUser(),
          imageName.getRepository(),
          imageName.getTag(),
          imageName.getDigest());
    }

  }
}
