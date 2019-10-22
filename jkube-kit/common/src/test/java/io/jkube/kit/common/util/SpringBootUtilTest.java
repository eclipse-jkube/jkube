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
package io.jkube.kit.common.util;

import java.util.Properties;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Checking the behaviour of utility methods.
 */
public class SpringBootUtilTest {


    @Test
    public void testYamlToPropertiesParsing() {

        Properties props = YamlUtil.getPropertiesFromYamlResource(SpringBootUtilTest.class.getResource("/util/test-application.yml"));
        assertNotEquals(0, props.size());

        assertEquals("8081", props.getProperty("management.port"));
        assertEquals("jdbc:mysql://127.0.0.1:3306", props.getProperty("spring.datasource.url"));
        assertEquals("value0", props.getProperty("example.nested.items[0].value"));
        assertEquals("value1", props.getProperty("example.nested.items[1].value"));
        assertEquals("sub0", props.getProperty("example.nested.items[2].elements[0].element[0].subelement"));
        assertEquals("sub1", props.getProperty("example.nested.items[2].elements[0].element[1].subelement"));

    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidFileThrowsException() {
        YamlUtil.getPropertiesFromYamlResource(SpringBootUtilTest.class.getResource("/util/invalid-application.yml"));
    }

    @Test
    public void testNonExistentYamlToPropertiesParsing() {

        Properties props = YamlUtil.getPropertiesFromYamlResource(SpringBootUtilTest.class.getResource("/this-file-does-not-exist"));
        assertNotNull(props);
        assertEquals(0, props.size());

    }

    @Test
    public void testPropertiesParsing() {

        Properties props = SpringBootUtil.getPropertiesResource(SpringBootUtilTest.class.getResource("/util/test-application.properties"));
        assertNotEquals(0, props.size());

        assertEquals("8081", props.getProperty("management.port"));
        assertEquals("jdbc:mysql://127.0.0.1:3306", props.getProperty("spring.datasource.url"));
        assertEquals("value0", props.getProperty("example.nested.items[0].value"));
        assertEquals("value1", props.getProperty("example.nested.items[1].value"));

    }

    @Test
    public void testNonExistentPropertiesParsing() {

        Properties props = SpringBootUtil.getPropertiesResource(SpringBootUtilTest.class.getResource("/this-file-does-not-exist"));
        assertNotNull(props);
        assertEquals(0, props.size());

    }

}
