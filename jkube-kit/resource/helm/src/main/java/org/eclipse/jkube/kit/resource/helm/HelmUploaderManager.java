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
package org.eclipse.jkube.kit.resource.helm;

import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.PluginServiceFactory;

import java.util.List;

public class HelmUploaderManager {
  private static final String[] SERVICE_PATHS = new String[] {
      "META-INF/jkube/helm-uploaders"
  };

  private final List<HelmUploader> helmUploaderList;

  public HelmUploaderManager(KitLogger log) {
    this.helmUploaderList = new PluginServiceFactory<>(log).createServiceObjects(SERVICE_PATHS);
  }

  public HelmUploader getHelmUploader(HelmRepository.HelmRepoType type) {
    for (HelmUploader helmUploader : helmUploaderList) {
      if (helmUploader.getType().equals(type)) {
        return helmUploader;
      }
    }
    throw new IllegalStateException("Could not find Helm Uploader for type " + type);
  }
}