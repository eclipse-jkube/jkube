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

public class ChartMuseumHelmRepositoryUploader extends StandardRepositoryUploader {

  public ChartMuseumHelmRepositoryUploader(KitLogger logger) {
    super("POST", logger, HelmRepository.HelmRepoType.CHARTMUSEUM);
  }

  @Override
  public String url(File helmChart, HelmRepository repository) {
    return repository.getUrl();
  }
}
