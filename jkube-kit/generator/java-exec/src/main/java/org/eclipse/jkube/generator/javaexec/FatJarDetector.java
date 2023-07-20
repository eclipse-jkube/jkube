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
package org.eclipse.jkube.generator.javaexec;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class for finding out the fat jar of a directory and provide
 * some insights into the fat jar
 * @author roland
 */
public class FatJarDetector {

    private final File directory;
    private Result result;

    FatJarDetector(File directory) {
        this.directory = directory;
    }

    public Result scan() {
        if (directory == null || !directory.exists()) {
            return null;
        }
        // Scanning is lazy ...
        if (result == null) {
            result = scanDirectory();
        }
        return result;
    }

    private Result scanDirectory() {
        final List<File> jarOrWars = Optional.ofNullable(
            directory.list((dir, name) -> name.endsWith(".war") || name.endsWith(".jar")))
            .map(files -> Stream.of(files).filter(Objects::nonNull).map(f -> new File(directory, f)).collect(Collectors.toList()))
            .orElse(Collections.emptyList());
        Result selectedJar = null;
        long maxSize = 0;
        for (File jarOrWar : jarOrWars) {
            try (JarFile archive = new JarFile(jarOrWar)){
                final Manifest mf = archive.getManifest();
                if (mf != null && mf.getMainAttributes() != null) {
                    final Attributes mainAttributes = mf.getMainAttributes();
                    String mainClass = mainAttributes.getValue("Main-Class");
                    if (mainClass != null) {
                        long size = jarOrWar.length();
                        // Take the largest jar / war file found
                        if (size > maxSize) {
                            maxSize = size;
                            selectedJar = new Result(jarOrWar, mainClass, mainAttributes);
                        }
                    }
                }
            } catch (IOException e) {
                throw new IllegalStateException("Cannot examine file " + jarOrWar.getName() + " for the manifest");
            }
        }
        return selectedJar;
    }

    public static final class Result {

        private final File archiveFile;
        private final String mainClass;
        private final Attributes attributes;

        private Result(File archiveFile, String mainClass, Attributes attributes) {
            this.archiveFile = archiveFile;
            this.mainClass = mainClass;
            this.attributes = attributes;
        }

        public File getArchiveFile() {
            return archiveFile;
        }

        public String getMainClass() {
            return mainClass;
        }

        public String getManifestEntry(String key) {
            return attributes.getValue(key);
        }
    }
}
