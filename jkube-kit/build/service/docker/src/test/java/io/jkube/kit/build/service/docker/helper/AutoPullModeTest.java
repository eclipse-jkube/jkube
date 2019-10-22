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
package io.jkube.kit.build.service.docker.helper;

import org.junit.Test;

import static io.jkube.kit.build.service.docker.helper.AutoPullMode.ALWAYS;
import static io.jkube.kit.build.service.docker.helper.AutoPullMode.OFF;
import static io.jkube.kit.build.service.docker.helper.AutoPullMode.ON;
import static io.jkube.kit.build.service.docker.helper.AutoPullMode.fromString;
import static org.junit.Assert.assertEquals;


/**
 * @author roland
 * @since 01/03/15
 */
public class AutoPullModeTest {

    @Test
    public void simple() {
        assertEquals(ON, fromString("on"));
        assertEquals(ON, fromString("true"));
        assertEquals(OFF, fromString("Off"));
        assertEquals(OFF, fromString("falsE"));
        assertEquals(ALWAYS, fromString("alWays"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void unknown() {
        fromString("unknown");
    }
}
