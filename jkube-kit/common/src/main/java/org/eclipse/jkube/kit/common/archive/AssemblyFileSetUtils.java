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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.AssemblyFileSet;
import org.eclipse.jkube.kit.common.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class AssemblyFileSetUtils {

  private static final String PATH_TO_SELF = ".";
  private static final String DIRECTORY_CAN_LIST_PERMISSION = "040111";

  private AssemblyFileSetUtils() {}

  public static Map<File, String> calculateFilePermissions(File destFile, AssemblyFileSet assemblyFileSet) {
    final Map<File, String> ret = new HashMap<>();
    if (destFile.isDirectory()) {
      final String directoryMode = Optional.ofNullable(assemblyFileSet.getDirectoryMode())
          .orElse(DIRECTORY_CAN_LIST_PERMISSION);
      ret.put(destFile, directoryMode);
      FileUtil.listFilesAndDirsRecursivelyInDirectory(destFile).forEach(f -> {
        if (f.isDirectory()) {
          ret.put(f, directoryMode);
        } else if(f.isFile() && assemblyFileSet.getFileMode() != null) {
          ret.put(f, assemblyFileSet.getFileMode());
        }
      });
    } else if (destFile.isFile() && assemblyFileSet.getFileMode() != null) {
      ret.put(destFile, assemblyFileSet.getFileMode());
    }
    return ret;
  }

  /**
   * Will copy files from the provided <code>baseDirectory</code> into <code>outputDirectory/assemblyConfiguration.name</code>
   * considering the inclusion and exclusion rules defined in the provided {@link AssemblyFileSet}.
   *
   * <p> <b>NO WILDCARDS ARE SUPPORTED</b>
   *
   * @param baseDirectory directory from where to source files.
   * @param outputDirectory directory where files should be output.
   * @param assemblyFileSet fileSet to process.
   * @param assemblyConfiguration configuration for assembly.
   * @return Map containing the calculated permissions.
   * @throws IOException in case something goes wrong when performing File operations.
   *
   * @see <a href="https://books.sonatype.com/mvnref-book/reference/assemblies-sect-controlling-contents.html">Maven Assemblies</a> (Spec <b>partially</b> compliant)
   */
  public static Map<File, String> processAssemblyFileSet(
      File baseDirectory, File outputDirectory, AssemblyFileSet assemblyFileSet,
      AssemblyConfiguration assemblyConfiguration) throws IOException {

    final Map<File, String> fileToPermissionsMap = new HashMap<>();
    final File sourceDirectory = resolveSourceDirectory(baseDirectory, assemblyFileSet);
    final File targetDirectory = outputDirectory.toPath()
        .resolve(Objects.requireNonNull(assemblyConfiguration.getName(), "Assembly Configuration name is required"))
        .toFile();
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
    for (String include : includes) {
      if (isSelfPath(include)) {
        fileToPermissionsMap.putAll(copy(sourceDirectory, destinationDirectory, assemblyFileSet));
      } else {
        final File sourceFile = new File(sourceDirectory, include);
        final File destFile = destinationDirectory.toPath().resolve(sourceDirectory.toPath().relativize(sourceFile.toPath())).toFile();
        FileUtil.createDirectory(destFile.getParentFile());
        fileToPermissionsMap.putAll(copy(sourceFile, destFile, assemblyFileSet));
      }
    }
    return fileToPermissionsMap;
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

  private static boolean isSelfPath(String path) {
    return StringUtils.isBlank(path) || path.equals(PATH_TO_SELF);
  }

  private static  Map<File, String> copy(File source, File target, AssemblyFileSet assemblyFileSet) throws IOException {
    if (source.exists()) {
      if (source.isDirectory()) {
        FileUtil.copyDirectoryIfNotExists(source, target);
      } else {
        FileUtil.copy(source, target);
      }
      return calculateFilePermissions(target, assemblyFileSet);
    }
    return Collections.emptyMap();
  }
}
