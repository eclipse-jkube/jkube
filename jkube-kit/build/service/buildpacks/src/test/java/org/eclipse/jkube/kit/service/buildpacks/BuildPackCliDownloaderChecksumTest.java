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
package org.eclipse.jkube.kit.service.buildpacks;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class BuildPackCliDownloaderChecksumTest {

  @TempDir
  private File temporaryFolder;

  // ── parseExpectedChecksum ────────────────────────────────────────────────

  @Test
  @DisplayName("parseExpectedChecksum: bare hex-only line returns the hash")
  void parseExpectedChecksum_bareHash_returnsHash() {
    String result = BuildPackCliDownloader.parseExpectedChecksum(
        "abc123def456abc123def456abc123def456abc123def456abc123def456abc1");
    assertThat(result).isEqualTo("abc123def456abc123def456abc123def456abc123def456abc123def456abc1");
  }

  @Test
  @DisplayName("parseExpectedChecksum: BSD-style '<hash>  <filename>' returns the hash")
  void parseExpectedChecksum_bsdStyleLine_returnsHash() {
    String result = BuildPackCliDownloader.parseExpectedChecksum(
        "deadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef  pack-v0.32.1-linux.tgz");
    assertThat(result).isEqualTo("deadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef");
  }

  @Test
  @DisplayName("parseExpectedChecksum: content with trailing newline is handled")
  void parseExpectedChecksum_trailingNewline_returnsHash() {
    String result = BuildPackCliDownloader.parseExpectedChecksum(
        "cafebabecafebabecafebabecafebabecafebabecafebabecafebabecafebabe  artifact.tgz\n");
    assertThat(result).isEqualTo("cafebabecafebabecafebabecafebabecafebabecafebabecafebabecafebabe");
  }

  @Test
  @DisplayName("parseExpectedChecksum: blank content throws IllegalStateException")
  void parseExpectedChecksum_blankContent_throwsIllegalState() {
    assertThatIllegalStateException()
        .isThrownBy(() -> BuildPackCliDownloader.parseExpectedChecksum("   "))
        .withMessage("Checksum file is empty");
  }

  @Test
  @DisplayName("parseExpectedChecksum: empty string throws IllegalStateException")
  void parseExpectedChecksum_emptyString_throwsIllegalState() {
    assertThatIllegalStateException()
        .isThrownBy(() -> BuildPackCliDownloader.parseExpectedChecksum(""))
        .withMessage("Checksum file is empty");
  }

  // ── calculateSha256 ──────────────────────────────────────────────────────

  @Test
  @DisplayName("calculateSha256: known content produces expected digest")
  void calculateSha256_knownContent_producesExpectedDigest() throws IOException {
    // SHA-256 of the UTF-8 bytes of "hello" is well-known
    File file = new File(temporaryFolder, "hello.txt");
    Files.write(file.toPath(), "hello".getBytes(StandardCharsets.UTF_8));

    String digest = BuildPackCliDownloader.calculateSha256(file);

    assertThat(digest).isEqualTo("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
  }

  @Test
  @DisplayName("calculateSha256: empty file produces well-known SHA-256 of empty input")
  void calculateSha256_emptyFile_producesEmptyFileDigest() throws IOException {
    File file = new File(temporaryFolder, "empty.txt");
    assertThat(file.createNewFile()).isTrue();

    String digest = BuildPackCliDownloader.calculateSha256(file);

    // SHA-256("") == e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
    assertThat(digest).isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
  }

  @Test
  @DisplayName("calculateSha256: returns lowercase hex string of length 64")
  void calculateSha256_returnsLowercaseHex64Chars() throws IOException {
    File file = new File(temporaryFolder, "data.bin");
    Files.write(file.toPath(), new byte[]{1, 2, 3, 4, 5});

    String digest = BuildPackCliDownloader.calculateSha256(file);

    assertThat(digest)
        .hasSize(64)
        .matches("[0-9a-f]+");
  }
}
