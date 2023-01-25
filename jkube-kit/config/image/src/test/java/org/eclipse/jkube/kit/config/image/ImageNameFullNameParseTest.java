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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ImageNameFullNameParseTest {

  public static Stream<Object[]> data() {
    return Stream.of(
     new Object[][] {
        {"Repo and Name", "eclipse/eclipse_jkube",
            "eclipse/eclipse_jkube:latest", null, "eclipse", "eclipse/eclipse_jkube", "latest", null},
        {"Repo and Name with special characters", "valid.name-with__separators/eclipse_jkube",
            "valid.name-with__separators/eclipse_jkube:latest", null,
            "valid.name-with__separators", "valid.name-with__separators/eclipse_jkube", "latest", null},
        {"Repo, Name and Tag", "eclipse/eclipse_jkube:1.3.3.7",
            "eclipse/eclipse_jkube:1.3.3.7", null, "eclipse", "eclipse/eclipse_jkube", "1.3.3.7", null},
        {"Repo, Name and Tag with special characters", "valid.name-with__separators/eclipse_jkube:valid__tag-4.2",
            "valid.name-with__separators/eclipse_jkube:valid__tag-4.2", null,
            "valid.name-with__separators", "valid.name-with__separators/eclipse_jkube", "valid__tag-4.2", null},
        {"Registry, Repo and Name", "docker.io/eclipse/eclipse_jkube",
            "docker.io/eclipse/eclipse_jkube:latest", "docker.io", "eclipse", "eclipse/eclipse_jkube", "latest", null},
        {"Registry, Repo and Name with special characters",
            "long.registry.example.com:8080/valid.name--with__separators/eclipse_jkube",
            "long.registry.example.com:8080/valid.name--with__separators/eclipse_jkube:latest",
            "long.registry.example.com:8080",
            "valid.name--with__separators", "valid.name--with__separators/eclipse_jkube", "latest", null},
        {"Registry, Repo, Name and Tag", "docker.io/eclipse/eclipse_jkube:1.33.7",
            "docker.io/eclipse/eclipse_jkube:1.33.7", "docker.io", "eclipse", "eclipse/eclipse_jkube", "1.33.7", null},
        {"Registry, Repo, Name and Tag with special characters",
            "long.registry.example.com:8080/valid.name--with__separators/eclipse_jkube:very--special__tag.2.A",
            "long.registry.example.com:8080/valid.name--with__separators/eclipse_jkube:very--special__tag.2.A",
            "long.registry.example.com:8080",
            "valid.name--with__separators", "valid.name--with__separators/eclipse_jkube", "very--special__tag.2.A", null},
    });
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("data")
  void fullNameParse(String testName, String providedFullName, String expectedFullName, String registry, String user,
                     String repository, String tag, String digest) {
    final ImageName result = new ImageName(providedFullName);
    assertThat(result)
        .extracting(
            ImageName::getFullName,
            ImageName::getRegistry,
            ImageName::getUser,
            ImageName::getRepository,
            ImageName::getTag,
            ImageName::getDigest
        )
        .containsExactly(expectedFullName, registry, user, repository, tag, digest);
  }
}
