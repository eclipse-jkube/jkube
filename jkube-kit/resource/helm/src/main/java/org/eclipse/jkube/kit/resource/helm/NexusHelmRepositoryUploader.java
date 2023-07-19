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

import java.io.File;

public class NexusHelmRepositoryUploader extends StandardRepositoryUploader {
  public NexusHelmRepositoryUploader(KitLogger logger) {
    super("PUT", logger, HelmRepository.HelmRepoType.NEXUS);
  }

  @Override
  public String url(File helmChart, HelmRepository repository) {
    String url = formatRepositoryURL(helmChart, repository);
    if (url.endsWith(".tar.gz")) {
      url = url.replaceAll("tar.gz$", "tgz");
    }
    return url;
  }
}
