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
package org.eclipse.jkube.kit.build.service.docker.access.hc.unix;

import jnr.unixsocket.UnixSocketChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.net.SocketException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

class UnixSocketTest {
    private static final int SO_TIMEOUT = 60;
    private static final boolean KEEP_ALIVE = true;
    private MockedStatic<UnixSocketChannel> unixSocketChannel;
    private jnr.unixsocket.UnixSocket socket;

    @BeforeEach
    void setUp() {
        UnixSocketChannel socketChannel = mock(UnixSocketChannel.class);
        socket = mock(jnr.unixsocket.UnixSocket.class);
        doReturn(socket).when(socketChannel).socket();
        unixSocketChannel = mockStatic(UnixSocketChannel.class);
        unixSocketChannel.when(UnixSocketChannel::open).thenReturn(socketChannel);
    }

    @AfterEach
    void close() {
        unixSocketChannel.close();
    }

    @Test
    void shouldReturnValuesFromSocket() throws IOException {
        // GIVEN
        UnixSocket unixSocket = new UnixSocket();
        doReturn(KEEP_ALIVE).when(socket).getKeepAlive();
        doReturn(SO_TIMEOUT).when(socket).getSoTimeout();

        // WHEN
        boolean keepAlive = unixSocket.getKeepAlive();
        int soTimeout = unixSocket.getSoTimeout();

        // THEN
        assertThat(keepAlive).isEqualTo(KEEP_ALIVE);
        assertThat(soTimeout).isEqualTo(SO_TIMEOUT);
    }

    @Test
    void shouldPassExceptionsFromSocket() throws IOException {
        // GIVEN
        UnixSocket unixSocket = new UnixSocket();
        doThrow(new SocketException()).when(socket).getKeepAlive();
        doThrow(new SocketException()).when(socket).getSoTimeout();

        // WHEN & THEN
        assertThatThrownBy(unixSocket::getKeepAlive).isInstanceOf(SocketException.class);
        assertThatThrownBy(unixSocket::getSoTimeout).isInstanceOf(SocketException.class);
    }

    @Test
    void shouldForwardValuesToSocket() throws IOException {
        // GIVEN
        UnixSocket unixSocket = new UnixSocket();

        // WHEN
        unixSocket.setKeepAlive(KEEP_ALIVE);
        unixSocket.setSoTimeout(SO_TIMEOUT);

        // THEN
        verify(socket).setKeepAlive(KEEP_ALIVE);
        verify(socket).setSoTimeout(SO_TIMEOUT);
    }
}