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
package org.eclipse.jkube.kit.build.core.assembly;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.eclipse.jkube.kit.common.util.FileUtil;
import org.eclipse.jkube.kit.config.image.build.ArchiveCompression;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JkubeTarArchiver {
    private Map<File, String> filesToIncludeNameMap = new HashMap<>();
    private Map<File, String> fileToPermissionsMap = new HashMap<>();
    private List<String> filesNamesToExclude = new ArrayList<>();

    public void includeFile(File inputFile, String destinationFileName) {
        filesToIncludeNameMap.put(inputFile, destinationFileName);
    }

    public void setFilePermissions(File file, String permissions) {
        fileToPermissionsMap.put(file, permissions);
    }

    public void excludeFile(String inputFilePath) {
        filesNamesToExclude.add(inputFilePath);
    }

    public Map<File, String> getFilesToIncludeNameMap() {
        return filesToIncludeNameMap;
    }

    public List<String> getFilesNamesToExcludeName() {
        return filesNamesToExclude;
    }

    public File createArchive(File inputDirectory, BuildDirs buildDirs, ArchiveCompression compression) throws IOException {
        File outputFile = new File(buildDirs.getTemporaryRootDirectory(), "docker-build." + (compression.equals(ArchiveCompression.none) ? "tar" : compression.getFileSuffix()));
        List<File> files = FileUtil.listFilesRecursivelyInDirectory(inputDirectory);

        if (!filesToIncludeNameMap.isEmpty()) {
            for (Map.Entry<File, String> entry : filesToIncludeNameMap.entrySet()) {
                File srcFile = entry.getKey();
                String targetFileName = entry.getValue();

                // 1. Check whether nested directory is there, if not create it
                String[] pathParts = targetFileName.split(File.separator);
                File parentDirectory = inputDirectory;
                if (pathParts.length > 0) {
                    StringBuilder finalPathBuilder = new StringBuilder();
                    for (int i = 0; i < pathParts.length - 1; i++) {
                        String pathPart = pathParts[i];
                        finalPathBuilder.append(pathPart + File.separator);
                    }
                    parentDirectory = new File(inputDirectory, finalPathBuilder.toString());
                    parentDirectory.mkdirs();
                }
                File targetFile = new File(parentDirectory, pathParts[pathParts.length - 1]);
                // Check whether file is not already created.
                if (!targetFile.exists()) {
                    FileUtils.copyFile(srcFile, targetFile);
                    files.add(targetFile);
                }
            }
        }

        List<File> fileListToAddInTarball = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            File currentFile = files.get(i);
            if (filesNamesToExclude.contains(currentFile.getName())) {
                continue;
            }
            fileListToAddInTarball.add(currentFile);
        }

        return createTarBall(outputFile, inputDirectory, fileListToAddInTarball, compression);
    }

    public File createTarBallOfDirectory(File outputFile, File inputDirectory, ArchiveCompression compression) throws IOException {
        return createTarBall(outputFile, inputDirectory, FileUtil.listFilesRecursivelyInDirectory(inputDirectory), compression);
    }

    public File createTarBall(File outputFile, File inputDirectory, List<File> fileList, ArchiveCompression compression) throws IOException {
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
