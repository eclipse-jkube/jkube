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

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

/**
 * Slightly modified adaptation of <a href="https://github.com/square/okio/blob/okio-parent-1.15.0/okio/src/main/java/okio/Buffer.java">Okio</a>'s Buffer
 * using StringReader.
 * <p>
 * It only implements required methods for {@link HttpHeaders#parseWwwAuthenticateChallengeHeaders(String)}
 */
public class BufferStringReader extends StringReader {
  private static final int READ_AHEAD_LIMIT = Integer.MAX_VALUE;

  public BufferStringReader(String s) {
    super(s);
  }

  /**
   * Check whether stream is empty or not.
   * @return boolean value whether stream is empty or not
   * @throws IOException in case of error while reading stream
   */
  public boolean exhausted() throws IOException {
    mark(READ_AHEAD_LIMIT);
    int charRead = read();
    boolean exhausted = true;
    if (charRead != -1) {
      exhausted = false;
      reset();
    }
    return exhausted;
  }

  /**
   * Consume single character (byte) from stream
   * @return character read in front of stream
   * @throws IOException in case of error while reading the character
   */
  public char readByte() throws IOException {
    return fetchCharAt(0, false);
  }

  /**
   * Get current character at the beginning of the character stream. This does not
   * remove the character from stream. It's only a read only operation.
   *
   * @return character in front of stream
   * @throws IOException in case of error while reading the character
   */
  public char getByte() throws IOException {
    return fetchCharAt(0, true);
  }

  /**
   * Get Nth character from the beginning of the character stream. This does not
   * remove the character from stream. It's only a read only operation.
   *
   * @param index index of character from the beginning of the stream
   * @return character found at Nth position
   * @throws IOException in case of error while reading the character
   */
  public char getByte(long index) throws IOException {
    return fetchCharAt(index, true);
  }

  /**
   * Get nearest index of one of the given set of characters
   *
   * @param sequences list of characters that need to be searched in stream
   * @return nearest index of the provided character, otherwise -1
   * @throws IOException in case of error while reading the characters from stream
   */
  public int indexOfElement(List<Character> sequences) throws IOException {
    mark(READ_AHEAD_LIMIT);
    String remainingString = readUtf8(size());
    int foundIndex = -1;
    int nearestIndex = Integer.MAX_VALUE;
    for (Character ch : sequences) {
      int indexOfCh = remainingString.indexOf(ch);
      if (indexOfCh >= 0) {
        foundIndex = indexOfCh;
        nearestIndex = Math.min(foundIndex, nearestIndex);
      }
    }
    reset();
    return foundIndex != -1 ? nearestIndex : foundIndex;
  }

  /**
   * Get size of stream.
   * @return integer representing number of characters left in stream to be read.
   * @throws IOException in case of error while reading the characters.
   */
  public int size() throws IOException {
    mark(READ_AHEAD_LIMIT);
    int size = 0;
    int charRead = 0;
    while (charRead != -1) {
      charRead = read();
      if (charRead != -1) {
        size++;
      }
    }
    reset();
    return size;
  }

  /**
   * Read a string of given length from stream
   * @param size size of String
   * @return a string extracted from character stream
   * @throws IOException in case of error while reading characters from steram
   */
  public String readUtf8(long size) throws IOException {
    StringBuilder stringBuilder = new StringBuilder();
    mark(READ_AHEAD_LIMIT);
    int readChars = 0;
    int charRead = 0;
    while (charRead != -1 && readChars < size) {
      charRead = read();
      if (charRead != -1) {
        char ch = (char) charRead;
        stringBuilder.append(ch);
        readChars++;
      }
    }
    return stringBuilder.toString();
  }

  private char fetchCharAt(long index, boolean resetMarkAfterRead) throws IOException {
    if (resetMarkAfterRead) {
      mark(READ_AHEAD_LIMIT);
    }
    long count = 0;
    long charRead = 0;
    while (count < index+1 && charRead != -1) {
      charRead = read();
      count++;
    }
    if (resetMarkAfterRead) {
      reset();
    }
    return (char) charRead;
  }
}
