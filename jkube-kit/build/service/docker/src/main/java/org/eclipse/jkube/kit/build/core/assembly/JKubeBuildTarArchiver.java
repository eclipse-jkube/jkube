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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jkube.kit.common.archive.ArchiveCompression;
import org.eclipse.jkube.kit.common.archive.JKubeTarArchiver;
import org.eclipse.jkube.kit.common.util.FileUtil;

import org.apache.commons.io.FileUtils;

public class JKubeBuildTarArchiver {
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
        List<File> files = FileUtil.listFilesAndDirsRecursivelyInDirectory(inputDirectory);

        if (!filesToIncludeNameMap.isEmpty()) {
            for (Map.Entry<File, String> entry : filesToIncludeNameMap.entrySet()) {
                File srcFile = entry.getKey();
                String targetFileName = entry.getValue();

                // 1. Check whether nested directory is there, if not create it
                File parentDirectory = new File(targetFileName).getParentFile();
                if (parentDirectory != null) {
                    FileUtil.createDirectory(new File(inputDirectory, parentDirectory.getPath()));
                }
                File targetFile = new File(inputDirectory, targetFileName);
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

        return JKubeTarArchiver.createTarBall(outputFile, inputDirectory, fileListToAddInTarball, fileToPermissionsMap, compression);
    }

}
