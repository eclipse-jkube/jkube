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
package org.eclipse.jkube.kit.common.archive;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.eclipse.jkube.kit.common.util.FileUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class JKubeTarArchiver {

  private JKubeTarArchiver() {}

  public static File createTarBallOfDirectory(
    File outputFile, File inputDirectory, ArchiveCompression compression) throws IOException {

    return JKubeTarArchiver.createTarBall(outputFile, inputDirectory,
        FileUtil.listFilesAndDirsRecursivelyInDirectory(inputDirectory), Collections.emptyMap(), compression);
  }

  public static File createTarBall(
      File outputFile, File inputDirectory, List<File> fileList, Map<File, String> fileModeMap,
      ArchiveCompression compression
  ) throws IOException {
    return JKubeTarArchiver.createTarBall(
        outputFile, inputDirectory, fileList, fileModeMap, compression, null, null);
  }

  public static File createTarBall(
      File outputFile, File inputDirectory, List<File> fileList, Map<File, String> fileModeMap,
      ArchiveCompression compression,
      Consumer<TarArchiveOutputStream> tarCustomizer, Consumer<TarArchiveEntry> tarArchiveEntryCustomizer
  ) throws IOException {
    try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream)) {

      final TarArchiveOutputStream tarArchiveOutputStream;
      if (compression.equals(ArchiveCompression.gzip)) {
        tarArchiveOutputStream = new TarArchiveOutputStream(new GzipCompressorOutputStream(bufferedOutputStream));
      } else if (compression.equals(ArchiveCompression.bzip2)) {
        tarArchiveOutputStream = new TarArchiveOutputStream(new BZip2CompressorOutputStream(bufferedOutputStream));
      } else {
        tarArchiveOutputStream = new TarArchiveOutputStream(bufferedOutputStream);
      }
      tarArchiveOutputStream.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
      tarArchiveOutputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
      Optional.ofNullable(tarCustomizer).ifPresent(tc -> tc.accept(tarArchiveOutputStream));
      for (File currentFile : fileList) {

        String relativeFilePath = inputDirectory.toURI().relativize(
            new File(currentFile.getAbsolutePath()).toURI()).getPath();

        final TarArchiveEntry tarEntry = new TarArchiveEntry(currentFile, relativeFilePath);
        tarEntry.setSize(currentFile.length());
        if (fileModeMap.containsKey(currentFile)) {
          tarEntry.setMode(Integer.parseInt(fileModeMap.get(currentFile), 8));
        } else if (currentFile.isDirectory()) {
          tarEntry.setMode(TarArchiveEntry.DEFAULT_DIR_MODE);
        }
        if (currentFile.isDirectory()) {
          tarEntry.setSize(0L);
        }
        Optional.ofNullable(tarArchiveEntryCustomizer).ifPresent(tac -> tac.accept(tarEntry));
        tarArchiveOutputStream.putArchiveEntry(tarEntry);
        if (currentFile.isFile()) {
          try (InputStream fis = new FileInputStream(currentFile)) {
            IOUtils.copy(fis, tarArchiveOutputStream);
          }
        }
        tarArchiveOutputStream.closeArchiveEntry();
      }
      tarArchiveOutputStream.close();
    }

    return outputFile;
  }
}
