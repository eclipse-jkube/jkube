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
package org.eclipse.jkube.kit.common.util;

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class UserConfigurationCompareTest {

    @DisplayName("Configs equality tests")
    @ParameterizedTest(name = "{0}")
    @MethodSource("configsTestData")
    void testConfigEqual(String testDesc, Object entity1, Object entity2, boolean expected) {
        // Given & When
        boolean result = UserConfigurationCompare.configEqual(entity1, entity2);
        // Then
        assertThat(result).isEqualTo(expected);
    }

    public static Stream<Arguments> configsTestData() {
        return Stream.of(
                Arguments.arguments("When Configs are equal should return true", "Hello", "Hello", true),
                Arguments.arguments("When either config is null should return false", null, "new", false),
                Arguments.arguments("Map and string config should return false", Collections.emptyMap(), "Hello", false),
                Arguments.arguments("When Object meta configs are equal should return true", new ObjectMetaBuilder().withName("test1").withAnnotations(Collections.singletonMap("foo", "bar")).build(),
                        new ObjectMetaBuilder().withName("test1").withAnnotations(Collections.singletonMap("foo", "bar")).build(), true),
                Arguments.arguments("When Configs are object meta and string should return false", "Hello", new ObjectMetaBuilder().withName("test1").withAnnotations(Collections.singletonMap("foo", "bar")).build(),
                        false),
                Arguments.arguments("With Collection should return false", Collections.singletonList(new ArrayList<String>()), Collections.emptyList(), false),
                Arguments.arguments("When Collection should return true", Collections.emptyList(), Collections.emptySet(), true),
                Arguments.arguments("When Configs are not equal should return false", "entity1", "entity2", false),
                Arguments.arguments("When not KDTO should return true", new KubernetesListBuilder().addToItems(new DeploymentBuilder().build()), new KubernetesListBuilder().addToItems(new DeploymentBuilder().build()),
                        true)
        );
    }

    @DisplayName("Configs equality tests with collection")
    @ParameterizedTest(name = "{0}")
    @MethodSource("configsWithCollectionTestsData")
    void testConfigEqualWhenMapAndEqual(String testDesc, String key1, String value1, String key2, String value2, boolean expected) {
        //Given
        Map<String, String> entity1 = new HashMap<>();
        Map<String, String> entity2 = new HashMap<>();
        entity1.put(key1, value1);
        entity2.put(key2, value2);
        //When
        boolean result = UserConfigurationCompare.configEqual(entity1, entity2);
        //Then
        assertThat(result).isEqualTo(expected);
    }


    public static Stream<Arguments> configsWithCollectionTestsData() {
        return Stream.of(
                Arguments.arguments("When Maps are equal should return true", "item1", "code", "item1", "code", true),
                Arguments.arguments("When Maps are not equal should return false", "item", "code", "item1", "code", false)
        );
    }

}
