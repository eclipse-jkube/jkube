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
package org.eclipse.jkube.kit.config.image.build;


import java.util.stream.Stream;

import org.eclipse.jkube.kit.common.Arguments;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests the health check configuration
 */

class HealthCheckConfigTest {

    @DisplayName("Good Health Check Tests")
    @ParameterizedTest(name = "{0}")
    @MethodSource("goodHealthCheckTestData")
    void testGoodHealthCheck(String testDesc, Integer retries, String interval, String timeout, String startPeriod) {
        HealthCheckConfiguration.builder()
                .mode(HealthCheckMode.cmd)
                .cmd(Arguments.builder().shell("exit 0").build())
                .retries(retries)
                .interval(interval)
                .timeout(timeout)
                .startPeriod(startPeriod)
                .build()
                .validate();
    }

    public static Stream<org.junit.jupiter.params.provider.Arguments> goodHealthCheckTestData() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.arguments("Good Health Check 1", null, null, null, null),
                org.junit.jupiter.params.provider.Arguments.arguments("Good Health Check 2", 1, null, null, null),
                org.junit.jupiter.params.provider.Arguments.arguments("Good Health Check 3", 1, "2s", null, null),
                org.junit.jupiter.params.provider.Arguments.arguments("Good Health Check 4", 1, "2s", "3s", null),
                org.junit.jupiter.params.provider.Arguments.arguments("Good Health Check 5", 1, "2s", "3s", "30s"),
                org.junit.jupiter.params.provider.Arguments.arguments("Good Health Check 6", 1, "2s", "3s", "4s")
        );
    }

    @Test
    void testGoodHealthCheck7() {
        HealthCheckConfiguration.builder()
                .mode(HealthCheckMode.none)
                .build()
                .validate();
    }
    
    @DisplayName("Bad Health Check Tests")
    @ParameterizedTest(name = "{0}")
    @MethodSource("badHealthCheckTestData")
    void testBadHealthCheck(String testDesc, String cmd, Integer retries, String interval, String timeout, String startPeriod) {
        HealthCheckConfiguration healthCheckConfiguration = HealthCheckConfiguration.builder()
                .mode(HealthCheckMode.none)
                .cmd(Arguments.builder().shell(cmd).build())
                .retries(retries)
                .interval(interval)
                .timeout(timeout)
                .startPeriod(startPeriod)
                .build();
        assertThatThrownBy(healthCheckConfiguration::validate)
                .isInstanceOf(IllegalArgumentException.class);
    }

    public static Stream<org.junit.jupiter.params.provider.Arguments> badHealthCheckTestData() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.arguments("Bad Health Check 1", null, null, "2s", null, null),
                org.junit.jupiter.params.provider.Arguments.arguments("Bad Health Check 2", null, 1, null, null, null),
                org.junit.jupiter.params.provider.Arguments.arguments("Bad Health Check 3", null, null, null, "3s", null),
                org.junit.jupiter.params.provider.Arguments.arguments("Bad Health Check 4", "echo a", null, null, null, "30s"),
                org.junit.jupiter.params.provider.Arguments.arguments("Bad Health Check 5", "echo a", null, null, null, null)
        );
    }

    @Test
    void testBadHealthCheck7() {
        HealthCheckConfiguration healthCheckConfiguration = HealthCheckConfiguration.builder()
                .mode(HealthCheckMode.cmd)
                .build();
        assertThatThrownBy(healthCheckConfiguration::validate)
                .isInstanceOf(IllegalArgumentException.class);

    }

}
