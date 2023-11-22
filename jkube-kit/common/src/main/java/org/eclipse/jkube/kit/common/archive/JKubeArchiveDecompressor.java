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
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.eclipse.jkube.kit.common.util.FileUtil;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class JKubeArchiveDecompressor {
  private JKubeArchiveDecompressor() { }

  /**
   * Extracts a given archive file to specified target directory
   *
   * @param inputArchiveFile input archive file
   * @param targetDirectory target folder where you want to extract
   * @throws IOException in case of failure while trying to create any directory
   */
  public static void extractArchive(File inputArchiveFile, File targetDirectory) throws IOException {
    if (targetDirectory.exists()) {
      FileUtil.cleanDirectory(targetDirectory);
    }
    Files.createDirectory(targetDirectory.toPath());

    if (isArchiveCompressedWithGZipAlgorithm(inputArchiveFile)) {
      extractTarArchive(inputArchiveFile, targetDirectory.toPath());
    } else if (isArchiveZip(inputArchiveFile)) {
      extractZipArchive(inputArchiveFile, targetDirectory.toPath());
    } else {
      throw new IllegalStateException("Unsupported archive file provided");
    }
  }

  private static boolean isArchiveCompressedWithGZipAlgorithm(File inputArchiveFile) throws IOException {
    BufferedInputStream bufferedInputStream = new BufferedInputStream(Files.newInputStream(inputArchiveFile.toPath()));
    try {
      String s = CompressorStreamFactory.detect(bufferedInputStream);
      return s.equals(CompressorStreamFactory.GZIP);
    } catch (CompressorException ignored) {
      // Unknown Compressor stream signature found, ignore
    }
    return false;
  }

  private static boolean isArchiveZip(File inputArchiveFile) throws IOException {
    BufferedInputStream bufferedInputStream = new BufferedInputStream(Files.newInputStream(inputArchiveFile.toPath()));
    try {
      String s = ArchiveStreamFactory.detect(bufferedInputStream);
      return s.equals(ArchiveStreamFactory.ZIP);
    } catch (ArchiveException ignored) {
      // Unknown Archive stream signature found, ignore
    }
    return false;
  }

  private static void extractTarArchive(File downloadedArchive, Path targetExtractionDir) throws IOException {
    try (BufferedInputStream inputStream = new BufferedInputStream(Files.newInputStream(downloadedArchive.toPath()));
         TarArchiveInputStream tar = new TarArchiveInputStream(new GzipCompressorInputStream(inputStream))) {
      ArchiveEntry entry;
      while ((entry = tar.getNextEntry()) != null) {
        Path extractTo = targetExtractionDir.resolve(entry.getName());
        if (entry.isDirectory()) {
          Files.createDirectories(extractTo);
        } else {
          Files.copy(tar, extractTo);
        }
      }
    }
  }

  private static void extractZipArchive(File downloadedArchive, Path targetExtractionDir) throws IOException {
    byte[] buffer = new byte[1024];
    try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(downloadedArchive.toPath()))) {
      ZipEntry zipEntry = zis.getNextEntry();
      while (zipEntry != null) {
        File newFile = new File(targetExtractionDir.toFile(), zipEntry.getName());
        if (!newFile.getCanonicalPath().startsWith(targetExtractionDir.toFile().getCanonicalPath())) {
          throw new IOException("Entry is outside of target dir: " + targetExtractionDir);
        }
        if (zipEntry.isDirectory()) {
          if (!newFile.isDirectory() && !newFile.mkdirs()) {
            throw new IOException("Failed to create directory " + newFile);
          }
        } else {
          try (FileOutputStream fos = new FileOutputStream(newFile)) {
            int len;
            while ((len = zis.read(buffer)) > 0) {
              fos.write(buffer, 0, len);
            }
          }
        }
        zipEntry = zis.getNextEntry();
      }
      zis.closeEntry();
    }
  }
}
