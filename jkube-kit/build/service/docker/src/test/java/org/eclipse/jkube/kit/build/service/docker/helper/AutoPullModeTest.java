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
package org.eclipse.jkube.kit.build.service.docker.helper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.kit.build.service.docker.helper.AutoPullMode.ALWAYS;
import static org.eclipse.jkube.kit.build.service.docker.helper.AutoPullMode.OFF;
import static org.eclipse.jkube.kit.build.service.docker.helper.AutoPullMode.ON;
import static org.eclipse.jkube.kit.build.service.docker.helper.AutoPullMode.fromString;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author roland
 * @since 01/03/15
 */
class AutoPullModeTest {

    @ParameterizedTest(name = "AutoPullMode ''{0}'' and from string ''{1}'' should be equal ")
    @MethodSource("data")
    void fromString_withSimpleString(AutoPullMode pullMode, String val) {
        assertThat(fromString(val)).isEqualTo(pullMode);
    }

    public static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of(ON, "on"),
                Arguments.of(ON, "true"),
                Arguments.of(OFF, "Off"),
                Arguments.of(OFF, "falsE"),
                Arguments.of(ALWAYS, "alWays")
        );
    }

    @Test
    void fromString_withUnknownString_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> fromString("unknown"));
    }
}
