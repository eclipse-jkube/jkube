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
package org.eclipse.jkube.kit.enricher.api.model;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class KindAndNameTest {

    @Test
    public void simpleTest() {

        ConfigMap configMap = new ConfigMapBuilder().withNewMetadata().withName("CMTest").endMetadata().addToData("foo","bar").build();

        KindAndName kindAndName = new KindAndName(configMap);

        assertThat(kindAndName.getKind()).isEqualTo("ConfigMap");
        assertThat(kindAndName.getName()).isEqualTo("CMTest");
    }

    @Test
    public void equalsTest(){

        KindAndName kindAndName = new KindAndName("kindTest","nameTest");
        KindAndName thirdKindAndName = new KindAndName("kindTest1","nameTest1");
        KindAndName fourthKindAndName = new KindAndName("kindTest1","nameTest");
        KindAndName fifthKindAndName = new KindAndName("kindTest","nameTest1");

        //if checking same object
        assertThat(kindAndName).isEqualTo(kindAndName).isNotNull().isEqualTo(kindAndName);

        //if two different are passsed with different combinations of value
        assertThat(thirdKindAndName).isNotEqualTo(kindAndName);
        assertThat(fourthKindAndName).isNotEqualTo(kindAndName);
        assertThat(fifthKindAndName).isNotEqualTo(kindAndName);
    }

    @Test
    public void testHashCode(){
        KindAndName kindAndName = new KindAndName("kindTest","nameTest");
        KindAndName secondKindAndName = new KindAndName("","");

        assertThat(kindAndName.hashCode()).isEqualTo(1812739127);
        assertThat(secondKindAndName.hashCode()).isZero();
    }
}