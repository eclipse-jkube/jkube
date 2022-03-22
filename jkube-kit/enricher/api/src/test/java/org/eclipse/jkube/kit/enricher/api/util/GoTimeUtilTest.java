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

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class GoTimeUtilTest {

    @Test
    public void testConversion() {
        // Given
        List<String> inputs = Arrays.asList("23s", "0.5s", "3ms", "3ns", "1002ms",
                "2m3s", "1h1m3s", "0.5h0.1m4s", "-15s", "2h-119.5m");
        List<Integer> expectations = Arrays.asList(23, 0, 0, 0, 1, 123, 3663, 1810, -15, 30);

        for (int i = 0; i < inputs.size(); i++) {
            assertThat(GoTimeUtil.durationSeconds(inputs.get(i)))
                    .contains(expectations.get(i));
        }
    }

    @Test
    public void testNull() {
        assertThat(GoTimeUtil.durationSeconds(null)).isEmpty();
    }

    @Test
    public void testEmpty() {
        assertThat(Optional.empty())
                .isEqualTo(GoTimeUtil.durationSeconds(""));
    }

    @Test
    public void testBlankSpace() {
        assertThat(Optional.empty())
                .isEqualTo(GoTimeUtil.durationSeconds(" "));
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
