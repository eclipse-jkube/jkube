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
package org.eclipse.jkube.kit.api;

import java.io.File;
import java.io.IOException;

public interface JKubeBuildPlugin {
  /**
   * Method for creating the startup script together with jkube
   *
   * @param targetDir target directory where we want extracted files to be copied.
   * @throws IOException in case of any failure while copying extracted files.
   */
  void addExtraFiles(File targetDir) throws IOException;

  /**
   * Return fully qualified name of the implementation
   * @return a string containing full name of class
   */
  String getName();
}
