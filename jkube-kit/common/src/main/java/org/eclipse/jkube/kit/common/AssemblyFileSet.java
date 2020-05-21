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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Singular;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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
   * TODO: Whether to filter symbols in the files as they are copied, using properties from the build configuration
   *
   * @param filtered New filtered value for the assembly fileSet.
   * @return The assembly fileSet filtered value.
   */
  private boolean filtered;
  /**
   *  A set of files and directory to include.
   *
   *  <p> If none is present, then everything is included
   *
   * @param includes New includes for the assembly fileSet.
   * @return The assembly fileSet includes
   */
  @Singular
  private List<String> includes;
  /**
   *  A set of files and directory to exclude.
   *
   *  <p> If none is present, then there are no exclusions
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
   * @param fileMode New file mode value for the assembly fileSet.
   * @return The assembly fileSet file mode value.
   */
  private String fileMode;

  public void addInclude(String item) {
    if (includes == null) {
      includes = new ArrayList<>();
    }
    includes.add(item);
  }

  public void addExclude(String item) {
    if (excludes == null) {
      excludes = new ArrayList<>();
    }
    excludes.add(item);
  }

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
