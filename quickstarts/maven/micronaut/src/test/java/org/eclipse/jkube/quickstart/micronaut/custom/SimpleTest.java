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
package org.eclipse.jkube.quickstart.micronaut.custom;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

@MicronautTest
class SimpleTest {

    @Inject
    SimpleClient simpleClient;

    @Test
    void testHello() {
        assertEquals(
                "Hello Fred!",
                simpleClient.hello("Fred"));
    }

    @Test
    void testGetRootPath() {
        assertEquals(
                "Hello from Micronaut deployed with JKube!",
                simpleClient.get());
    }
}
