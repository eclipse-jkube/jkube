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

import org.junit.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import static org.eclipse.jkube.kit.common.util.EnvUtil.isWindows;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

public class JKubeFileInterpolatorTest {
    @Test
    public void testLongDelimitersInContext() {
        // Given
        String src = "This is a <expression>test.label</expression> for long delimiters in context.";
        Properties p = new Properties();
        p.setProperty("test.label", "test");

        // When
        String result = JKubeFileInterpolator.interpolate(src, p, Collections.singletonMap("<expression>", "</expression>"));

        // Then
        assertEquals("This is a test for long delimiters in context.", result);
    }

    @Test
    public void testLongDelimitersWithNoStartContext() {
        // Given
        String src = "<expression>test.label</expression> for long delimiters in context.";
        Properties p = new Properties();
        p.setProperty("test.label", "test");

        // When
        String result = JKubeFileInterpolator.interpolate(src, p, Collections.singletonMap("<expression>", "</expression>"));

        // Then
        assertEquals("test for long delimiters in context.", result);
    }

    @Test
    public void testLongDelimitersWithNoEndContext() {
        String src = "This is a <expression>test.label</expression>";

        Properties p = new Properties();
        p.setProperty("test.label", "test");

        // When
        String result = JKubeFileInterpolator.interpolate(src, p, Collections.singletonMap("<expression>", "</expression>"));

        // Then
        assertEquals("This is a test", result);
    }

    @Test
    public void testLongDelimitersWithNoContext() {
        // Given
        String src = "<expression>test.label</expression>";
        Properties p = new Properties();
        p.setProperty("test.label", "test");
        // When
        String result = JKubeFileInterpolator.interpolate(src, p, Collections.singletonMap("<expression>", "</expression>"));

        // Then
        assertEquals("test", result);
    }

    @Test
    public void testSimpleSubstitution() {
        // Given
        Properties p = new Properties();
        p.setProperty("key", "value");

        // When
        String result = JKubeFileInterpolator.interpolate("This is a test ${key}.", p, Collections.singletonMap("${", "}"));

        // Then
        assertEquals("This is a test value.", result);
    }

    @Test
    public void testSimpleSubstitution_TwoExpressions() {
        // Given
        Properties p = new Properties();
        p.setProperty("key", "value");
        p.setProperty("key2", "value2");

        // When
        String result = JKubeFileInterpolator.interpolate("${key}-${key2}", p, Collections.singletonMap("${", "}"));

        // Then
        assertEquals("value-value2", result);
    }

