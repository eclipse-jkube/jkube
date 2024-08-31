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
package org.eclipse.jkube.kit.config.image.build;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.eclipse.jkube.kit.config.image.build.CleanupMode.NONE;
import static org.eclipse.jkube.kit.config.image.build.CleanupMode.REMOVE;
import static org.eclipse.jkube.kit.config.image.build.CleanupMode.TRY_TO_REMOVE;

/**
 * @author roland
 * @since 01/03/16
 */
class CleanupModeTest {

    @ParameterizedTest
    @MethodSource("provideParseTestData")
    void parse(String input, CleanupMode expected) {
        assertThat(CleanupMode.parse(input)).isEqualTo(expected);
    }

    static Stream<Arguments> provideParseTestData() {
        return Stream.of(
            Arguments.of(null, CleanupMode.TRY_TO_REMOVE),
            Arguments.of("try", CleanupMode.TRY_TO_REMOVE),
            Arguments.of("FaLsE", CleanupMode.NONE),
            Arguments.of("NONE", CleanupMode.NONE),
            Arguments.of("true", CleanupMode.REMOVE),
            Arguments.of("removE", CleanupMode.REMOVE)
        );
    }

    @Test
    void invalid() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> CleanupMode.parse("blub"))
                .withMessageContainingAll("blub", "try", "none", "remove");
    }
}
