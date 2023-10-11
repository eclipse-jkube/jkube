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
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * These tests are ported from OkHttp's <a href="https://github.com/square/okhttp/blob/parent-3.14.9/okhttp-tests/src/test/java/okhttp3/HeadersTest.java#L439">HeaderTests</a>
 * All the tests related to WWW-Authenticate parsing.
 */
class HttpHeadersTest {
  @Test
  void testDigestChallengeWithStrictRfc2617Header() throws IOException {
    // Given
    String responseHeader = "Digest realm=\"myrealm\", nonce=\"fjalskdflwejrlaskdfjlaskdjflaks"
        + "jdflkasdf\", qop=\"auth\", stale=\"FALSE\"";
    // When
    List<Map<String, String>> challenges = HttpHeaders.parseWwwAuthenticateChallengeHeaders(responseHeader);
    // Then
    assertThat(challenges)
        .hasSize(1)
        .singleElement(InstanceOfAssertFactories.MAP)
        .containsEntry("scheme", "Digest")
        .containsEntry("realm", "myrealm")
        .containsEntry("nonce", "fjalskdflwejrlaskdfjlaskdjflaksjdflkasdf")
        .containsEntry("qop", "auth")
        .containsEntry("stale", "FALSE");
  }

  @Test
  void testDigestChallengeWithDifferentlyOrderedAuthParams() throws IOException {
    // Given
    String responseHeader = "Digest qop=\"auth\", realm=\"myrealm\", nonce=\"fjalskdflwejrlask"
        + "dfjlaskdjflaksjdflkasdf\", stale=\"FALSE\"";
    // When
    List<Map<String, String>> challenges = HttpHeaders.parseWwwAuthenticateChallengeHeaders(responseHeader);
    // Then
    assertThat(challenges)
        .hasSize(1)
        .singleElement(InstanceOfAssertFactories.MAP)
        .containsEntry("scheme", "Digest")
        .containsEntry("realm", "myrealm")
        .containsEntry("nonce", "fjalskdflwejrlaskdfjlaskdjflaksjdflkasdf")
        .containsEntry("qop", "auth")
        .containsEntry("stale", "FALSE");
  }

  @Test
  void testDigestChallengeWithDifferentlyOrderedAuthParams2() throws IOException {
    // Given
    String responseHeader = "Digest qop=\"auth\", nonce=\"fjalskdflwejrlaskdfjlaskdjflaksjdflk"
        + "asdf\", realm=\"myrealm\", stale=\"FALSE\"";
    // When
    List<Map<String, String>> challenges = HttpHeaders.parseWwwAuthenticateChallengeHeaders(responseHeader);
    // Then
    assertThat(challenges)
        .hasSize(1)
        .singleElement(InstanceOfAssertFactories.MAP)
        .containsEntry("scheme", "Digest")
        .containsEntry("realm", "myrealm")
        .containsEntry("nonce", "fjalskdflwejrlaskdfjlaskdjflaksjdflkasdf")
        .containsEntry("qop", "auth")
        .containsEntry("stale", "FALSE");
  }

  @Test
  void testDigestChallengeWithMissingRealm() throws IOException {
    String responseHeader = "Digest qop=\"auth\", underrealm=\"myrealm\", nonce=\"fjalskdflwej"
        + "rlaskdfjlaskdjflaksjdflkasdf\", stale=\"FALSE\"";
    List<Map<String, String>> challenges = HttpHeaders.parseWwwAuthenticateChallengeHeaders(responseHeader);
    assertThat(challenges)
        .hasSize(1)
        .singleElement(InstanceOfAssertFactories.MAP)
        .containsEntry("scheme", "Digest")
        .containsEntry("underrealm", "myrealm")
        .containsEntry("nonce", "fjalskdflwejrlaskdfjlaskdjflaksjdflkasdf")
        .containsEntry("qop", "auth")
        .containsEntry("stale", "FALSE");
  }

  @Test
  void testDigestChallengeWithAdditionalSpaces() throws IOException {
    // Given
    String responseHeader = "Digest qop=\"auth\",    realm=\"myrealm\", nonce=\"fjalskdflwejrl"
        + "askdfjlaskdjflaksjdflkasdf\", stale=\"FALSE\"";
    // When
    List<Map<String, String>> challenges = HttpHeaders.parseWwwAuthenticateChallengeHeaders(responseHeader);
    // Then
    assertThat(challenges)
        .hasSize(1)
        .singleElement(InstanceOfAssertFactories.MAP)
        .containsEntry("scheme", "Digest")
        .containsEntry("realm", "myrealm")
        .containsEntry("nonce", "fjalskdflwejrlaskdfjlaskdjflaksjdflkasdf")
        .containsEntry("qop", "auth")
        .containsEntry("stale", "FALSE");
  }

