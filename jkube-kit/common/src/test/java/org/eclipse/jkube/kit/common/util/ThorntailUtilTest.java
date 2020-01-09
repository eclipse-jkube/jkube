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

import java.util.Properties;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ThorntailUtilTest {

    @Test
    public void testReadThorntailPort() {
        Properties props = YamlUtil.getPropertiesFromYamlResource(ThorntailUtilTest.class.getResource("/util/project-default.yml"));
        assertNotNull(props);
        assertEquals("8082", props.getProperty("thorntail.http.port"));

    }

}
