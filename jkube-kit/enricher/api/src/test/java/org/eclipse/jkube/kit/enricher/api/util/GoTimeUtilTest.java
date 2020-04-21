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
package org.eclipse.jkube.kit.enricher.api.util;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class GoTimeUtilTest {

    @Test
    public void testConversion() {
        Assert.assertEquals(new Integer(23), GoTimeUtil.durationSeconds("23s"));
        Assert.assertEquals(new Integer(0), GoTimeUtil.durationSeconds("0.5s"));
        Assert.assertEquals(new Integer(0), GoTimeUtil.durationSeconds("3ms"));
        Assert.assertEquals(new Integer(0), GoTimeUtil.durationSeconds("3ns"));
        Assert.assertEquals(new Integer(1), GoTimeUtil.durationSeconds("1002ms"));
        Assert.assertEquals(new Integer(123), GoTimeUtil.durationSeconds("2m3s"));
        Assert.assertEquals(new Integer(3663), GoTimeUtil.durationSeconds("1h1m3s"));
        Assert.assertEquals(new Integer(1810), GoTimeUtil.durationSeconds("0.5h0.1m4s"));
        Assert.assertEquals(new Integer(-15), GoTimeUtil.durationSeconds("-15s"));
        Assert.assertEquals(new Integer(30), GoTimeUtil.durationSeconds("2h-119.5m"));
    }

    @Test
    public void testEmpty() {
        assertNull(GoTimeUtil.durationSeconds(null));
        assertNull(GoTimeUtil.durationSeconds(""));
        assertNull(GoTimeUtil.durationSeconds(" "));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testErrorOverflow() {
        GoTimeUtil.durationSeconds(Integer.MAX_VALUE + "0s");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testErrorNoUnit() {
        GoTimeUtil.durationSeconds("145");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testErrorUnknownUnit() {
        GoTimeUtil.durationSeconds("1w");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testErrorUnparsable() {
        GoTimeUtil.durationSeconds("ms");
    }



}
