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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.AssemblyFileEntry;
import org.eclipse.jkube.kit.common.AssemblyFileSet;
import org.eclipse.jkube.kit.common.util.FileUtil;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AssemblyFileSetUtils {

  private static final String PATH_TO_SELF = ".";
  private static final String DIRECTORY_MODE_DEFAULT = "040755";
  private static final String FILE_MODE_DEFAULT = "0644";

  private AssemblyFileSetUtils() {}

  @Nonnull
  public static List<AssemblyFileEntry> calculateFilePermissions(File source, File dest, AssemblyFileSet assemblyFileSet) {
    final List<AssemblyFileEntry> ret = new ArrayList<>();
    final String fileMode = Optional.ofNullable(assemblyFileSet.getFileMode()).orElse(FILE_MODE_DEFAULT);
    if (dest.isDirectory()) {
      final String directoryMode = Optional.ofNullable(assemblyFileSet.getDirectoryMode())
          .orElse(DIRECTORY_MODE_DEFAULT);
      ret.add(new AssemblyFileEntry(source, dest, directoryMode));
      FileUtil.listFilesAndDirsRecursivelyInDirectory(dest).forEach(f -> {
        final File s = source.toPath().resolve(dest.toPath().relativize(f.toPath())).toFile();
        if (f.isDirectory()) {
          ret.add(new AssemblyFileEntry(s, f, directoryMode));
        } else if(f.isFile()) {
          ret.add(new AssemblyFileEntry(s, f, fileMode));
        }
      });
    } else if (dest.isFile()) {
      ret.add(new AssemblyFileEntry(source, dest, fileMode));
    }
    return ret;
  }

  /**
   * Will copy files from the provided <code>baseDirectory</code> into <code>outputDirectory/assemblyConfiguration.targetDir</code>
   * considering the inclusion and exclusion rules defined in the provided {@link AssemblyFileSet}.
   *
   * @param baseDirectory directory from where to resolve source files.
   * @param outputDirectory directory where files should be output.
   * @param assemblyFileSet fileSet to process.
   * @param assemblyConfiguration configuration for assembly.
   * @return List containing the copied {@link AssemblyFileEntry} for the processed {@link AssemblyFileSet}
   * @throws IOException in case something goes wrong when performing File operations.
   */
  @SuppressWarnings("squid:S3864")
  @Nonnull
  public static List<AssemblyFileEntry> processAssemblyFileSet(
      File baseDirectory, File outputDirectory, AssemblyFileSet assemblyFileSet,
      AssemblyConfiguration assemblyConfiguration) throws IOException {

    final File sourceDirectory = resolveSourceDirectory(baseDirectory, assemblyFileSet);
    if (!sourceDirectory.exists()) {
      return Collections.emptyList();
    }
    final File targetDirectory = new File(outputDirectory, Objects.requireNonNull(
        assemblyConfiguration.getTargetDir(), "Assembly Configuration target dir is required"));
    final File destinationDirectory;
    if (assemblyFileSet.getOutputDirectory() == null) {
      destinationDirectory = new File(targetDirectory, sourceDirectory.getName());
    } else if (assemblyFileSet.getOutputDirectory().isAbsolute()) {
      destinationDirectory = assemblyFileSet.getOutputDirectory();
    } else if (assemblyFileSet.getOutputDirectory().getPath().equals(PATH_TO_SELF)) {
      destinationDirectory = targetDirectory;
    } else {
      destinationDirectory = targetDirectory.toPath().resolve(assemblyFileSet.getOutputDirectory().getPath()).toFile();
    }
    final List<String> includes = Optional.ofNullable(assemblyFileSet.getIncludes())
        .filter(i -> !i.isEmpty())
        .orElse(Collections.singletonList(PATH_TO_SELF));
    final List<AssemblyFileEntry> allEntries = new ArrayList<>();
    for (String include : includes) {
      final String effectiveInclude = isSelfPath(include) ? "**" : include;
      allEntries.addAll(processInclude(sourceDirectory.toPath(), effectiveInclude, destinationDirectory.toPath(), assemblyFileSet));
    }
    final List<AssemblyFileEntry> excludedEntries = allEntries.stream()
        .filter(excludeFilter(sourceDirectory.toPath(), assemblyFileSet))
        .peek(afe -> FileUtils.deleteQuietly(afe.getDest()))
        .collect(Collectors.toList());
    allEntries.removeAll(excludedEntries);
    return allEntries;
  }

  private static Set<AssemblyFileEntry> processInclude(
      Path sourceDirectory, String include, Path destinationDirectory, AssemblyFileSet assemblyFileSet) throws IOException {

    final Set<AssemblyFileEntry> entries = new LinkedHashSet<>();
    for(File sourceFile : findFilesUsingGlobMatcher(sourceDirectory, include)) {
      final File destFile = destinationDirectory.resolve(sourceDirectory.relativize(sourceFile.toPath())).toFile();
      FileUtil.createDirectory(destFile.getParentFile());
      entries.addAll(copy(sourceFile, destFile, assemblyFileSet));
    }
    return entries;
  }

  private static List<File> findFilesUsingGlobMatcher(Path sourceDirectory, String include) throws IOException {
    final PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher(String.format("glob:%s", include));
    try (Stream<Path> sourceDirectoryStream = Files.walk(sourceDirectory)) {
      return sourceDirectoryStream.filter(p -> pathMatcher.matches(sourceDirectory.relativize(p)))
          .map(Path::toFile).collect(Collectors.toList());
    }
  }

  static File resolveSourceDirectory(File baseDirectory, AssemblyFileSet assemblyFileSet) {
    if (Objects.requireNonNull(assemblyFileSet.getDirectory(), "Assembly FileSet directory is required").isAbsolute()) {
      return assemblyFileSet.getDirectory();
    } else {
      return baseDirectory.toPath()
          .resolve(assemblyFileSet.getDirectory().toPath())
          .toFile();
    }
  }

  static boolean isSelfPath(String path) {
    return StringUtils.isBlank(path) || path.equals(PATH_TO_SELF);
  }

  private static List<AssemblyFileEntry> copy(File source, File target, AssemblyFileSet assemblyFileSet) throws IOException {
    if (source.exists()) {
      if (source.isDirectory()) {
        FileUtil.copyDirectoryIfNotExists(source, target);
      } else {
        FileUtil.copy(source, target);
      }
      return calculateFilePermissions(source, target, assemblyFileSet);
    }
    return Collections.emptyList();
  }

  /**
   * Functional filter that will filter {@link AssemblyFileEntry#getSource()} files that match any of the exclude
   * paths provided in {@link AssemblyFileSet#getExcludes()} using {@link PathMatcher} glob syntax.
   *
   * @param sourceDirectory the source directory to relativize files prior to applying th path matcher
   * @param fileSet the fileSet with the declared exclude patterns
   * @return Predicate function to evaluate a Stream of {@link AssemblyFileEntry}
   */
  @Nonnull
  private static Predicate<AssemblyFileEntry> excludeFilter(@Nonnull Path sourceDirectory, @Nonnull AssemblyFileSet fileSet) {
    final List<PathMatcher> pathMatchers = Optional.ofNullable(fileSet.getExcludes()).orElse(Collections.emptyList())
        .stream()
        .map(exclude -> FileSystems.getDefault().getPathMatcher(String.format("glob:%s", exclude)))
        .collect(Collectors.toList());
    return afe -> pathMatchers.stream().anyMatch(p -> p.matches(sourceDirectory.relativize(afe.getSource().toPath())));
  }
}
