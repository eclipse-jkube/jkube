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
package org.eclipse.jkube.kit.common.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.common.KitLogger;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * File related methods which cannot be found elsewhere
 * @author roland
 */
public class FileUtil {

    private FileUtil() { }

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
            return text.substring(0, text.length() - postfix.length());
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

    public static void cleanDirectory(File directoryFile) throws IOException {
        Path directory = Paths.get(directoryFile.getAbsolutePath());
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * <b>Adapted from https://github.com/sonatype/plexus-utils/blob/5ba6cfcca911200b5b9d2b313bb939e6d7cbbac6/src/main/java/org/codehaus/plexus/util/PathTool.java#L302</b>
     *
     * <p>This method can calculate the relative path between two paths on a file system.
     * <br>
     * <pre>{@code
     * getRelativeFilePath( null, null )                                   = ""
     * getRelativeFilePath( null, "/usr/local/java/bin" )                  = ""
     * getRelativeFilePath( "/usr/local", null )                           = ""
     * getRelativeFilePath( "/usr/local", "/usr/local/java/bin" )          = "java/bin"
     * getRelativeFilePath( "/usr/local", "/usr/local/java/bin/" )         = "java/bin"
     * getRelativeFilePath( "/usr/local/java/bin", "/usr/local/" )         = "../.."
     * getRelativeFilePath( "/usr/local/", "/usr/local/java/bin/java.sh" ) = "java/bin/java.sh"
     * getRelativeFilePath( "/usr/local/java/bin/java.sh", "/usr/local/" ) = "../../.."
     * getRelativeFilePath( "/usr/local/", "/bin" )                        = "../../bin"
     * getRelativeFilePath( "/bin", "/usr/local/" )                        = "../usr/local"
     * }</pre>
     * Note: On Windows based system, the <code>/</code> character should be replaced by <code>\</code> character.
     *
     * @param oldFilePath the old file path
     * @param newFilePath the new file path
     * @return a relative file path from <code>oldFilePath</code>.
     */
    //
    public static String getRelativeFilePath( final String oldFilePath, final String newFilePath )
    {
        if (StringUtils.isEmpty(oldFilePath) || StringUtils.isEmpty(newFilePath)) {
            return "";
        }
        final Path oldPath = new File(oldFilePath).toPath();
        final Path newPath = new File(newFilePath).toPath();
        if (!Objects.equals(oldPath.getRoot(), newPath.getRoot())) {
            return null;
        }
        final StringBuilder relativeFilePath = new StringBuilder();
        relativeFilePath.append(oldPath.relativize(newPath).toString());
        if (newFilePath.endsWith(File.separator)) {
            relativeFilePath.append(File.separator);
        }
        return relativeFilePath.toString();
    }


    public static void copy(File sourceFile, File targetFile) throws IOException {
        copy(Paths.get(sourceFile.getAbsolutePath()), Paths.get(targetFile.getAbsolutePath()));
    }

    public static void copy(Path sourcePath, Path targetPath) throws IOException {
        Files.copy(sourcePath, targetPath, REPLACE_EXISTING, COPY_ATTRIBUTES);
    }

    public static void copyDirectoryIfNotExists(File sourceDir, File targetDir) throws IOException {
        if (targetDir.exists() && targetDir.isDirectory() && !isDirEmpty(targetDir.toPath())) {
            return;
        }
        final Path sourcePath = sourceDir.toPath();
        try (Stream<Path> sourceTree = Files.walk(sourcePath)) {
          for (Path source : sourceTree.collect(Collectors.toList())){
            Path target = targetDir.toPath().resolve(sourcePath.relativize(source));
            FileUtils.forceMkdir(target.toFile());
            Files.copy(source, target, REPLACE_EXISTING);
          }
        }
    }

    private static boolean isDirEmpty(final Path directory) throws IOException {
        try(DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory)) {
            return !dirStream.iterator().hasNext();
        }
    }

    public static String trimWildcardCharactersFromPath(String filePath) {
        if (!filePath.endsWith("*")) {
            return filePath;
        }
        int charIndex = filePath.length() - 1;

        while (filePath.charAt(charIndex) == '*') {
            charIndex--;
        }

        return filePath.substring(0, charIndex);
    }

    public static List<File> listFilesAndDirsRecursivelyInDirectory(File directory) {
        return FileUtils.listFilesAndDirs(directory, new RegexFileFilter("^(.*?)"),
            DirectoryFileFilter.DIRECTORY
        )
            .stream()
            .filter(f -> !f.equals(directory))
            .collect(Collectors.toList());
    }

    public static void createDirectory(File directory) throws IOException {
      FileUtils.forceMkdir(directory);
        if (!directory.exists()) {
          throw new IOException("Failed to create directory: " + directory.getAbsolutePath());
        }
    }
}

