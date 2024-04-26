/*
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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class GoTimeUtilTest {

    @DisplayName("duration conversion")
    @ParameterizedTest(name = "duration ''{0}'' should be ''{1}'' seconds")
    @MethodSource("data")
    void conversion(String duration, int expectedDuration) {
        int result = GoTimeUtil.durationSeconds(duration).get();
        assertThat(result).isEqualTo(expectedDuration);
    }

    static Stream<Arguments> data() {
      return Stream.of(
          arguments("23s", 23),
          arguments("0.5s", 0),
          arguments("3ms", 0),
          arguments("3ns", 0),
          arguments("1002ms", 1),
          arguments("2m3s", 123),
          arguments("1h1m3s", 3663),
          arguments("0.5h0.1m4s", 1810),
          arguments("-15s", -15),
          arguments("2h-119.5m", 30));
    }

    @DisplayName("with duration")
    @ParameterizedTest(name = "''{0}'', should be empty")
    @NullAndEmptySource
    @ValueSource(strings = { " " })
    void durationSeconds(String duration) {
      assertThat(GoTimeUtil.durationSeconds(duration)).isEmpty();
    }

    @DisplayName("with invalid duration")
    @ParameterizedTest(name = "''{1} {0}'' , should throw exception")
    @MethodSource("invalidDurations")
    void durationSeconds_invalid(String description, String duration) {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> GoTimeUtil.durationSeconds(duration));
    }

    static Stream<Arguments> invalidDurations() {
      return Stream.of(
          arguments("overflowing integer", Integer.MAX_VALUE + "0s"),
          arguments("no unit", "145"),
          arguments("unknown unit", "1w"),
          arguments("unparsable unit", "ms"));
    }
}
