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
package org.eclipse.jkube.kit.common.util.okhttp;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class BufferStringReaderTest {
  @Test
  void exhausted_whenStreamNotExhausted_thenReturnFalse() throws IOException {
    // Given
    BufferStringReader buffer = new BufferStringReader("foo");
    // When
    buffer.readByte();
    boolean result = buffer.exhausted();
    // Then
    assertThat(result).isFalse();
  }

  @Test
  void exhausted_whenStreamEmpty_thenReturnTrue() throws IOException {
    // Given
    BufferStringReader buffer = new BufferStringReader("f");
    // When
    buffer.readByte();
    boolean result = buffer.exhausted();
    // Then
    assertThat(result).isTrue();
  }

  @Test
  void readByte_whenInvoked_shouldConsumeOneCharacterFromStream() throws IOException {
    // Given
    BufferStringReader buffer = new BufferStringReader("foo");
    // When + Then
    assertThat(buffer.readByte()).isEqualTo('f');
    assertThat(buffer.readByte()).isEqualTo('o');
    assertThat(buffer.readByte()).isEqualTo('o');
    assertThat(buffer.exhausted()).isTrue();
  }

  @Test
  void getByte_whenInvoked_shouldReturnFirstCharacterFromStream() throws IOException {
    // Given
    BufferStringReader buffer = new BufferStringReader("foo");
    // When
    char ch = buffer.getByte();
    // Then
    assertThat(ch).isEqualTo('f');
    assertThat(buffer.size()).isEqualTo(3);
  }

  @Test
  void getByte_whenIndexProvided_shouldReturnCharacterAtSpecifiedIndex() throws IOException {
    // Given
    BufferStringReader buffer = new BufferStringReader("foo");
    // When + Then
    assertThat(buffer.getByte(0)).isEqualTo('f');
    assertThat(buffer.getByte(1)).isEqualTo('o');
    assertThat(buffer.getByte(2)).isEqualTo('o');
    assertThat(buffer.size()).isEqualTo(3);
  }

  @Test
  void indexOfElement_whenCharSequencePresent_thenReturnNearestIndex() throws IOException {
    // Given
    BufferStringReader buffer = new BufferStringReader("my\\\"realm");
    // When
    int index = buffer.indexOfElement(Arrays.asList('\\', '\"'));
    // Then
    assertThat(index).isEqualTo(2);
  }

  @Test
  void indexOfElement_whenCharSequenceAbsent_thenReturnNegativeIndex() throws IOException {
    // Given
    BufferStringReader buffer = new BufferStringReader("my\"realm");
    // When
    int index = buffer.indexOfElement(Collections.singletonList(','));
    // Then
    assertThat(index).isEqualTo(-1);
  }

  @Test
  void readUtf8_whenInvoked_shouldReadCharactersIntoString() throws IOException {
    // Given
    BufferStringReader buffer = new BufferStringReader("Bearer realm=\"myrealm\"");
    // When + Then
    assertThat(buffer.readUtf8(6)).isEqualTo("Bearer");
    assertThat(buffer.readUtf8(16)).isEqualTo(" realm=\"myrealm\"");
  }
}
