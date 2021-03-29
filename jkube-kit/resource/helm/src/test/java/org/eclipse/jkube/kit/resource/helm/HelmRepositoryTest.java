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

import org.eclipse.jkube.kit.resource.helm.HelmRepository.HelmRepoType;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class HelmRepositoryTest {

  @Test
  public void createHelmRepository() {
    // Given
    HelmRepository helmRepository = new HelmRepository();
    helmRepository.setType(HelmRepoType.ARTIFACTORY);
    helmRepository.setPassword("password");
    helmRepository.setUsername("username");
    helmRepository.setUrl("url");
    helmRepository.setName("name");
    // Then
    Assertions.assertThat(helmRepository.getPassword()).isEqualTo("password");
    Assertions.assertThat(helmRepository.getUsername()).isEqualTo("username");
    Assertions.assertThat(helmRepository.getUrl()).isEqualTo("url");
    Assertions.assertThat(helmRepository.getName()).isEqualTo("name");
    Assertions.assertThat(helmRepository.getType()).isEqualTo(HelmRepoType.ARTIFACTORY);
    Assertions.assertThat(helmRepository.toString()).hasToString("[name / url]");
  }
}
