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
package org.eclipse.jkube.kit.build.service.docker.watch;

import java.io.File;
import java.io.IOException;

@FunctionalInterface
public interface CopyFilesTask {

  /**
   * Implementations should handle this method so the passed {@link File} is extracted in
   * the root directory of the running Container.
   *
   * @param changedFilesArchive File with the reference to the generated change files archive.
   * @throws WatchException in case the copy operation doesn't complete successfully.
   */
  void copy(File changedFilesArchive) throws IOException, WatchException;
}
