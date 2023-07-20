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
package org.eclipse.jkube.kit.build.service.docker.access;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.security.PrivateKey;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.kit.build.service.docker.access.KeyStoreUtilTest.*;

class KeyStoreUtilLoadPrivateKeyTest {
  public static Stream<Arguments> privateKeyFiles() {
    return Stream.of(
            Arguments.of("Default", "keys/pkcs1.pem"),
            Arguments.of("PKCS8", "keys/pkcs8.pem"),
            Arguments.of("ECDSA", "keys/ecdsa.pem")
    );
  }

  @ParameterizedTest(name = "''{0}'' key should not be null")
  @MethodSource("privateKeyFiles")
  void loadPrivateKey(String testDesc, String privateKeyFile) throws Exception {
    PrivateKey privateKey = KeyStoreUtil.loadPrivateKey(getFile(privateKeyFile));
    assertThat(privateKey).isNotNull();
  }
}
