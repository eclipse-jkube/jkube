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
package io.jkube.kit.config.image.build;

import org.junit.Test;

import static io.jkube.kit.config.image.build.CleanupMode.NONE;
import static io.jkube.kit.config.image.build.CleanupMode.REMOVE;
import static io.jkube.kit.config.image.build.CleanupMode.TRY_TO_REMOVE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author roland
 * @since 01/03/16
 */
public class CleanupModeTest {

    @Test
    public void parse() {

        Object[] data = {
            null, TRY_TO_REMOVE,
            "try", TRY_TO_REMOVE,
            "FaLsE", NONE,
            "NONE", NONE,
            "true", REMOVE,
            "removE", REMOVE
        };

        for (int i = 0; i < data.length; i += 2) {
            assertEquals(data[i+1], CleanupMode.parse((String) data[i]));
        }
    }

    @Test
    public void invalid() {
        try {
            CleanupMode.parse("blub");
            fail();
        } catch (IllegalArgumentException exp) {
            assertTrue(exp.getMessage().contains("blub"));
            assertTrue(exp.getMessage().contains("try"));
            assertTrue(exp.getMessage().contains("none"));
            assertTrue(exp.getMessage().contains("remove"));
        }
    }
}
