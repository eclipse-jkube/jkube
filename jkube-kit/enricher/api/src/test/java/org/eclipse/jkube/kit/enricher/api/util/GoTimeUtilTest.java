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

import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GoTimeUtilTest {

    @Test
    public void testConversion() {
        // Given
        List<String> inputs = Arrays.asList("23s", "0.5s", "3ms", "3ns", "1002ms",
                "2m3s", "1h1m3s", "0.5h0.1m4s", "-15s", "2h-119.5m");
        List<Integer> expectations = Arrays.asList(23, 0, 0, 0, 1, 123, 3663, 1810, -15, 30);

        for (int i = 0; i < inputs.size(); i++) {
            // When
            Optional<Integer> result = GoTimeUtil.durationSeconds(inputs.get(i));

            // Then
            assertTrue(result.isPresent());
            Assertions.assertThat(result.get()).isEqualTo(expectations.get(i).intValue());
        }
    }

    @Test
    public void testEmpty() {
        assertEquals(Optional.empty(), GoTimeUtil.durationSeconds(null));
        assertEquals(Optional.empty(), GoTimeUtil.durationSeconds(""));
        assertEquals(Optional.empty(), GoTimeUtil.durationSeconds(" "));
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
