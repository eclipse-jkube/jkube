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
package org.eclipse.jkube.kit.common.assertj;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.AbstractFileAssert;
import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.InputStreamAssert;
import org.assertj.core.api.ListAssert;
import org.assertj.core.api.ObjectAssert;
import org.assertj.core.error.BasicErrorMessageFactory;
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

  private List<TarArchiveEntry> loadEntries() throws IOException {
    return loadEntries(actual);
  }

  public AbstractListAssert<ListAssert<TarArchiveEntry>, List<? extends TarArchiveEntry>, TarArchiveEntry, ObjectAssert<TarArchiveEntry>> entries()
      throws IOException {
    return org.assertj.core.api.Assertions.assertThat(loadEntries());
  }

  public AbstractListAssert<ListAssert<String>, List<? extends String>, String, ObjectAssert<String>> fileTree()
      throws IOException {
    return org.assertj.core.api.Assertions.assertThat(
        loadEntries().stream().map(TarArchiveEntry::getName).collect(Collectors.toList()));
  }

  public ArchiveAssertions hasSameContentAsDirectory(File directory) throws IOException {
    final List<String> actualEntries = new ArrayList<>();
    processArchive(actual, (entry, tis) -> {
      // Remove last separator for directory entries -> Required for fileTree assertion
      actualEntries.add(entry.isDirectory() ?
          entry.getName().substring(0, entry.getName().length() - 1) : entry.getName());
      final File expected = new File(directory, entry.getName());
      if (!expected.exists()) {
        throw failures.failure(info, new BasicErrorMessageFactory("%nExpecting archive <%s>%nnot to contain entry:%n <%s>%n",
          actual.getName(), entry.getName()));
      }
      if (!expected.isDirectory()) {
        try (FileInputStream expectedFis = new FileInputStream(expected)) {
          new InputStreamAssert(IOUtils.toBufferedInputStream(tis)).hasSameContentAs(expectedFis);
        }
      }
    });
    FileAssertions.assertThat(directory).fileTree()
        .hasSize(actualEntries.size())
        .hasSameElementsAs(actualEntries);
    return this;
  }

  private static TarArchiveInputStream inputStream(BufferedInputStream bis) {
    try {
      return new TarArchiveInputStream(new CompressorStreamFactory().createCompressorInputStream(bis));
    } catch (CompressorException ex) {
      return new TarArchiveInputStream(bis);
    }
  }

  private static List<TarArchiveEntry> loadEntries(File file) throws IOException{
    final List<TarArchiveEntry> archiveEntries = new ArrayList<>();
    processArchive(file, (e, is) -> archiveEntries.add(e));
    return archiveEntries;
  }

  private static void processArchive(File file, TarEntryConsumer entryConsumer) throws IOException {
    try (
        FileInputStream fis = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(fis);
        TarArchiveInputStream tis = inputStream(bis)
    ) {
      TarArchiveEntry entry;
      while ((entry = tis.getNextTarEntry()) != null) {
        entryConsumer.accept(entry, tis);
      }
    }
  }

  @FunctionalInterface
  private interface TarEntryConsumer {
    void accept(TarArchiveEntry tarArchiveEntry, TarArchiveInputStream tarArchiveInputStream) throws IOException;
  }
}
