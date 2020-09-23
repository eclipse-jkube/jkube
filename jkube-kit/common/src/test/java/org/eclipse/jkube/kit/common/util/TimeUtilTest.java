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

import org.eclipse.jkube.kit.common.TimeUtil;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertTrue;

public class TimeUtilTest {
    @Test
    public void testWaitUntilCondition() {
        long timeBeforeWait = System.currentTimeMillis();
        AtomicBoolean value = new AtomicBoolean(false);
        new Thread(() -> value.set(true)).start();

        TimeUtil.waitUntilCondition(value::get, 200);
        long timeAfterWait = System.currentTimeMillis();
        assertTrue(timeAfterWait - timeBeforeWait < 200);
    }
}
