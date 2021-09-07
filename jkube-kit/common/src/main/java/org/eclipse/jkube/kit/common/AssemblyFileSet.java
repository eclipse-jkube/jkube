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
package org.eclipse.jkube.kit.common;

import java.io.File;
import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Singular;

@SuppressWarnings("JavaDoc")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class AssemblyFileSet implements Serializable {

  private static final long serialVersionUID = 1705427103183503262L;
  /**
   * Absolute or relative location from the project's directory.
   *
   * @param directory New directory for the assembly fileSet.
   * @return The assembly fileSet directory.
   */
  private File directory;
  /**
   * Output directory relative to the root of the root directory of the assembly fileSet.
   *
   * @param outputDirectory New output directory for the assembly fileSet.
   * @return The assembly fileSet output directory.
   */
  private File outputDirectory;
  /**
   *  A set of files and directories to include.
   *
   *  <p> If none is present, then everything is included.
   *
   *  <p> Files can be referenced by using their complete path name.
   *
   *  <p> Wildcards are also supported, patterns will be matched using
   *  {@link java.nio.file.FileSystem#getPathMatcher(String)} <code>glob</code> syntax.
   *
   *  <p> e.g. &#42;&#42;/&#42;.txt will match any file with a txt extension in any directory.
   *
   * @param includes New includes for the assembly fileSet.
   * @return The assembly fileSet includes
   */
  @Singular
  private List<String> includes;
  /**
   *  A set of files and directory to exclude.
   *
   *  <p> If none is present, then there are no exclusions.
   *
   * @param excludes New includes for the assembly fileSet.
   * @return The assembly fileSet includes
   */
  @Singular
  private List<String> excludes;
  /**
   * Similar to a UNIX permission, sets the file mode of the files included. THIS IS AN OCTAL VALUE.
   *
   * <p> Format: (User)(Group)(Other) where each component is a sum of Read = 4, Write = 2, and Execute = 1.
   *
   * <p> For example, the value 0644 translates to User read-write, Group and Other read-only.
   *
   * <p> The default value is 0644
   *
   * @param fileMode New file mode value for the assembly fileSet.
   * @return The assembly fileSet file mode value.
   */
  private String fileMode;
  /**
   * Similar to a UNIX permission, sets the directory mode of the directories included.
   *
   * <p> Format: (User)(Group)(Other) where each component is a sum of Read = 4, Write = 2, and Execute = 1.
   *
   * <p> For example, the value 0755 translates to User read-write, Group and Other read-only.
   *
   * <p> The default value is 0755
   *
   * @param directoryMode New file mode value for the assembly fileSet.
   * @return The assembly fileSet directory mode value.
   */
  private String directoryMode;

  // Plexus deserialization specific setters
  /**
   * Output directory relative to the root of the root directory of the assembly fileSet.
   *
   * @param outputDirectory New output directory for the assembly fileSet.
   */
  public void setOutputDirectory(String outputDirectory) {
    this.outputDirectory = new File(outputDirectory);
  }
}
