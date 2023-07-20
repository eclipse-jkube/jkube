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
package org.eclipse.jkube.kit.common;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.File;


@Getter
@Setter
@EqualsAndHashCode
public class AssemblyFileEntry {

  private long lastModified;
  private File source;
  private File dest;
  private String fileMode;

  @Builder
  public AssemblyFileEntry(File source, File dest, String fileMode) {
    this.lastModified = source.lastModified();
    this.source = source;
    this.dest = dest;
    this.fileMode = fileMode;
  }

  public boolean isUpdated() {
    if (source.lastModified() > lastModified) {
      // Update last modified as a side effect
      lastModified = source.lastModified();
      return true;
    } else {
      return false;
    }
  }
}
