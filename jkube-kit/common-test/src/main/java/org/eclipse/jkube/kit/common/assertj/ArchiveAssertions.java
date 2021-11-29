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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.assertj.core.api.AbstractFileAssert;
import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.ListAssert;
import org.assertj.core.api.ObjectAssert;
import org.assertj.core.error.ShouldBeEmpty;
import org.assertj.core.internal.Failures;
import org.assertj.core.util.Files;

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

  private List<TarArchiveEntry> loadEntries() throws IOException {
    final List<TarArchiveEntry> archiveEntries = new ArrayList<>();
    try (
        FileInputStream fis = new FileInputStream(actual);
        BufferedInputStream bis = new BufferedInputStream(fis);
        TarArchiveInputStream tis = inputStream(bis)
    ) {
      TarArchiveEntry entry;
      while ((entry = tis.getNextTarEntry()) != null) {
        archiveEntries.add(entry);
      }
    }
    return archiveEntries;
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

  public ArchiveAssertions hasSameFolderContentAs(File directory) throws IOException, ArchiveException {
    File temporaryOutputDir = Files.newTemporaryFolder();
    try (final InputStream is = new FileInputStream(actual);
         final TarArchiveInputStream debInputStream = (TarArchiveInputStream) new ArchiveStreamFactory().createArchiveInputStream("tar", is)) {
      TarArchiveEntry entry;
      while ((entry = (TarArchiveEntry)debInputStream.getNextEntry()) != null) {
        final File outputFile = new File(temporaryOutputDir, entry.getName());
        if (entry.isDirectory()) {
          if (!outputFile.exists() && !outputFile.mkdirs()) {
            throw new IllegalStateException(String.format("ArchiveAssertion: Couldn't create temporary directory %s. while extracting archive", outputFile.getAbsolutePath()));
          }
        } else {
          final OutputStream outputFileStream = new FileOutputStream(outputFile);
          IOUtils.copy(debInputStream, outputFileStream);
          outputFileStream.close();
        }
      }
    }
    verifyFolderContainSameContent(temporaryOutputDir, directory);

    return this;
  }

  private void verifyFolderContainSameContent(File folder1, File folder2) {
    Map<String, File> relativePathsInFolder1 = createRelativePathList(folder1);
    Map<String, File> relativePathsInFolder2 = createRelativePathList(folder2);

    for (Map.Entry<String, File> relativePathToFileEntry : relativePathsInFolder1.entrySet()) {
      org.assertj.core.api.Assertions.assertThat(relativePathsInFolder2).containsKey(relativePathToFileEntry.getKey());
      File folder2File = relativePathsInFolder2.get(relativePathToFileEntry.getKey());
      if (folder2File.isFile()) {
        org.assertj.core.api.Assertions.assertThat(folder2File).hasSameBinaryContentAs(relativePathToFileEntry.getValue());
      }
    }
  }

  private Map<String, File> createRelativePathList(File folder) {
    Collection<File> files = FileUtils.listFiles(
        folder,
        new RegexFileFilter("^(.*?)"),
        DirectoryFileFilter.DIRECTORY
    );
    Map<String, File> relativePathsToFileMap = new HashMap<>();

    for (File file : files) {
      String relativePath = new File(folder.getAbsolutePath()).toURI().relativize(new File(file.getAbsolutePath()).toURI()).getPath();
      relativePathsToFileMap.put(relativePath, file);
    }

    return relativePathsToFileMap;
  }
}
