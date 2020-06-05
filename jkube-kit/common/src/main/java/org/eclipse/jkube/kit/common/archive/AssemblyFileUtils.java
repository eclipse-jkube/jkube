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

import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.AssemblyFile;

import java.io.File;
import java.util.Objects;

public class AssemblyFileUtils {

  private AssemblyFileUtils() {}

  public static File getAssemblyFileOutputDirectory(
      AssemblyFile assemblyFile, File outputDirectoryForRelativePaths, AssemblyConfiguration assemblyConfiguration) {
    final File outputDirectory;
    if (assemblyFile.getOutputDirectory().isAbsolute()) {
      outputDirectory = assemblyFile.getOutputDirectory();
    } else {
      outputDirectory = outputDirectoryForRelativePaths.toPath()
          .resolve(Objects.requireNonNull(assemblyConfiguration.getName(), "Assembly Configuration name is required"))
          .resolve(assemblyFile.getOutputDirectory().toPath())
          .toFile();
    }
    return outputDirectory;
  }

  public static File resolveSourceFile(File baseDirectory, AssemblyFile assemblyFile) {
    return baseDirectory.toPath()
        .resolve(Objects.requireNonNull(assemblyFile.getSource(), "Assembly File source is required").toPath())
        .toFile();
  }
}
