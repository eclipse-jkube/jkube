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
package org.eclipse.jkube.kit.common.util;

import org.eclipse.jkube.kit.common.KitLogger;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * File related methods which cannot be found elsewhere
 * @author roland
 * @since 23.05.17
 */
public class FileUtil {

    public static File createTempDirectory() {
        try {
            return Files.createTempDirectory("jkube").toFile();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static File getRelativePath(File baseDir, File file) {
        Path baseDirPath = Paths.get(baseDir.getAbsolutePath());
        Path filePath = Paths.get(file.getAbsolutePath());
        return baseDirPath.relativize(filePath).toFile();
    }

    public static String stripPrefix(String text, String prefix) {
        if (text.startsWith(prefix)) {
            return text.substring(prefix.length());
        }
        return text;
    }

    public static String stripPostfix(String text, String postfix) {
        if (text.endsWith(postfix)) {
            return text.substring(text.length() - postfix.length());
        }
        return text;
    }


    /**
     * Returns the absolute path to a file with name <code>fileName</code>
     * @param fileName the name of a file
     * @return absolute path to the file
     */
    public static String getAbsolutePath(String fileName) {
        return Paths.get(fileName).toAbsolutePath().toString();
    }

    /**
     * Returns the absolute path to a resource addressed by the given <code>url</code>
     * @param url resource URL
     * @return absolute path to the resource
     */
    public static String getAbsolutePath(URL url) {
        try {
            return url != null ? Paths.get(url.toURI()).toAbsolutePath().toString() : null;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static void downloadRemotes(final File outputDirectory, List<String> remotes, KitLogger log) {

        if (!outputDirectory.exists()) {
            try {
                Files.createDirectories(outputDirectory.toPath());
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }

        remotes.stream()
                .map(remote -> {
                    try {
                        return new URL(remote);
                    } catch (MalformedURLException e) {
                        throw new IllegalArgumentException(e);
                    }
                })
                .forEach(url -> {
                    try {
                        IoUtil.download(log, url, new File(outputDirectory, getOutputName(url)));
                    } catch (IOException e) {
                        throw new IllegalArgumentException(e);
                    }
                });
    }

    private static String getOutputName(URL url) {
        final String path = url.getPath();

        final int slashIndex = path.lastIndexOf('/');
        if (slashIndex >= 0) {
            return path.substring(slashIndex + 1);
        } else {
            throw new IllegalArgumentException(String.format("URL %s should contain a name file to be downloaded.", url.toString()));
        }

    }
}

