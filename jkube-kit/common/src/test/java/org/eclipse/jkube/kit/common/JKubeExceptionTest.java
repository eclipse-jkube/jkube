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
package org.eclipse.jkube.kit.common;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class JKubeExceptionTest {
  @Test
  void launderThrowable_whenInvoked_shouldWrapExceptionAsJKubeException() {
    // Given
    IOException actualException = new IOException("I/O Error");

    // When
    assertThatExceptionOfType(JKubeException.class)
        .isThrownBy(() -> {
          throw JKubeException.launderThrowable("custom message", actualException);
        })
        .withMessage("custom message")
        .withCause(actualException);
  }
}
