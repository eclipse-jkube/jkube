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
package org.eclipse.jkube.kit.common.archive;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.eclipse.jkube.kit.common.util.FileUtil;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class ArchiveDecompressor {

  private static final String ERROR_MESSAGE = "Unsupported archive file provided";

  private ArchiveDecompressor() { }

  /**
   * Extracts a given compressed or archive {@link File} to specified target directory.
   *
   * @param inputFile compressed or archive input file.
   * @param targetDirectory target directory to extract the archive to.
   * @throws IOException in case a failure occurs while trying to extract the file.
   */
  public static void extractArchive(File inputFile, File targetDirectory) throws IOException {
    try (InputStream fis = Files.newInputStream(inputFile.toPath())) {
      extractArchive(fis, targetDirectory);
    }
  }

  /**
   * Extracts a given compressed or archive {@link InputStream} to specified target directory.
   *
   * @param archiveInputStream compressed or archive input stream.
   * @param targetDirectory target directory to extract the archive to.
   * @throws IOException in case a failure occurs while trying to extract the stream.
   */
  public static void extractArchive(InputStream archiveInputStream, File targetDirectory) throws IOException {
    try (BufferedInputStream bis = new BufferedInputStream(archiveInputStream)) {
      if (isCompressedFile(bis)) {
        extractCompressedFile(bis, targetDirectory);
      } else if (isArchive(bis)) {
        extractArchiveContents(bis, targetDirectory);
      } else {
        throw new IllegalArgumentException(ERROR_MESSAGE);
      }
    }
  }

  private static void extractCompressedFile(InputStream is, File targetDirectory) throws IOException {
    try (
      CompressorInputStream cis = new CompressorStreamFactory().createCompressorInputStream(is);
      BufferedInputStream bis = new BufferedInputStream(cis)
    ) {
      if (isArchive(bis)) {
        extractArchiveContents(bis, targetDirectory);
      } else {
        throw new IllegalArgumentException(ERROR_MESSAGE);
      }
    } catch (CompressorException ex) {
      throw new IllegalArgumentException(ERROR_MESSAGE, ex);
    }
  }

  private static void extractArchiveContents(InputStream is, File targetDirectory) throws IOException {
    if (targetDirectory.exists() && !targetDirectory.isDirectory()) {
      throw new IllegalArgumentException("Target directory is not a directory");
    } else if (targetDirectory.exists()) {
      FileUtil.cleanDirectory(targetDirectory);
    }
    FileUtil.createDirectory(targetDirectory);
    try (ArchiveInputStream<ArchiveEntry> ais = new ArchiveStreamFactory().createArchiveInputStream(is)) {
      ArchiveEntry entry;
      while ((entry = ais.getNextEntry()) != null) {
        final File extractTo = new File(targetDirectory, fileName(entry.getName()));
        if (extractTo.getCanonicalFile().toPath().startsWith(targetDirectory.getCanonicalFile().toPath())) {
          if (entry.isDirectory()) {
            FileUtil.createDirectory(extractTo);
          } else {
            Files.copy(ais, extractTo.toPath());
          }
        }
      }
    } catch (ArchiveException ex) {
      throw new IllegalArgumentException(ERROR_MESSAGE, ex);
    }
  }

  /**
   * Checks if a given {@link File} is an archive or compressed file.
   *
   * @param file the file to check.
   * @return true if the file is an archive or compressed file, false otherwise.
   */
  public static boolean isArchive(File file) {
    // Fast path: check common archive extensions first to avoid I/O
    if (hasArchiveExtension(file.getName())) {
      return true;
    }

    // Fallback: content-based detection for files without standard extensions
    try (InputStream fis = Files.newInputStream(file.toPath());
         BufferedInputStream bis = new BufferedInputStream(fis)) {
      return isCompressedFile(bis) || isArchive(bis);
    } catch (IOException ex) {
      return false;
    }
  }

  private static boolean hasArchiveExtension(String fileName) {
    String lowerName = fileName.toLowerCase();
    return lowerName.endsWith(".tar") || lowerName.endsWith(".tgz") ||
           lowerName.endsWith(".tar.gz") || lowerName.endsWith(".tar.bz2") ||
           lowerName.endsWith(".tar.xz") || lowerName.endsWith(".zip") ||
           lowerName.endsWith(".jar") || lowerName.endsWith(".war") ||
           lowerName.endsWith(".ear") || lowerName.endsWith(".rar") ||
           lowerName.endsWith(".7z") || lowerName.endsWith(".gz") ||
           lowerName.endsWith(".bz2") || lowerName.endsWith(".xz");
  }

  private static boolean isCompressedFile(InputStream inputStream) {
    try {
      CompressorStreamFactory.detect(inputStream);
      return true;
    } catch(CompressorException ex) {
      return false;
    }
  }

  private static boolean isArchive(InputStream inputStream) {
    try {
      ArchiveStreamFactory.detect(inputStream);
      return true;
    } catch (ArchiveException ex) {
      return false;
    }
  }

  private static String fileName(String originalName) {
    return originalName.replace('/', File.separatorChar);
  }
}
