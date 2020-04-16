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

import org.junit.Test;

/**
 * Tests the health check configuration
 */
public class HealthCheckConfigTest {

    @Test
    public void testGoodHealthCheck1() {
        HealthCheckConfiguration.builder()
                .cmd(Arguments.builder().shell("exit 0").build())
                .build()
                .validate();
    }

    @Test
    public void testGoodHealthCheck2() {
        HealthCheckConfiguration.builder()
                .cmd(Arguments.builder().shell("exit 0").build())
                .retries(1)
                .build()
                .validate();
    }

    @Test
    public void testGoodHealthCheck3() {
        HealthCheckConfiguration.builder()
                .cmd(Arguments.builder().shell("exit 0").build())
                .retries(1)
                .interval("2s")
                .build()
                .validate();
    }

    @Test
    public void testGoodHealthCheck4() {
        HealthCheckConfiguration.builder()
                .cmd(Arguments.builder().shell("exit 0").build())
                .retries(1)
                .interval("2s")
                .timeout("3s")
                .build()
                .validate();
    }

    @Test
    public void testGoodHealthCheck5() {
        HealthCheckConfiguration.builder()
                .mode(HealthCheckMode.cmd)
                .cmd(Arguments.builder().shell("exit 0").build())
                .retries(1)
                .interval("2s")
                .timeout("3s")
                .startPeriod("30s")
                .build()
                .validate();
    }

    @Test
    public void testGoodHealthCheck6() {
        HealthCheckConfiguration.builder()
                .mode(HealthCheckMode.cmd)
                .cmd(Arguments.builder().shell("exit 0").build())
                .retries(1)
                .interval("2s")
                .timeout("3s")
                .startPeriod("4s")
                .build()
                .validate();
    }

    @Test
    public void testGoodHealthCheck7() {
        HealthCheckConfiguration.builder()
                .mode(HealthCheckMode.none)
                .build()
                .validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadHealthCheck1() {
        HealthCheckConfiguration.builder()
                .mode(HealthCheckMode.none)
                .interval("2s")
                .build()
                .validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadHealthCheck2() {
        HealthCheckConfiguration.builder()
                .mode(HealthCheckMode.none)
                .retries(1)
                .build()
                .validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadHealthCheck3() {
        HealthCheckConfiguration.builder()
                .mode(HealthCheckMode.none)
                .timeout("3s")
                .build()
                .validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadHealthCheck4() {
        HealthCheckConfiguration.builder()
                .mode(HealthCheckMode.none)
                .startPeriod("30s")
                .cmd(Arguments.builder().shell("echo a").build())
                .build()
                .validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadHealthCheck5() {
        HealthCheckConfiguration.builder()
                .mode(HealthCheckMode.none)
                .cmd(Arguments.builder().shell("echo a").build())
                .build()
                .validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadHealthCheck6() {
        HealthCheckConfiguration.builder()
                .build()
                .validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadHealthCheck7() {
        HealthCheckConfiguration.builder()
                .mode(HealthCheckMode.cmd)
                .build()
                .validate();
    }

}
