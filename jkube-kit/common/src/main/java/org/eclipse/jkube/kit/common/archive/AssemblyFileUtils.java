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
package org.eclipse.jkube.kit.common.archive;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.AssemblyFile;

import java.io.File;
import java.util.Objects;

public class AssemblyFileUtils {

  private AssemblyFileUtils() {}

  public static File getAssemblyFileOutputDirectory(
      AssemblyFile assemblyFile, File outputDirectoryForRelativePaths,
      Assembly layer, AssemblyConfiguration assemblyConfiguration) {
    final File outputDirectory;

    Objects.requireNonNull(assemblyFile.getOutputDirectory(), "Assembly Configuration output dir is required");

    if (assemblyFile.getOutputDirectory().isAbsolute()) {
      outputDirectory = assemblyFile.getOutputDirectory();
    } else if (StringUtils.isBlank(layer.getId())) {
      Objects.requireNonNull(assemblyConfiguration.getTargetDir(), "Assembly Configuration target dir is required");
      outputDirectory = new File(outputDirectoryForRelativePaths, assemblyConfiguration.getTargetDir()).toPath()
          .resolve(assemblyFile.getOutputDirectory().toPath())
          .toFile();
    } else {
      Objects.requireNonNull(assemblyConfiguration.getTargetDir(), "Assembly Configuration target dir is required");
      outputDirectory = new File(new File(outputDirectoryForRelativePaths, layer.getId()),
          assemblyConfiguration.getTargetDir()).toPath()
          .resolve(assemblyFile.getOutputDirectory().toPath())
          .toFile();
    }
    return outputDirectory.toPath().normalize().toFile();
  }

  public static File resolveSourceFile(File baseDirectory, AssemblyFile assemblyFile) {
    return baseDirectory.toPath()
        .resolve(Objects.requireNonNull(assemblyFile.getSource(), "Assembly File source is required").toPath())
        .toFile();
  }
}
