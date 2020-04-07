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

}
