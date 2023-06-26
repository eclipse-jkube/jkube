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
package org.eclipse.jkube.kit.common.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ArtifactUtil {
  static final String LAST_MODIFIED_TIME_SAVE_FILENAME = ".jkube-final-output-artifact-last-modified";

  private ArtifactUtil() {
  }

  /**
   * Saves the last modified time of the current artifact to the given directory.
   *
   * @param saveDirectoryPath the directory where the last modified time should be saved
   * @param artifact the current artifact
   * @throws IOException if an I/O error occurs
   */
  public static void saveCurrentArtifactLastModifiedTime(Path saveDirectoryPath, File artifact) throws IOException {
    try (DataOutputStream out = new DataOutputStream(
        Files.newOutputStream(saveDirectoryPath.resolve(LAST_MODIFIED_TIME_SAVE_FILENAME)))) {
      out.writeLong(artifact.lastModified());
    }
  }

  /**
   * Retrieves the last modified time of the previously built artifact from the given directory.
   *
   * @param loadDirectoryPath the directory where the last modified time was saved
   * @return the last modified time of the previously built artifact
   * @throws IOException if an I/O error occurs
   */
  public static long retrievePreviousArtifactLastModifiedTime(Path loadDirectoryPath) throws IOException {
    try (DataInputStream in = new DataInputStream(
        Files.newInputStream(loadDirectoryPath.resolve(LAST_MODIFIED_TIME_SAVE_FILENAME)))) {
      return in.readLong();
    }
  }
}