    @Test
    public void testBrokenExpression_LeaveItAlone() {
        // Given
        Properties p = new Properties();
        p.setProperty("key", "value");

        // When
        String result = JKubeFileInterpolator.interpolate("This is a test ${key.", p, Collections.singletonMap("${", "}"));

        // Then
        assertEquals("This is a test ${key.", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testShouldFailOnExpressionCycleParsed() {
        // Given
        Properties props = new Properties();
        props.setProperty("key1", "${key2}");
        props.setProperty("key2", "${key1}");

        // When
        JKubeFileInterpolator.interpolate("${key1}", props, Collections.singletonMap("${", "}"));

        fail("Should detect expression cycle and fail.");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testShouldFailOnExpressionCycleDirect() {
        // Given
        Properties props = new Properties();
        props.setProperty("key1", "key2");
        props.setProperty("key2", "key1");

        // When
        JKubeFileInterpolator.interpolate("${key1}", props, Collections.singletonMap("${", "}"));

        fail("Should detect expression cycle and fail.");
    }

    @Test
    public void testShouldResolveByMy_getVar_Method() {
        // Given
        Properties properties = new Properties();
        properties.put("${var}", "testVar");

        // When
        String result = JKubeFileInterpolator.interpolate("this is a ${var}", properties, Collections.singletonMap("${", "}"));

        // Then
        assertEquals("this is a testVar", result);
    }

    @Test
    public void testShouldResolveByEnvVarInLinux() {
        // Given
        assumeFalse(isWindows());
        Properties p = new Properties();
        String result = JKubeFileInterpolator.interpolate("this is a ${env.HOME} ${env.PATH}", p, Collections.singletonMap("${env.", "}"));

        // When + Then
        assertNotEquals("this is a ${HOME} ${PATH}", result);
        assertNotEquals("this is a ${env.HOME} ${env.PATH}", result);
    }

    @Test
    public void testShouldResolveByEnvVarInWindows() {
        // Given
        assumeTrue(isWindows());
        Properties p = new Properties();
        String result = JKubeFileInterpolator.interpolate("this is a ${env.USERNAME} ${env.OS}", p, Collections.singletonMap("${env.", "}"));

        // When + Then
        assertNotEquals("this is a ${USERNAME} ${OS}", result);
        assertNotEquals("this is a ${env.USERNAME} ${env.OS}", result);
    }

    @Test
    public void testNotEscapeWithLongEscapeStrAtStart() {
        // Given
        Properties p = new Properties();
        p.setProperty("key", "value");

        // When
        String result = JKubeFileInterpolator.interpolate("@{key} This is a test.", p, Collections.singletonMap("@{", "}"));

        // Then
        assertEquals("value This is a test.", result);
    }

    @Test
    public void testNPEFree() {
        // When
        String result = JKubeFileInterpolator.interpolate(null, new Properties(), Collections.emptyMap());

        // Then
        assertNull(result);
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
        String result1 = JKubeFileInterpolator.interpolate(EXPR, p, Collections.singletonMap("${", "}"));
        p.put("test2.label", "zz");
        String result2 = JKubeFileInterpolator.interpolate(EXPR2, p, Collections.singletonMap("${", "}"));

        // Then
        assertEquals("pANDx", result1);
        assertEquals("pzzANDx", result2);
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
        assertEquals("pANDx", JKubeFileInterpolator.interpolate(EXPR, p1,Collections.singletonMap("${", "}")));
        assertEquals("pdominantANDx", JKubeFileInterpolator.interpolate(EXPR2, p1, Collections.singletonMap("${", "}")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCyclesWithLinked() {
        // Given
        Properties p = new Properties();
        p.put("key1", "${key2}");
        p.put("key2", "${key2}");
        // When
        JKubeFileInterpolator.interpolate("${key2}", p, Collections.singletonMap("${", "}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCyclesWithLinked_betweenRootAndOther() {
        // Given
        Properties p = new Properties();
        p.put("key1", "${key2}");
        p.put("key2", "${key1}");
        // When
        JKubeFileInterpolator.interpolate("${key1}", p, Collections.singletonMap("${", "}"));
    }

    @Test
    public void testGetExpressionMarkersFromFilterWithDefaultFilter() {
        // Given
        String filter = "${*}";

        // When
        Map<String, String> result = JKubeFileInterpolator.getExpressionMarkersFromFilter(filter);

        // Then
        assertEquals(Collections.singletonMap("${", "}"), result);
    }

    @Test
    public void testGetExpressionMarkersFromFilterWithSingleCharacterFilter() {
        // Given
        String filter = "@";

        // When
        Map<String, String> result = JKubeFileInterpolator.getExpressionMarkersFromFilter(filter);

        // Then
        assertEquals(Collections.singletonMap("@", "@"), result);
    }

    @Test
    public void testGetExpressionMarkersFromFilterWithFalseFilter() {
        // Given
        String filter = "false";

        // When
        Map<String, String> result = JKubeFileInterpolator.getExpressionMarkersFromFilter(filter);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetExpressionMarkersFromFilterWithBlankFilter() {
        // Given
        String filter = "";

        // When
        Map<String, String> result = JKubeFileInterpolator.getExpressionMarkersFromFilter(filter);

        // Then
        assertTrue(result.isEmpty());
    }
}
