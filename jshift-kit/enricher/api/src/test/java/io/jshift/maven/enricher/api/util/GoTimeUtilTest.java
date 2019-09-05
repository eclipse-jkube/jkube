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
package io.jshift.maven.enricher.api.util;

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
