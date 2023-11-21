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

import org.eclipse.jkube.kit.common.KitLogger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class ArtifactUtil {

  private static final String LAST_MODIFIED_TIME_SAVE_FILENAME = ".jkube-last-modified";

  private ArtifactUtil() {
  }

  /**
   * Logs a warning in case the provided {@link File} artifact is stale.
   *
   * This happens if the artifact was not rebuilt since the last time this method was called.
   *
   * @param logger the {@link KitLogger} to use to log the warning.
   * @param artifact the File artifact for which to check staleness.
   */
  public static void warnStaleArtifact(KitLogger logger, File artifact) {
    if (artifact == null || artifact.isDirectory() || !artifact.exists()) {
      logger.warn("Final output artifact file was not detected. The project may have not been built. " +
        "HINT: try to compile and package your application prior to running the container image build task.");
      return;
    }
    final Path lastModifiedMarker = artifact.getParentFile().toPath().resolve(LAST_MODIFIED_TIME_SAVE_FILENAME);
    try {
      if (lastModifiedMarker.toFile().exists() &&
        Long.parseLong(new String(Files.readAllBytes(lastModifiedMarker))) == artifact.lastModified()
      ) {
        logger.info("Final output artifact file was not rebuilt since last build. " +
          "HINT: try to compile and package your application prior to running the container image build task.");
      }
      Files.write(lastModifiedMarker, String.valueOf(artifact.lastModified()).getBytes());
    } catch (Exception ex) {
      // NOOP - prevent serious stuff from failing due to this hint warning
    }
  }
}
