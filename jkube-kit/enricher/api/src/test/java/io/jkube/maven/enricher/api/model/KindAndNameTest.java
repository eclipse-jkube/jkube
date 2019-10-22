/**
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.jkube.maven.enricher.api.model;

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