  @Test
  void testDigestChallengeWithAdditionalSpacesBeforeFirstAuthParam() throws IOException {
    // Given
    String responseHeader = "Digest    realm=\"myrealm\", nonce=\"fjalskdflwejrlaskdfjlaskdjfl"
        + "aksjdflkasdf\", qop=\"auth\", stale=\"FALSE\"";
    // When
    List<Map<String, String>> challenges = HttpHeaders.parseWwwAuthenticateChallengeHeaders(responseHeader);
    // Then
    assertThat(challenges)
        .hasSize(1)
        .singleElement(InstanceOfAssertFactories.MAP)
        .containsEntry("scheme", "Digest")
        .containsEntry("realm", "myrealm")
        .containsEntry("nonce", "fjalskdflwejrlaskdfjlaskdjflaksjdflkasdf")
        .containsEntry("qop", "auth")
        .containsEntry("stale", "FALSE");
  }

  @Test
  void testDigestChallengeWithCamelCasedNames() throws IOException {
    // Given
    String responseHeader = "DiGeSt qop=\"auth\", rEaLm=\"myrealm\", nonce=\"fjalskdflwejrlask"
        + "dfjlaskdjflaksjdflkasdf\", stale=\"FALSE\"";
    // When
    List<Map<String, String>> challenges = HttpHeaders.parseWwwAuthenticateChallengeHeaders(responseHeader);
    // Then
    assertThat(challenges)
        .hasSize(1)
        .singleElement(InstanceOfAssertFactories.MAP)
        .containsEntry("scheme", "DiGeSt")
        .containsEntry("rEaLm", "myrealm")
        .containsEntry("nonce", "fjalskdflwejrlaskdfjlaskdjflaksjdflkasdf")
        .containsEntry("qop", "auth")
        .containsEntry("stale", "FALSE");
  }

  @Test
  void testDigestChallengeWithCamelCasedNames2() throws IOException {
    // Given
    String responseHeader = "DIgEsT rEaLm=\"myrealm\", nonce=\"fjalskdflwejrlaskdfjlaskdjflaks"
        + "jdflkasdf\", qop=\"auth\", stale=\"FALSE\"";
    // When
    List<Map<String, String>> challenges = HttpHeaders.parseWwwAuthenticateChallengeHeaders(responseHeader);
    // Then
    assertThat(challenges)
        .hasSize(1)
        .singleElement(InstanceOfAssertFactories.MAP)
        .containsEntry("scheme", "DIgEsT")
        .containsEntry("rEaLm", "myrealm")
        .containsEntry("nonce", "fjalskdflwejrlaskdfjlaskdjflaksjdflkasdf")
        .containsEntry("qop", "auth")
        .containsEntry("stale", "FALSE");
  }

