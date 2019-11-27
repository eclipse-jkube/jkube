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
package org.eclipse.jkube.maven.enricher.api.model;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class KindAndNameTest {

    @Test
    public void simpleTest() {

        ConfigMap configMap = new ConfigMapBuilder().withNewMetadata().withName("CMTest").endMetadata().addToData("foo","bar").build();

        KindAndName kindAndName = new KindAndName(configMap);

        assertEquals("ConfigMap",kindAndName.getKind());
        assertEquals("CMTest",kindAndName.getName());
    }

    @Test
    public void equalsTest(){

        KindAndName kindAndName = new KindAndName("kindTest","nameTest");
        KindAndName secondKindAndName = new KindAndName("kindTest","nameTest");
        KindAndName thirdKindAndName = new KindAndName("kindTest1","nameTest1");
        KindAndName fourthKindAndName = new KindAndName("kindTest1","nameTest");
        KindAndName fifthKindAndName = new KindAndName("kindTest","nameTest1");

        //if checking same object
        assertTrue(kindAndName.equals(kindAndName));

        //if one null is passed
        assertFalse(kindAndName.equals(null));

        //if two different are checked with same value
        assertTrue(kindAndName.equals(secondKindAndName));

        //if two different are passsed with different combinations of value
        assertFalse(kindAndName.equals(thirdKindAndName));
        assertFalse(kindAndName.equals(fourthKindAndName));
        assertFalse(kindAndName.equals(fifthKindAndName));
    }

    @Test
    public void testHashCode(){
        KindAndName kindAndName = new KindAndName("kindTest","nameTest");
        KindAndName secondKindAndName = new KindAndName("","");

        assertEquals(1812739127,kindAndName.hashCode());
        assertEquals(0,secondKindAndName.hashCode());
    }
}