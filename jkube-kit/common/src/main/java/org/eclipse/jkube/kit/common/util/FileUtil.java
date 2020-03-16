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
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

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

    public static ArrayList<String> fileReadArray(File file) throws IOException {
        ArrayList<String> lineList = new ArrayList();

        try (Stream<String> lines = Files.lines(Paths.get(file.getAbsolutePath()), Charset.defaultCharset())) {
            lines.forEachOrdered(line -> lineList.add(line));
        }
        return lineList;
    }

    // Taken from https://github.com/sonatype/plexus-utils/blob/master/src/main/java/org/codehaus/plexus/util/PathTool.java
    public static final String getRelativeFilePath( final String oldPath, final String newPath )
    {
        if ( StringUtils.isEmpty( oldPath ) || StringUtils.isEmpty( newPath ) )
        {
            return "";
        }

        // normalise the path delimiters
        String fromPath = new File( oldPath ).getPath();
        String toPath = new File( newPath ).getPath();

        // strip any leading slashes if its a windows path
        if ( toPath.matches( "^\\[a-zA-Z]:" ) )
        {
            toPath = toPath.substring( 1 );
        }
        if ( fromPath.matches( "^\\[a-zA-Z]:" ) )
        {
            fromPath = fromPath.substring( 1 );
        }

        // lowercase windows drive letters.
        if ( fromPath.startsWith( ":", 1 ) )
        {
            fromPath = Character.toLowerCase( fromPath.charAt( 0 ) ) + fromPath.substring( 1 );
        }
        if ( toPath.startsWith( ":", 1 ) )
        {
            toPath = Character.toLowerCase( toPath.charAt( 0 ) ) + toPath.substring( 1 );
        }

        // check for the presence of windows drives. No relative way of
        // traversing from one to the other.
        if ( ( toPath.startsWith( ":", 1 ) && fromPath.startsWith( ":", 1 ) ) && ( !toPath.substring( 0, 1 ).equals(
                fromPath.substring( 0, 1 ) ) ) )
        {
            // they both have drive path element but they dont match, no
            // relative path
            return null;
        }

        if ( ( toPath.startsWith( ":", 1 ) && !fromPath.startsWith( ":", 1 ) ) || ( !toPath.startsWith( ":", 1 )
                && fromPath.startsWith( ":", 1 ) ) )
        {
            // one has a drive path element and the other doesnt, no relative
            // path.
            return null;
        }

        String resultPath = buildRelativePath( toPath, fromPath, File.separatorChar );

        if ( newPath.endsWith( File.separator ) && !resultPath.endsWith( File.separator ) )
        {
            return resultPath + File.separator;
        }

        return resultPath;
    }

    private static final String buildRelativePath( String toPath, String fromPath, final char separatorChar )
    {
        // use tokeniser to traverse paths and for lazy checking
        StringTokenizer toTokeniser = new StringTokenizer( toPath, String.valueOf( separatorChar ) );
        StringTokenizer fromTokeniser = new StringTokenizer( fromPath, String.valueOf( separatorChar ) );

        int count = 0;

        // walk along the to path looking for divergence from the from path
        while ( toTokeniser.hasMoreTokens() && fromTokeniser.hasMoreTokens() )
        {
            if ( separatorChar == '\\' )
            {
                if ( !fromTokeniser.nextToken().equalsIgnoreCase( toTokeniser.nextToken() ) )
                {
                    break;
                }
            }
            else
            {
                if ( !fromTokeniser.nextToken().equals( toTokeniser.nextToken() ) )
                {
                    break;
                }
            }

            count++;
        }

        // reinitialise the tokenisers to count positions to retrieve the
        // gobbled token

        toTokeniser = new StringTokenizer( toPath, String.valueOf( separatorChar ) );
        fromTokeniser = new StringTokenizer( fromPath, String.valueOf( separatorChar ) );

        while ( count-- > 0 )
        {
            fromTokeniser.nextToken();
            toTokeniser.nextToken();
        }

        String relativePath = "";

        // add back refs for the rest of from location.
        while ( fromTokeniser.hasMoreTokens() )
        {
            fromTokeniser.nextToken();

            relativePath += "..";

            if ( fromTokeniser.hasMoreTokens() )
            {
                relativePath += separatorChar;
            }
        }

        if ( relativePath.length() != 0 && toTokeniser.hasMoreTokens() )
        {
            relativePath += separatorChar;
        }

        while ( toTokeniser.hasMoreTokens() )
        {
            relativePath += toTokeniser.nextToken();

            if ( toTokeniser.hasMoreTokens() )
            {
                relativePath += separatorChar;
            }
        }
        return relativePath;
    }

    public static void copy(File sourceFile, File targetFile) throws IOException {
        copy(Paths.get(sourceFile.getAbsolutePath()), Paths.get(targetFile.getAbsolutePath()));
    }

    public static void copy(Path sourcePath, Path targetPath) throws IOException {
        Files.copy(sourcePath, targetPath, REPLACE_EXISTING);
    }

    public static void copyDirectory(File sourceDir, File targetDir) throws IOException {
        if (targetDir.exists() && targetDir.isDirectory() && !isDirEmpty(targetDir.toPath())) {
            // Directory already exists, return
            return;
        }

        Path sourcePath = Paths.get(sourceDir.getAbsolutePath());
        Path targetPath = Paths.get(targetDir.getAbsolutePath());

        Files.walk(sourcePath).forEach((source) -> {
            try {
                Path target = targetPath.resolve(sourcePath.relativize(source));
                Files.copy(source, target, REPLACE_EXISTING);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        });
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

    public static List<File> listFilesRecursivelyInDirectory(File directory) {
        return new ArrayList<>(FileUtils.listFiles(
                directory,
                new RegexFileFilter("^(.*?)"),
                DirectoryFileFilter.DIRECTORY
        ));
    }

    public static void createDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            boolean isCreated = directory.mkdirs();
            if (!isCreated) {
                throw new IOException("Failed to create directory: " + directory.getAbsolutePath());
            }
        }
    }
}

