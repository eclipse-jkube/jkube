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
package org.eclipse.jkube.kit.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GenericCustomResourceEqualsHashCodeTest {

  @Test
  public void equals_withNull_shouldBeFalse() {
    // Given
    final GenericCustomResource gcr = new GenericCustomResource();
    // When - Then
    final boolean result = gcr.equals(null);
    // Then
    assertThat(result).isFalse();
  }

  @Test
  public void equals_withSameFields_shouldBeTrue() {
    // Given
    final GenericCustomResource one = initGenericCustomResource();
    final GenericCustomResource other = initGenericCustomResource();
    // When - Then
    assertThat(one).isEqualTo(other).isNotSameAs(other);
  }

  @Test
  public void equals_withDifferentMapFields_shouldBeFalse() {
    // Given
    final GenericCustomResource one = initGenericCustomResource();
    final GenericCustomResource other = initGenericCustomResource();
    other.setAdditionalProperty("extra", "other");
    // When - Then
    assertThat(one).isNotEqualTo(other).isNotSameAs(other);
  }

  @Test
  public void hashSet_withSomeDuplicates_duplicatesAreRemoved() {
    // Given
    final Set<GenericCustomResource> uniqueValues = new HashSet<>();
    uniqueValues.add(initGenericCustomResource());
    uniqueValues.add(initGenericCustomResource());
    final GenericCustomResource different = initGenericCustomResource();
    different.setKind("Other");
    uniqueValues.add(different);
    // When - Then
    assertThat(uniqueValues).hasSize(2).extracting("kind").containsExactlyInAnyOrder("Kind", "Other");
  }

  private GenericCustomResource initGenericCustomResource() {
    final GenericCustomResource gcr = new GenericCustomResource();
    gcr.setApiVersion("v1");
    gcr.setKind("Kind");
    gcr.setMetadata(new ObjectMetaBuilder().withName("name").build());
    gcr.setAdditionalProperties(new HashMap<>(Collections.singletonMap("key", "value")));
    return gcr;
  }
}
