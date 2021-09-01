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
package org.eclipse.jkube.kit.common.util;

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UserConfigurationCompareTest {

    @Test
    public void testConfigEqualWhenEqual() {
        //Given
        Object entity1 = "Hello";
        Object entity2 = "Hello";
        //When
        boolean result = UserConfigurationCompare.configEqual(entity1, entity2);
        //Then
        assertTrue(result);
    }

    @Test
    public void testConfigEqualWhenEitherNull() {
        //Given
        Object entity1 = null;
        Object entity2 = "new";
        //When
        boolean result = UserConfigurationCompare.configEqual(entity1, entity2);
        //Then
        assertFalse(result);
    }

    @Test
    public void testConfigEqualWhenMapAndString() {
        //Given
        Object entity1 = Collections.EMPTY_MAP;
        Object entity2 = "Hello";

        //When
        boolean result = UserConfigurationCompare.configEqual(entity1, entity2);
        //Then
        assertFalse(result);
    }

    @Test
    public void testConfigEqualWhenMapAndEqual() {
        //Given
        Map<String, String> entity1 = new HashMap();
        Map<String, String> entity2 = new HashMap();
        entity1.put("item1", "code");
        entity2.put("item1", "code");
        //When
        boolean result = UserConfigurationCompare.configEqual(entity1, entity2);
        //Then
        assertTrue(result);
    }

    @Test
    public void testConfigEqualWhenMapAndNotEqual() {
        //Given
        Map<String, String> entity1 = new HashMap();
        Map<String, String> entity2 = new HashMap();
        entity1.put("item", "code");
        entity2.put("item1", "code");
        //When
        boolean result = UserConfigurationCompare.configEqual(entity1, entity2);
        //Then
        assertFalse(result);
    }

    @Test
    public void testConfigEqualWhenObjectMetaIsTrue() {
        //Given
        Object entity1 = new ObjectMetaBuilder().withName("test1").withAnnotations(Collections.singletonMap("foo", "bar")).build();
        Object entity2 = new ObjectMetaBuilder().withName("test1").withAnnotations(Collections.singletonMap("foo","bar")).build();
        //When
        boolean result = UserConfigurationCompare.configEqual(entity1, entity2);
        //Then
        assertTrue(result);
    }

    @Test
    public void testConfigEqualWhenCollectionWhenFalse(){
        //Given
        ArrayList<String> testList = new ArrayList<>();
        Object entity1 = Collections.singletonList(testList);
        Object entity2 = Collections.emptyList();
        //When
        boolean result = UserConfigurationCompare.configEqual(entity1,entity2);
        //Then
        assertFalse(result);
    }

    @Test
    public void testConfigEqualWhenCollectionWhenTrue(){
        //Given
        Object entity1 = Collections.emptyList();
        Object entity2 = Collections.emptySet();
        //When
        boolean result = UserConfigurationCompare.configEqual(entity1,entity2);
        //Then
        assertTrue(result);
    }


    @Test
    public void testConfigEqualWhenNotEqual() {
        //Given
        Object entity1 = "entity1";
        Object entity2 = "entity2";
        //When
        boolean result = UserConfigurationCompare.configEqual(entity1, entity2);
        //Then
        assertFalse(result);
    }

    @Test
    public void testConfigEqualWhenNotKDTO() {
        //Given
        Object entity1 = new KubernetesListBuilder().addToItems(new DeploymentBuilder().build()); //
        Object entity2 = new KubernetesListBuilder().addToItems(new DeploymentBuilder().build()); //
        //When
        boolean result = UserConfigurationCompare.configEqual(entity1, entity2);
        //Then
        assertTrue(result);
    }
}

