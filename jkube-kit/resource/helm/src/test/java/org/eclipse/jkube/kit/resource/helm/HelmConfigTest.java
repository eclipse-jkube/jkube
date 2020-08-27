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

import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class HelmConfigTest {

  @Test
  public void helmTypeParseStringNullTest() {
    // When
    final List<HelmConfig.HelmType> result = HelmConfig.HelmType.parseString(null);
    // Then
    assertThat(result.isEmpty(), is(true));
  }

  @Test
  public void helmTypeParseStringEmptyTest() {
    // When
    final List<HelmConfig.HelmType> result = HelmConfig.HelmType.parseString(" ");
    // Then
    assertThat(result.isEmpty(), is(true));
  }

  @Test
  public void helmTypeParseStringEmptiesTest() {
    // When
    final List<HelmConfig.HelmType> result = HelmConfig.HelmType.parseString(",,  ,   ,, ");
    // Then
    assertThat(result.isEmpty(), is(true));
  }

  @Test
  public void helmTypeParseStringKubernetesTest() {
    // When
    final List<HelmConfig.HelmType> result = HelmConfig.HelmType.parseString("kuBerNetes");
    // Then
    assertThat(result, hasItem(HelmConfig.HelmType.KUBERNETES));
  }

  @Test(expected = IllegalArgumentException.class)
  public void helmTypeParseStringKubernetesInvalidTest() {
    // When
    HelmConfig.HelmType.parseString("OpenShift,Kuberentes");
    // Then > throw Exception
    fail();
  }
}
