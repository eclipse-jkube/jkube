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
public class JKubeProjectDependency implements Serializable {

  private static final long serialVersionUID = 1446536983695411537L;

  /**
   * Maven group ID.
   *
   * @param groupId New maven group ID for the dependency.
   * @return The maven group ID for the dependency.
   */
  private String groupId;
  /**
   * Maven artifact ID.
   *
   * @param artifactId New maven artifact ID for the dependency.
   * @return The maven artifact ID for the dependency.
   */
  private String artifactId;
  /**
   * Maven version.
   *
   * @param version New maven version for the dependency.
   * @return The maven version for the dependency.
   */
  private String version;
  /**
   * Dependency type (e.g. jar, war, etc.).
   *
   * @param type New type for the dependency.
   * @return The dependency type.
   */
  private String type;
  /**
   * Dependency scope (e.g. compile, provided, runtime, test, system).
   *
   * @param scope New scope for the dependency.
   * @return The dependency scope.
   */
  private String scope;
  /**
   * Dependency file.
   *
   * @param file New file for the dependency.
   * @return The dependency file.
   */
  private File file;

  public static JKubeProjectDependency fromString(String jkubeDependencyAsString) {
    String[] parts = jkubeDependencyAsString.split(",");
    if (parts.length == 5) { // Case without artifact file object
      return new JKubeProjectDependency(parts[0], parts[1], parts[2], parts[3], parts[4], null);
    } else if (parts.length == 6) { // Case with artifact file object
      return new JKubeProjectDependency(parts[0], parts[1], parts[2], parts[3], parts[4], new File(parts[5]));
    }
    return null;
  }

  public static List<JKubeProjectDependency> listFromStringDependencies(List<String> jkubeDependenciesAsStr) {
    List<JKubeProjectDependency> dependencies = new ArrayList<>();
    for (String commaSeparatedDependencies : jkubeDependenciesAsStr) {
      JKubeProjectDependency jkubeProjectDependency = JKubeProjectDependency.fromString(commaSeparatedDependencies);
      if (jkubeProjectDependency != null) {
        dependencies.add(jkubeProjectDependency);
      }
    }
    return dependencies;
  }
}
