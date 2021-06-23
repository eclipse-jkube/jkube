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
package org.eclipse.jkube.kit.build.api.helper;

import org.junit.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

public class JKubeDockerfileInterpolatorTest {
    @Test
    public void testLongDelimitersInContext() {
        // Given
        String src = "This is a <expression>test.label</expression> for long delimiters in context.";
        Properties p = new Properties();
        p.setProperty("test.label", "test");

        // When
        String result = JKubeDockerfileInterpolator.interpolate(src, p, Collections.singletonMap("<expression>", "</expression>"));

        // Then
        assertThat(result).isEqualTo("This is a test for long delimiters in context.");
    }

    @Test
    public void testLongDelimitersWithNoStartContext() {
        // Given
        String src = "<expression>test.label</expression> for long delimiters in context.";
        Properties p = new Properties();
        p.setProperty("test.label", "test");

        // When
        String result = JKubeDockerfileInterpolator.interpolate(src, p, Collections.singletonMap("<expression>", "</expression>"));

        // Then
        assertThat(result).isEqualTo("test for long delimiters in context.");
    }

    @Test
    public void testLongDelimitersWithNoEndContext() {
        String src = "This is a <expression>test.label</expression>";

        Properties p = new Properties();
        p.setProperty("test.label", "test");

        // When
        String result = JKubeDockerfileInterpolator.interpolate(src, p, Collections.singletonMap("<expression>", "</expression>"));

        // Then
        assertThat(result).isEqualTo("This is a test");
    }

    @Test
    public void testLongDelimitersWithNoContext() {
        // Given
        String src = "<expression>test.label</expression>";
        Properties p = new Properties();
        p.setProperty("test.label", "test");
        // When
        String result = JKubeDockerfileInterpolator.interpolate(src, p, Collections.singletonMap("<expression>", "</expression>"));

        // Then
        assertThat(result).isEqualTo("test");
    }

    @Test
    public void testSimpleSubstitution() {
        // Given
        Properties p = new Properties();
        p.setProperty("key", "value");

        // When
        String result = JKubeDockerfileInterpolator.interpolate("This is a test ${key}.", p, Collections.singletonMap("${", "}"));

        // Then
        assertThat(result).isEqualTo("This is a test value.");
    }

    @Test
    public void testSimpleSubstitution_TwoExpressions() {
        // Given
        Properties p = new Properties();
        p.setProperty("key", "value");
        p.setProperty("key2", "value2");

        // When
        String result = JKubeDockerfileInterpolator.interpolate("${key}-${key2}", p, Collections.singletonMap("${", "}"));

        // Then
        assertThat(result).isEqualTo("value-value2");
    }

