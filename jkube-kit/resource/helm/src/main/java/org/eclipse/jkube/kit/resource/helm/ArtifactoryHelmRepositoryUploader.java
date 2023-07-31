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
package org.eclipse.jkube.kit.resource.helm;

import java.io.File;

public class ArtifactoryHelmRepositoryUploader extends StandardRepositoryUploader {

  public ArtifactoryHelmRepositoryUploader() {
    super("PUT", HelmRepository.HelmRepoType.ARTIFACTORY);
  }

  @Override
  public String url(File helmChart, HelmRepository repository) {
    return formatRepositoryURL(helmChart, repository);
  }
}
