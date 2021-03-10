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
package org.eclipse.jkube.kit.common.assertj;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.assertj.core.api.AbstractFileAssert;
import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.ListAssert;
import org.assertj.core.api.ObjectAssert;
import org.assertj.core.error.ShouldBeEmpty;
import org.assertj.core.internal.Failures;

import static org.assertj.core.error.ShouldBeEqualIgnoringCase.shouldBeEqual;

public class ArchiveAssertions extends AbstractFileAssert<ArchiveAssertions> {

  private final Failures failures = Failures.instance();

  private ArchiveAssertions(File actual) {
    super(actual, ArchiveAssertions.class);
  }

  public static ArchiveAssertions assertThat(File archive) {
    return new ArchiveAssertions(archive);
  }

  private String detect() throws IOException, CompressorException {
    try (
        FileInputStream fis = new FileInputStream(actual);
        BufferedInputStream bis = new BufferedInputStream(fis)
    ) {
      return CompressorStreamFactory.detect(bis);
    }
  }

  private void assertCompression(String expected) throws IOException, CompressorException {
    final String detected = detect();
    if (expected.equals(detected)) {
      return;
    }
    throw failures.failure(info, shouldBeEqual(detected, expected));
  }

  public ArchiveAssertions isUncompressed() throws IOException, CompressorException {
    try {
      final String detected = detect();
      throw failures.failure(info, ShouldBeEmpty.shouldBeEmpty(detected));
    } catch (CompressorException ex) {
      if (ex.getMessage().equals("No Compressor found for the stream signature.")) {
        return this;
      }
      throw ex;
    }
  }

  public ArchiveAssertions isGZip() throws IOException, CompressorException {
    assertCompression("gz");
    return this;
  }

  public ArchiveAssertions isBzip2() throws IOException, CompressorException {
    assertCompression("bzip2");
    return this;
  }

  private TarArchiveInputStream inputStream(BufferedInputStream bis) {
    try {
      return new TarArchiveInputStream(new CompressorStreamFactory().createCompressorInputStream(bis));
    } catch (CompressorException ex) {
      return new TarArchiveInputStream(bis);
    }
  }

  public AbstractListAssert<ListAssert<String>, List<? extends String>, String, ObjectAssert<String>> fileTree()
      throws IOException {
    final List<String> archiveFileTree = new ArrayList<>();

    try (
        FileInputStream fis = new FileInputStream(actual);
        BufferedInputStream bis = new BufferedInputStream(fis);
        TarArchiveInputStream tis = inputStream(bis)
    ) {
      TarArchiveEntry entry;
      while ((entry = tis.getNextTarEntry()) != null) {
        archiveFileTree.add(entry.getName());
      }
    }
    return org.assertj.core.api.Assertions.assertThat(archiveFileTree);
  }
}
