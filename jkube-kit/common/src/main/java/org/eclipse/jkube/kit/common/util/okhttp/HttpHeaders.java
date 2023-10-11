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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Originally ported from OkHttp's <a href="https://github.com/square/okhttp/blob/parent-3.14.9/okhttp/src/main/java/okhttp3/internal/http/HttpHeaders.java#L180">HttpHeaders</a>
 * <p>
 * We have runtime dependency on OkHttp which is a transitive dependency coming from Fabric8 Kubernetes Client. We needed
 * only parseChallengeHeader method for parsing WWW-Authenticate header for fetching authentication challenges. It seemed
 * a better option to port the required method instead of adding okhttp dependency to the project.
 */
public class HttpHeaders {
  private static final int MARK_LIMIT = Integer.MAX_VALUE;
  private static final List<Character> QUOTED_STRING_DELIMITERS = Arrays.asList('\\', '\"');
  private static final List<Character> TOKEN_DELIMITERS = Arrays.asList('\t', ' ', ',', '=');

  private HttpHeaders() { }

  /**
   * Parse WWW-Authenticate header contents as a list of maps, with each map representing
   * an authentication challenge.
   *
   * @param header string containing header value
   * @return list of maps containing values of each authentication challenge
   * @throws IOException in case of parsing string
   */
  public static List<Map<String, String>> parseWwwAuthenticateChallengeHeaders(String header) throws IOException {
    String peek = null;
    List<Map<String, String>> challenges = new ArrayList<>();

    BufferStringReader stringReader = new BufferStringReader(header);
    while (true) {
      // Read a scheme name for this challenge if we don't have one already.
      if (peek == null) {
        skipWhitespaceAndCommas(stringReader);
        peek = readToken(stringReader);
        if (peek == null) return challenges;
      }

      String schemeName = peek;

      // Read a token68, a sequence of parameters, or nothing.
      boolean commaPrefixed = skipWhitespaceAndCommas(stringReader);
      peek = readToken(stringReader);
      if (peek == null) {
        if (!stringReader.exhausted()) {
          return challenges; // Expected a token; got something else.
        }
        challenges.add(createNewChallenge(schemeName, Collections.emptyMap()));
        return challenges;
      }

      int eqCount = skipAll(stringReader, '=');
      boolean commaSuffixed = skipWhitespaceAndCommas(stringReader);

      // It's a token68 because there isn't a value after it.
      if (!commaPrefixed && (commaSuffixed || stringReader.exhausted())) {
        challenges.add(createNewChallenge(schemeName, Collections.singletonMap(null, peek + repeat('=', eqCount))));
        peek = null;
        continue;
      }

      // It's a series of parameter names and values.
      Map<String, String> parameters = new LinkedHashMap<>();
      eqCount += skipAll(stringReader, '=');
      while (true) {
        if (peek == null) {
          peek = readToken(stringReader);
          if (skipWhitespaceAndCommas(stringReader)) break; // We peeked a scheme name followed by ','.
          eqCount = skipAll(stringReader, '=');
        }
        if (eqCount == 0) break; // We peeked a scheme name.
        if (eqCount > 1) return challenges; // Unexpected '=' characters.
        if (skipWhitespaceAndCommas(stringReader)) return challenges; // Unexpected ','.

        String parameterValue = !stringReader.exhausted() && stringReader.getByte(0) == '"'
            ? readQuotedString(stringReader)
            : readToken(stringReader);
        if (parameterValue == null) return challenges; // Expected a value.
        String replaced = parameters.put(peek, parameterValue);
        peek = null;
        if (replaced != null) return challenges; // Unexpected duplicate parameter.
        if (!skipWhitespaceAndCommas(stringReader) && !stringReader.exhausted()) return challenges; // Expected ',' or EOF.
      }
      challenges.add(createNewChallenge(schemeName, parameters));
    }
  }

  private static Map<String, String> createNewChallenge(String scheme, Map<String, String> authParams) {
    Map<String, String> challengeAsMap = new HashMap<>();
    challengeAsMap.put("scheme", scheme);
    challengeAsMap.putAll(authParams);
    return challengeAsMap;
  }

  private static boolean skipWhitespaceAndCommas(StringReader buffer) throws IOException {
    boolean commaFound = false;
    int charRead = 0;
    while (charRead != -1) {
      buffer.mark(MARK_LIMIT);
      charRead = buffer.read();
      char b = (char) charRead;
      if (b == ',' || b == ' ' || b == '\t') {
        if (b == ',') {
          commaFound = true;
        }
      } else if (charRead != -1){
        buffer.reset();
        break;
      }
    }
    return commaFound;
  }

  private static int skipAll(BufferStringReader buffer, char b) throws IOException {
    int count = 0;
    int charRead;
    char curChar;
    do {
      buffer.mark(MARK_LIMIT);
      charRead = buffer.read();
      curChar = (char) charRead;
      if (charRead != -1 && curChar == b) {
        count++;
      } else if (curChar != b) {
        buffer.reset();
      }
    } while (charRead != -1 && curChar == b);
    return count;
  }

  /**
   * Reads a double-quoted string, unescaping quoted pairs like {@code \"} to the 2nd character in
   * each sequence. Returns the unescaped string, or null if the buffer isn't prefixed with a
   * double-quoted string.
   */
  private static String readQuotedString(BufferStringReader buffer) throws IOException {
    if (buffer.readByte() != '\"') throw new IllegalArgumentException();
    StringBuilder result = new StringBuilder();
    while (true) {
      long i = buffer.indexOfElement(QUOTED_STRING_DELIMITERS);
      if (i == -1L) return null; // Unterminated quoted string.

      if (buffer.getByte(i) == '"') {
        String quoteString = buffer.readUtf8(i);
        result.append(quoteString);
        buffer.readByte(); // Consume '"'.
        return result.toString();
      }

      if (buffer.size() == i + 1L) return null; // Dangling escape.
      result.append(buffer.readUtf8(i));
      buffer.readByte(); // Consume '\'.
      result.append(buffer.readUtf8(1)); // The escaped character.
    }
  }

  /**
   * Consumes and returns a non-empty token, terminating at special characters in. Returns null if the buffer is empty or prefixed with a delimiter.
   */
  private static String readToken(BufferStringReader buffer) {
    try {
      long tokenSize = buffer.indexOfElement(TOKEN_DELIMITERS);
      if (tokenSize == -1L) tokenSize = buffer.size();

      return tokenSize != 0L
          ? buffer.readUtf8(tokenSize)
          : null;
    } catch (IOException e) {
      throw new AssertionError();
    }
  }

  private static String repeat(char c, int count) {
    char[] array = new char[count];
    Arrays.fill(array, c);
    return new String(array);
  }
}
