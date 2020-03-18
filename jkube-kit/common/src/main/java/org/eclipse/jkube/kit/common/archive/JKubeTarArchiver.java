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
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JKubeTarArchiver {

  private JKubeTarArchiver() {}

  public static File createTarBallOfDirectory(
    File outputFile, File inputDirectory, ArchiveCompression compression) throws IOException {

    return JKubeTarArchiver.createTarBall(outputFile, inputDirectory, FileUtil.listFilesRecursivelyInDirectory(inputDirectory), Collections.emptyMap(), compression);
  }

  public static File createTarBallOfDirectory(
      File outputFile, File inputDirectory, Map<File, String> fileToPermissionsMap, ArchiveCompression compression)
      throws IOException {

    return JKubeTarArchiver.createTarBall(outputFile, inputDirectory, FileUtil.listFilesRecursivelyInDirectory(inputDirectory),
        fileToPermissionsMap, compression);
  }

  public static File createTarBall(
      File outputFile, File inputDirectory, List<File> fileList, Map<File, String> fileToPermissionsMap,
      ArchiveCompression compression)
      throws IOException {

    try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream)) {

      TarArchiveOutputStream tarArchiveOutputStream = null;
      if (compression.equals(ArchiveCompression.gzip)) {
        tarArchiveOutputStream = new TarArchiveOutputStream(new GzipCompressorOutputStream(bufferedOutputStream));
      } else if (compression.equals(ArchiveCompression.bzip2)) {
        tarArchiveOutputStream = new TarArchiveOutputStream(new BZip2CompressorOutputStream(bufferedOutputStream));
      } else {
        tarArchiveOutputStream = new TarArchiveOutputStream(bufferedOutputStream);
      }

      tarArchiveOutputStream.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
      tarArchiveOutputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
      for (File currentFile : fileList) {

        String relativeFilePath = inputDirectory.toURI().relativize(
            new File(currentFile.getAbsolutePath()).toURI()).getPath();

        TarArchiveEntry tarEntry = new TarArchiveEntry(currentFile, relativeFilePath);
        tarEntry.setSize(currentFile.length());
        if (fileToPermissionsMap.containsKey(currentFile)) {
          tarEntry.setMode(Integer.parseInt(fileToPermissionsMap.get(currentFile), 8));
        }

        tarArchiveOutputStream.putArchiveEntry(tarEntry);
        tarArchiveOutputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
        tarArchiveOutputStream.write(IOUtils.toByteArray(new FileInputStream(currentFile)));
        tarArchiveOutputStream.closeArchiveEntry();
      }
      tarArchiveOutputStream.close();
    }

    return outputFile;
  }
}