  @Test
  void testDigestChallengeWithTokenFormOfAuthParam() throws IOException {
    // Given
    String responseHeader = "Digest realm=myrealm";
    // When
    List<Map<String, String>> challenges = HttpHeaders.parseWwwAuthenticateChallengeHeaders(responseHeader);
    // Then
    assertThat(challenges)
        .hasSize(1)
        .satisfies(c -> assertChallenge(c.get(0), "Digest", "myrealm"));
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "Digest",
      "Digest,,,, Basic ,,,realm=\"my\\\\\\\\\"r\\ealm\"",
      "Digest,,,, Basic ,,,realm=\"my\"realm\""
  })
  void parseWwwAuthenticateChallengeHeaders_whenOnlyFirstChallengeParsable_thenReturnSingleChallenge(String responseHeader) throws IOException {
    // When
    List<Map<String, String>> challenges = HttpHeaders.parseWwwAuthenticateChallengeHeaders(responseHeader);
    // Then
    assertThat(challenges)
        .hasSize(1)
        .satisfies(c -> assertChallenge(c.get(0), "Digest", null));
  }

  @Test
  void basicChallenge() throws IOException {
    // Given
    String responseHeader = "Basic realm=\"protected area\"";
    // When
    List<Map<String, String>> challenges = HttpHeaders.parseWwwAuthenticateChallengeHeaders(responseHeader);
    // Then
    assertThat(challenges)
        .hasSize(1)
        .satisfies(c -> assertChallenge(c.get(0), "Basic", "protected area"));
  }

  @Test
  void basicChallengeWithCharset() throws IOException {
    // Given
    String responseHeader = "Basic realm=\"protected area\", charset=\"UTF-8\"";
    // When
    List<Map<String, String>> challenges = HttpHeaders.parseWwwAuthenticateChallengeHeaders(responseHeader);
    // Then
    assertThat(challenges)
        .hasSize(1)
        .satisfies(c -> assertChallenge(c.get(0), "Basic", "protected area"))
        .singleElement(InstanceOfAssertFactories.MAP)
        .containsEntry("charset", "UTF-8");
  }

  @Test
  void basicChallengeWithUnexpectedCharset() throws IOException {
    // Given
    String responseHeader = "Basic realm=\"protected area\", charset=\"US-ASCII\"";
    // When
    List<Map<String, String>> challenges = HttpHeaders.parseWwwAuthenticateChallengeHeaders(responseHeader);
    // Then
    assertThat(challenges)
        .hasSize(1)
        .satisfies(c -> assertChallenge(c.get(0), "Basic", "protected area"))
        .singleElement(InstanceOfAssertFactories.MAP)
        .containsEntry("charset", "US-ASCII");
  }

  @Test
  void separatorsBeforeFirstChallenge() throws IOException {
    // Given
    String responseHeader = " ,  , Basic realm=myrealm";
    // When
    List<Map<String, String>> challenges = HttpHeaders.parseWwwAuthenticateChallengeHeaders(responseHeader);
    // Then
    assertThat(challenges)
        .hasSize(1)
        .satisfies(c -> assertChallenge(c.get(0), "Basic", "myrealm"));
  }

  @Test
  void spacesAroundKeyValueSeparator() throws IOException {
    // Given
    String responseHeader = "Basic realm = \"myrealm\"";
    // When
    List<Map<String, String>> challenges = HttpHeaders.parseWwwAuthenticateChallengeHeaders(responseHeader);
    // Then
    assertThat(challenges)
        .hasSize(1)
        .satisfies(c -> assertChallenge(c.get(0), "Basic", "myrealm"));
  }

  @Test
  void multipleChallengesInOneHeader() throws IOException {
    // Given
    String responseHeader = "Basic realm = \"myrealm\",Digest";
    // When
    List<Map<String, String>> challenges = HttpHeaders.parseWwwAuthenticateChallengeHeaders(responseHeader);
    // Then
    assertThat(challenges)
        .hasSize(2)
        .satisfies(c -> assertChallenge(c.get(0), "Basic", "myrealm"))
        .satisfies(c -> assertChallenge(c.get(1), "Digest", null));
  }

  @Test
  void multipleChallengesWithSameSchemeButDifferentRealmInOneHeader() throws IOException {
    // Given
    String responseHeader = "Basic realm = \"myrealm\",Basic realm=myotherrealm";
    // When
    List<Map<String, String>> challenges = HttpHeaders.parseWwwAuthenticateChallengeHeaders(responseHeader);
    // Then
    assertThat(challenges)
        .hasSize(2)
        .satisfies(c -> assertChallenge(c.get(0), "Basic", "myrealm"))
        .satisfies(c -> assertChallenge(c.get(1), "Basic", "myotherrealm"));
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "Digest, Basic ,,realm=\"myrealm\"",
      "Digest,Basic realm=\"myrealm\"",
      "Digest,,,, Basic ,,realm=\"myrealm\"",
  })
  void parseWwwAuthenticateChallengeHeaders_withDigestAndAuthParam_shouldParseBoth(String responseHeader) throws IOException {
    // When
    List<Map<String, String>> challenges = HttpHeaders.parseWwwAuthenticateChallengeHeaders(responseHeader);
    // Then
    assertThat(challenges).hasSize(2);
    assertThat(challenges.get(0))
        .containsEntry("scheme", "Digest");
    assertThat(challenges.get(1))
        .containsEntry("scheme", "Basic")
        .containsEntry("realm", "myrealm");
  }

  @Test
  void unknownAuthParams() throws IOException {
    // Given
    String responseHeader = "Digest,,,, Basic ,,foo=bar,realm=\"myrealm\"";
    // When
    List<Map<String, String>> challenges = HttpHeaders.parseWwwAuthenticateChallengeHeaders(responseHeader);
    // Then
    assertThat(challenges)
        .hasSize(2)
        .satisfies(c -> assertChallenge(c.get(0), "Digest", null))
        .satisfies(c -> assertChallenge(c.get(1), "Basic", "myrealm"))
        .element(1)
        .asInstanceOf(InstanceOfAssertFactories.MAP)
        .containsEntry("foo", "bar");
  }

  @Test
  void escapedCharactersInQuotedString() throws IOException {
    // Given
    String responseHeader = "Digest,,,, Basic ,,,realm=\"my\\\\\\\"r\\ealm\"";
    // When
    List<Map<String, String>> challenges = HttpHeaders.parseWwwAuthenticateChallengeHeaders(responseHeader);
    // Then
    assertThat(challenges)
        .hasSize(2)
        .satisfies(c -> assertChallenge(c.get(0), "Digest", null))
        .satisfies(c -> assertChallenge(c.get(1), "Basic", "my\\\"realm"));
  }

  @Test
  void commaInQuotedStringAndBeforeFirstChallenge() throws IOException {
    // Given
    String responseHeader = ",Digest,,,, Basic ,,,realm=\"my, realm,\"";
    // When
    List<Map<String, String>> challenges = HttpHeaders.parseWwwAuthenticateChallengeHeaders(responseHeader);
    // Then
    assertThat(challenges)
        .hasSize(2)
        .satisfies(c -> assertChallenge(c.get(0), "Digest", null))
        .satisfies(c -> assertChallenge(c.get(1), "Basic", "my, realm,"));
  }

  @Test
  void unescapedDoubleQuoteInQuotedStringWithEvenNumberOfBackslashesInFront() throws IOException {
    // Given
    String responseHeader = "Digest,,,, Basic ,,,realm=\"my\\\\\\\\\"r\\ealm\"";
    // When
    List<Map<String, String>> challenges = HttpHeaders.parseWwwAuthenticateChallengeHeaders(responseHeader);
    // Then
    assertThat(challenges)
        .hasSize(1)
        .satisfies(c -> assertChallenge(c.get(0), "Digest", null));
  }

  @Test
  void unescapedDoubleQuoteInQuotedString() throws IOException {
    // Given
    String responseHeader = "Digest,,,, Basic ,,,realm=\"my\"realm\"";
    // When
    List<Map<String, String>> challenges = HttpHeaders.parseWwwAuthenticateChallengeHeaders(responseHeader);
    // Then
    assertThat(challenges)
        .hasSize(1)
        .satisfies(c -> assertChallenge(c.get(0), "Digest", null));
  }

  @Test
  void token68InsteadOfAuthParams() throws IOException {
    // Given
    String responseHeader = "Other abc==";
    // When
    List<Map<String, String>> challenges = HttpHeaders.parseWwwAuthenticateChallengeHeaders(responseHeader);
    // Then
    assertThat(challenges)
        .hasSize(1)
        .singleElement(InstanceOfAssertFactories.MAP)
        .containsEntry("scheme", "Other")
        .containsEntry(null, "abc==");
  }

  @Test
  void token68AndAuthParams() throws IOException {
    // Given
    String responseHeader = "Other abc==, realm=myrealm";
    // When
    List<Map<String, String>> challenges = HttpHeaders.parseWwwAuthenticateChallengeHeaders(responseHeader);
    // Then
    assertThat(challenges)
        .hasSize(1)
        .singleElement(InstanceOfAssertFactories.MAP)
        .containsEntry("scheme", "Other")
        .containsEntry(null, "abc==");
  }

  @Test
  void repeatedAuthParamKey() throws IOException {
    // Given
    String responseHeader = "Other realm=myotherrealm, realm=myrealm";
    // When
    List<Map<String, String>> challenges = HttpHeaders.parseWwwAuthenticateChallengeHeaders(responseHeader);
    // Then
    assertThat(challenges).isEmpty();
  }

  @Test
  void multipleAuthenticateHeaders() throws IOException {
    // Given
    String responseHeader = "Digest, Basic realm=myrealm";
    // When
    List<Map<String, String>> challenges = HttpHeaders.parseWwwAuthenticateChallengeHeaders(responseHeader);
    // Then
    assertThat(challenges)
        .hasSize(2)
        .satisfies(c -> assertChallenge(c.get(0), "Digest", null))
        .satisfies(c -> assertChallenge(c.get(1), "Basic", "myrealm"));
  }

  @Test
  void multipleAuthenticateHeadersInDifferentOrder() throws IOException {
    // Given
    String responseHeader = "Basic realm=myrealm, Digest";
    // When
    List<Map<String, String>> challenges = HttpHeaders.parseWwwAuthenticateChallengeHeaders(responseHeader);
    // Then
    assertThat(challenges)
        .hasSize(2)
        .satisfies(c -> assertChallenge(c.get(0), "Basic", "myrealm"))
        .satisfies(c -> assertChallenge(c.get(1), "Digest", null));
  }

  @Test
  void multipleBasicAuthenticateHeaders() throws IOException {
    // Given
    String responseHeader = "Basic realm=myrealm, Basic realm=myotherrealm";
    // When
    List<Map<String, String>> challenges = HttpHeaders.parseWwwAuthenticateChallengeHeaders(responseHeader);
    // Then
    assertThat(challenges)
        .hasSize(2)
        .satisfies(c -> assertChallenge(c.get(0), "Basic", "myrealm"))
        .satisfies(c -> assertChallenge(c.get(1), "Basic", "myotherrealm"));
  }

  private void assertChallenge(Map<String, String> challengeHeader, String scheme, String realm) {
    assertThat(challengeHeader).containsEntry("scheme", scheme);
    if (StringUtils.isNotBlank(realm)) {
      assertThat(challengeHeader).containsEntry("realm", realm);
    }
  }
}
