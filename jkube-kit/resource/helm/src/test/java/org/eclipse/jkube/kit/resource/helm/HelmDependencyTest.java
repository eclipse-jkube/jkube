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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class HelmDependencyTest {

  @Test
  public void equalsAndHashCodeTest() {

    // Given
    HelmDependency helmDependency = HelmDependency
        .builder()
        .name("name")
        .repository("repository")
        .version("version")
        .build();

    // Then
    assertThat(helmDependency).isEqualTo(helmDependency);
    assertThat(helmDependency.getName()).isEqualTo("name");
    assertThat(helmDependency.getRepository()).isEqualTo("repository");
    assertThat(helmDependency.getVersion()).isEqualTo("version");
  }

  @Test
  public void contructorTest() {

    // Given
    HelmDependency helmDependency = new HelmDependency();
    helmDependency.setVersion("version");
    helmDependency.setName("name");
    helmDependency.setRepository("repository");

    // Then
    assertThat(helmDependency).isEqualTo(helmDependency);
    assertThat(helmDependency.getName()).isEqualTo("name");
    assertThat(helmDependency.getRepository()).isEqualTo("repository");
    assertThat(helmDependency.getVersion()).isEqualTo("version");
  }
}
