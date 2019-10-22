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
package io.jkube.kit.common;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class ConfigsTest {

    public static final String KEY_1 = "key1";
    public static final String KEY_2 = "key2";
    public static final String KEY_3 = "key3";

    String value="value";

    //@Mocked can't mock properties anymore in recent jmocckit
    Properties properties = new Properties();

    @Test
    public void getIntValueTest(){
        int result = Configs.asInt("85");
        assertEquals(85,result);

        result = Configs.asInt(null);
        assertEquals(0,result);

        try{
            result = Configs.asInt("parse");
        }
        catch (Exception e){
            assertEquals("For input string: \"parse\"",e.getMessage());
        }
    }

    @Test
    public void getBooleanValueTest(){
        boolean result = Configs.asBoolean("85");
        assertEquals(false,result);

        result = Configs.asBoolean(null);
        assertEquals(false,result);

        result = Configs.asBoolean("false");
        assertEquals(false,result);

        result = Configs.asBoolean("true");
        assertEquals(true,result);

        result = Configs.asBoolean("0");
        assertEquals(false,result);

        result = Configs.asBoolean("1");
        assertEquals(false,result);

    }

    @Test
    public void getStringValueTest(){
        String test = RandomStringUtils.randomAlphabetic(10);
        assertEquals(test,Configs.asString(test));
    }

    @Test
    public void getPropertyValueTest(){
        properties.setProperty(KEY_1, value);
        System.setProperty(KEY_2, value);

        assertEquals("value",Configs.getSystemPropertyWithMavenPropertyAsFallback(properties, KEY_1));
        assertEquals("value",Configs.getSystemPropertyWithMavenPropertyAsFallback(properties, KEY_2));
        assertEquals(null,Configs.getSystemPropertyWithMavenPropertyAsFallback(properties, KEY_3));
    }
}
