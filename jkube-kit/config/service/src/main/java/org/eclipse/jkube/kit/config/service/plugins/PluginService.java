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
package org.eclipse.jkube.kit.config.service.plugins;

import org.eclipse.jkube.api.JKubePlugin;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;

import java.io.File;

public interface PluginService {

  /**
   * Creates the <code>jkube-extra</code> directory and calls the {@link JKubePlugin#addExtraFiles(File)} method for
   * each {@link JKubePlugin} available.
   *
   * @throws JKubeServiceException in case there's an error while processing the plugins.
   */
  void addExtraFiles() throws JKubeServiceException;
}
