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
package org.eclipse.jkube.kit.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class JKubeFileInterpolatorTest {

    @Test
    void testSimpleSubstitution_TwoExpressions() {
        // Given
        Properties p = new Properties();
        p.setProperty("key", "value");
        p.setProperty("key2", "value2");

        // When
        String result = JKubeFileInterpolator.interpolate("${key}-${key2}", p, Collections.singletonMap("${", "}"));

        // Then
        assertThat(result).isEqualTo("value-value2");
    }

    @Test
    void testShouldFailOnExpressionCycleParsed() {
        // Given
        Properties props = new Properties();
        props.setProperty("key1", "${key2}");
        props.setProperty("key2", "${key1}");

        assertThatIllegalArgumentException()
                .as("Should detect expression cycle and fail.")
                .isThrownBy(() -> JKubeFileInterpolator.interpolate("${key1}", props, Collections.singletonMap("${", "}")));
    }

    @Test
    void testShouldFailOnExpressionCycleDirect() {
        // Given
        Properties props = new Properties();
        props.setProperty("key1", "key2");
        props.setProperty("key2", "key1");
        assertThatIllegalArgumentException()
                .as("Should detect expression cycle and fail.")
                .isThrownBy(() -> JKubeFileInterpolator.interpolate("${key1}", props, Collections.singletonMap("${", "}")));
    }

    @Test
    void testShouldResolveTransitiveReferences() {
        // Given
        Properties props = new Properties();
        props.setProperty("key1", "${key2}");
        props.setProperty("key2", "${key3}");
        props.setProperty("key3", "value");

        String result = JKubeFileInterpolator.interpolate("${key1}", props, Collections.singletonMap("${", "}"));

        assertThat(result).isEqualTo("value");
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void testShouldResolveByEnvVarInLinux() {
        // Given
        Properties p = new Properties();
        String result = JKubeFileInterpolator.interpolate("this is a ${env.HOME} ${env.PATH}", p, Collections.singletonMap("${env.", "}"));

        // When + Then
        assertThat(result)
                .isNotEqualTo("this is a ${HOME} ${PATH}")
                .isNotEqualTo("this is a ${env.HOME} ${env.PATH}");
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testShouldResolveByEnvVarInWindows() {
        // Given
        Properties p = new Properties();
        String result = JKubeFileInterpolator.interpolate("this is a ${env.USERNAME} ${env.OS}", p, Collections.singletonMap("${env.", "}"));

        // When + Then
        assertThat(result)
                .isNotEqualTo("this is a ${USERNAME} ${OS}")
                .isNotEqualTo("this is a ${env.USERNAME} ${env.OS}");
    }

    @Test
    void testLinkedInterpolators() {
        // Given
        final String EXPR = "${test.label}AND${test2}";
        final String EXPR2 = "${test.label}${test2.label}AND${test2}";
        Properties p = new Properties();
        p.put("test.label", "p");
        p.put("test2", "x");

        // When
        String result1 = JKubeFileInterpolator.interpolate(EXPR, p, Collections.singletonMap("${", "}"));
        p.put("test2.label", "zz");
        String result2 = JKubeFileInterpolator.interpolate(EXPR2, p, Collections.singletonMap("${", "}"));

        // Then
        assertThat(result1).isEqualTo("pANDx");
        assertThat(result2).isEqualTo("pzzANDx");
    }

    @Test
    void testDominance() {
        // Given
        final String EXPR = "${test.label}AND${test2}";
        final String EXPR2 = "${test.label}${test2.label}AND${test2}";
        Properties p1 = new Properties();
        p1.put("test.label", "p");
        p1.put("test2", "x");
        p1.put("test2.label", "dominant");

        // When + Then
        assertThat(JKubeFileInterpolator.interpolate(EXPR, p1,Collections.singletonMap("${", "}"))).isEqualTo("pANDx");
        assertThat(JKubeFileInterpolator.interpolate(EXPR2, p1, Collections.singletonMap("${", "}"))).isEqualTo("pdominantANDx");
    }

    @Test
    void testCyclesWithLinked() {
        // Given
        Properties p = new Properties();
        p.put("key1", "${key2}");
        p.put("key2", "${key2}");
        assertThatIllegalArgumentException()
                .as("Should detect expression cycle and fail.")
                .isThrownBy(() -> JKubeFileInterpolator.interpolate("${key2}", p, Collections.singletonMap("${", "}")));
    }

    @Test
    void testCyclesWithLinked_betweenRootAndOther() {
        // Given
        Properties p = new Properties();
        p.put("key1", "${key2}");
        p.put("key2", "${key1}");
        assertThatIllegalArgumentException()
                .as("Should detect expression cycle and fail.")
                .isThrownBy(() -> JKubeFileInterpolator.interpolate("${key1}", p, Collections.singletonMap("${", "}")));
    }

    @Test
    void testGetExpressionMarkersFromFilterWithDefaultFilter() {
        // Given
        String filter = "${*}";

        // When
        Map<String, String> result = JKubeFileInterpolator.getExpressionMarkersFromFilter(filter);

        // Then
        assertThat(result).isEqualTo(Collections.singletonMap("${", "}"));
    }

    @Test
    void testGetExpressionMarkersFromFilterWithSingleCharacterFilter() {
        // Given
        String filter = "@";

        // When
        Map<String, String> result = JKubeFileInterpolator.getExpressionMarkersFromFilter(filter);

        // Then
        assertThat(result).isEqualTo(Collections.singletonMap("@", "@"));
    }

    @Test
    void testGetExpressionMarkersFromFilterWithFalseFilter() {
        // Given
        String filter = "false";

        // When
        Map<String, String> result = JKubeFileInterpolator.getExpressionMarkersFromFilter(filter);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void testGetExpressionMarkersFromFilterWithBlankFilter() {
        // Given
        String filter = "";

        // When
        Map<String, String> result = JKubeFileInterpolator.getExpressionMarkersFromFilter(filter);

        // Then
        assertThat(result).isEmpty();
    }

    @DisplayName("Delimiters Tests")
    @ParameterizedTest(name = "{0}")
    @MethodSource("delimitersTestData")
    void testDelimitersInContext(String testDesc, String src, String key, String value, String expected) {
        // Given
        Properties p = new Properties();
        p.setProperty("test.label", "test");
        // When
        String result = JKubeFileInterpolator.interpolate(src, p, Collections.singletonMap(key, value));
        // Then
        assertThat(result).isEqualTo(expected);
    }

    public static Stream<Arguments> delimitersTestData(){
        return Stream.of(
                Arguments.arguments("Long delimiters in context", "This is a <expression>test.label</expression> for long delimiters in context.", "<expression>", "</expression>", "This is a test for long delimiters in context."),
                Arguments.arguments("Long delimiters with no start context", "<expression>test.label</expression> for long delimiters in context.", "<expression>", "</expression>", "test for long delimiters in context."),
                Arguments.arguments("Long delimiters with no end context", "This is a <expression>test.label</expression>", "<expression>", "</expression>", "This is a test"),
                Arguments.arguments("Long delimiters with no context", "<expression>test.label</expression>", "<expression>", "</expression>", "test"));
    }

    @DisplayName("Substitution Tests")
    @ParameterizedTest(name = "{0}")
    @MethodSource("substitutionTestData")
    void testSubstitution(String testDesc, String key, String value, String line, Map<String, String> expressionMarkers, String expected) {
        // Given
        Properties p = new Properties();
        if (key != null && value != null) {
            p.setProperty(key, value);
        }
        // When
        final String result = JKubeFileInterpolator.interpolate(line, p, expressionMarkers);
        // Then
        assertThat(result).isEqualTo(expected);
    }

    public static Stream<Arguments> substitutionTestData() {
      return Stream.of(
          Arguments.arguments("Simple substitution", "key", "value", "This is a test ${key}.", Collections.singletonMap("${", "}"), "This is a test value."),
          Arguments.arguments("Broken expression leave it alone", "key", "value", "This is a test ${key.", Collections.singletonMap("${", "}"), "This is a test ${key."),
          Arguments.arguments("Not escape with long escape str at start", "key", "value", "@{key} This is a test.", Collections.singletonMap("@{", "}"), "value This is a test."),
          Arguments.arguments("Should resolve by my getVar method", "${var}", "testVar", "this is a ${var}", Collections.singletonMap("${", "}"), "this is a testVar"),
          Arguments.arguments("NPE free", null, null, null, Collections.emptyMap(), null)
      );
    }

}