    @Test
    public void testBrokenExpression_LeaveItAlone() {
        // Given
        Properties p = new Properties();
        p.setProperty("key", "value");

        // When
        String result = JKubeDockerfileInterpolator.interpolate("This is a test ${key.", p, Collections.singletonMap("${", "}"));

        // Then
        assertThat(result).isEqualTo("This is a test ${key.");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testShouldFailOnExpressionCycleParsed() {
        // Given
        Properties props = new Properties();
        props.setProperty("key1", "${key2}");
        props.setProperty("key2", "${key1}");

        // When
        JKubeDockerfileInterpolator.interpolate("${key1}", props, Collections.singletonMap("${", "}"));

        fail("Should detect expression cycle and fail.");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testShouldFailOnExpressionCycleDirect() {
        // Given
        Properties props = new Properties();
        props.setProperty("key1", "key2");
        props.setProperty("key2", "key1");

        // When
        JKubeDockerfileInterpolator.interpolate("${key1}", props, Collections.singletonMap("${", "}"));

        fail("Should detect expression cycle and fail.");
    }

    @Test
    public void testShouldResolveByMy_getVar_Method() {
        // Given
        Properties properties = new Properties();
        properties.put("${var}", "testVar");

        // When
        String result = JKubeDockerfileInterpolator.interpolate("this is a ${var}", properties, Collections.singletonMap("${", "}"));

        // Then
        assertThat(result).isEqualTo("this is a testVar");
    }

    @Test
    public void testShouldResolveByEnvar() {
        // Given
        Properties p = new Properties();
        String result = JKubeDockerfileInterpolator.interpolate("this is a ${env.HOME} ${env.PATH}", p, Collections.singletonMap("${env.", "}"));

        // When + Then
        assertThat(result).isNotEqualTo("this is a ${HOME} ${PATH}");
        assertThat(result).isNotEqualTo("this is a ${env.HOME} ${env.PATH}");
    }

    @Test
    public void testNotEscapeWithLongEscapeStrAtStart() {
        // Given
        Properties p = new Properties();
        p.setProperty("key", "value");

        // When
        String result = JKubeDockerfileInterpolator.interpolate("@{key} This is a test.", p, Collections.singletonMap("@{", "}"));

        // Then
        assertThat(result).isEqualTo("value This is a test.");
    }

    @Test
    public void testNPEFree() {
        // When
        String result = JKubeDockerfileInterpolator.interpolate(null, new Properties(), Collections.emptyMap());

        // Then
        assertThat(result).isNull();
    }

    @Test
    public void testLinkedInterpolators() {
        // Given
        final String EXPR = "${test.label}AND${test2}";
        final String EXPR2 = "${test.label}${test2.label}AND${test2}";
        Properties p = new Properties();
        p.put("test.label", "p");
        p.put("test2", "x");

        // When
        String result1 = JKubeDockerfileInterpolator.interpolate(EXPR, p, Collections.singletonMap("${", "}"));
        p.put("test2.label", "zz");
        String result2 = JKubeDockerfileInterpolator.interpolate(EXPR2, p, Collections.singletonMap("${", "}"));

        // Then
        assertThat(result1).isEqualTo("pANDx");
        assertThat(result2).isEqualTo("pzzANDx");
    }

    @Test
    public void testDominance() {
        // Given
        final String EXPR = "${test.label}AND${test2}";
        final String EXPR2 = "${test.label}${test2.label}AND${test2}";
        Properties p1 = new Properties();
        p1.put("test.label", "p");
        p1.put("test2", "x");
        p1.put("test2.label", "dominant");

        // When + Then
        assertThat(JKubeDockerfileInterpolator.interpolate(EXPR, p1,Collections.singletonMap("${", "}"))).isEqualTo("pANDx");
        assertThat(JKubeDockerfileInterpolator.interpolate(EXPR2, p1, Collections.singletonMap("${", "}"))).isEqualTo("pdominantANDx");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCyclesWithLinked() {
        // Given
        Properties p = new Properties();
        p.put("key1", "${key2}");
        p.put("key2", "${key2}");
        // When
        JKubeDockerfileInterpolator.interpolate("${key2}", p, Collections.singletonMap("${", "}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCyclesWithLinked_betweenRootAndOther() {
        // Given
        Properties p = new Properties();
        p.put("key1", "${key2}");
        p.put("key2", "${key1}");
        // When
        JKubeDockerfileInterpolator.interpolate("${key1}", p, Collections.singletonMap("${", "}"));
    }

    @Test
    public void testGetExpressionMarkersFromFilterWithDefaultFilter() {
        // Given
        String filter = "${*}";

        // When
        Map<String, String> result = JKubeDockerfileInterpolator.getExpressionMarkersFromFilter(filter);

        // Then
        assertThat(result).isEqualTo(Collections.singletonMap("${", "}"));
    }

    @Test
    public void testGetExpressionMarkersFromFilterWithSingleCharacterFilter() {
        // Given
        String filter = "@";

        // When
        Map<String, String> result = JKubeDockerfileInterpolator.getExpressionMarkersFromFilter(filter);

        // Then
        assertThat(result).isEqualTo(Collections.singletonMap("@", "@"));
    }

    @Test
    public void testGetExpressionMarkersFromFilterWithFalseFilter() {
        // Given
        String filter = "false";

        // When
        Map<String, String> result = JKubeDockerfileInterpolator.getExpressionMarkersFromFilter(filter);

        // Then
        assertThat(result.isEmpty());
    }

    @Test
    public void testGetExpressionMarkersFromFilterWithBlankFilter() {
        // Given
        String filter = "";

        // When
        Map<String, String> result = JKubeDockerfileInterpolator.getExpressionMarkersFromFilter(filter);

        // Then
        assertThat(result.isEmpty());
    }
}
