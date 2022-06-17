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
package org.eclipse.jkube.kit.common.util;

import java.io.IOException;
import java.net.ServerSocket;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IoUtilTest {

    @Test
    void findOpenPort() throws IOException {
        int port = IoUtil.getFreeRandomPort();
        try (ServerSocket ss = new ServerSocket(port)) {
            assertThat(ss).isNotNull();
        }
    }

    @Test
    void findOpenPortWhenPortsAreBusy() throws IOException {
        int port = IoUtil.getFreeRandomPort(49152, 60000, 100);
        try (ServerSocket ss = new ServerSocket(port)) {
            assertThat(ss).isNotNull();
        }
        int port2 = IoUtil.getFreeRandomPort(port, 65535, 100);
        try (ServerSocket ss = new ServerSocket(port2)) {
            assertThat(ss).isNotNull();
        }
        assertThat(port2 > port).isTrue();
        assertThat(port2).isGreaterThan(port);
    }

    @Test
    void testSanitizeFileName() {
        assertThat(IoUtil.sanitizeFileName(null)).isNull();
        assertThat(IoUtil.sanitizeFileName("Hello/&%World")).isEqualTo("Hello-World");
        assertThat(IoUtil.sanitizeFileName(" _H-.-e-.-l-l--.//o()")).isEqualTo("-H-e-l-l-o-");
        assertThat(IoUtil.sanitizeFileName("s2i-env-docker.io/fabric8/java:latest")).isEqualTo("s2i-env-docker-io-fabric8-java-latest");
    }

}
