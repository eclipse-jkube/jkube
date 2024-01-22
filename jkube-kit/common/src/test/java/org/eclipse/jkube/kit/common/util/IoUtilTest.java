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
package org.eclipse.jkube.kit.common.util;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URL;

import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.TestHttpStaticServer;
import org.eclipse.jkube.kit.common.assertj.FileAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.apache.commons.io.FilenameUtils.separatorsToSystem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class IoUtilTest {
    @TempDir
    private File temporaryFolder;

    private KitLogger kitLogger;

    @BeforeEach
    void setUp() {
        kitLogger = spy(new KitLogger.SilentLogger());
    }

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
        assertThat(port2).isGreaterThan(port);
    }

    @Test
    void findOpenPortWithSmallAttemptsCount() throws IOException {
        int port = IoUtil.getFreeRandomPort(30000, 60000, 30);
        try (ServerSocket ss = new ServerSocket(port)) {
            assertThat(ss).isNotNull();
        }
    }

    @Test
    void findOpenPortWithLargeAttemptsCount() throws IOException {
        int port = IoUtil.getFreeRandomPort(30000, 60000, 1000);
        try (ServerSocket ss = new ServerSocket(port)) {
            assertThat(ss).isNotNull();
        }
    }

    @Test
    void invokeExceptionWhenCouldntFindPort() throws IOException {

        // find an open port to occupy
        int foundPort = IoUtil.getFreeRandomPort(30000, 65000, 1000);

        // use port
        try (ServerSocket ignored = new ServerSocket(foundPort)) {
            String expectedMessage = "Cannot find a free random port in the range [" + foundPort + ", " + foundPort + "] after 3 attempts";

            // try to use the used port
            assertThatThrownBy(() -> IoUtil.getFreeRandomPort(foundPort, foundPort, 3))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(expectedMessage);
        }

    }

    @Test
    void testSanitizeFileName() {
        //noinspection ConstantConditions
        assertThat(IoUtil.sanitizeFileName(null)).isNull();
        assertThat(IoUtil.sanitizeFileName("Hello/&%World")).isEqualTo("Hello-World");
        assertThat(IoUtil.sanitizeFileName(" _H-.-e-.-l-l--.//o()")).isEqualTo("-H-e-l-l-o-");
        assertThat(IoUtil.sanitizeFileName("s2i-env-docker.io/fabric8/java:latest")).isEqualTo("s2i-env-docker-io-fabric8-java-latest");
    }

    @Test
    void download_whenRemoteFragmentProvided_thenDownloadToSpecifiedDir() throws IOException {
        File remoteDirectory = new File(getClass().getResource("/remote-resources").getFile());
        try (TestHttpStaticServer http = new TestHttpStaticServer(remoteDirectory)) {
            // Given
            URL downloadUrl = new URL(String.format("http://localhost:%d/deployment.yaml", http.getPort()));

            // When
            IoUtil.download(kitLogger, downloadUrl, new File(temporaryFolder, "deployment.yaml"));

            // Then
            verify(kitLogger).progressStart();
            verify(kitLogger).progressFinished();
            FileAssertions.assertThat(temporaryFolder)
                .exists()
                .fileTree()
                .containsExactlyInAnyOrder("deployment.yaml");
        }
    }

    @Test
    void downloadArchive_whenUnixArtifactProvided_thenDownloadAndExtract() throws IOException {
        File remoteDirectory = new File(getClass().getResource("/downloadable-artifacts").getFile());
        try (TestHttpStaticServer http = new TestHttpStaticServer(remoteDirectory)) {
            // Given
            URL downloadUrl = new URL(String.format("http://localhost:%d/foo-v0.0.1-linux.tgz", http.getPort()));

            // When
            IoUtil.downloadArchive(downloadUrl, temporaryFolder);

            // Then
            FileAssertions.assertThat(temporaryFolder)
                .exists()
                .fileTree()
                    .containsExactlyInAnyOrder("linux-amd64", separatorsToSystem("linux-amd64/foo"));
        }
    }

    @Test
    void downloadArchive_whenZipArtifactProvided_thenDownloadAndExtract() throws IOException {
        File remoteDirectory = new File(getClass().getResource("/downloadable-artifacts").getFile());
        try (TestHttpStaticServer http = new TestHttpStaticServer(remoteDirectory)) {
            // Given
            URL downloadUrl = new URL(String.format("http://localhost:%d/foo-v0.0.1-windows.zip", http.getPort()));

            // When
            IoUtil.downloadArchive(downloadUrl, temporaryFolder);

            // Then
            FileAssertions.assertThat(temporaryFolder)
                .exists()
                .fileTree()
                .containsExactlyInAnyOrder("foo.exe");
        }
    }

    @Test
    void downloadArchive_whenArtifactNotAvailable_thenThrowException() throws IOException {
        File remoteDirectory = new File(getClass().getResource("/downloadable-artifacts").getFile());
        try (TestHttpStaticServer http = new TestHttpStaticServer(remoteDirectory)) {
            // Given
            URL downloadUrl = new URL(String.format("http://localhost:%d/idontexist-v0.0.1-linux.tgz", http.getPort()));

            // When + Then
            assertThatIOException()
                .isThrownBy(() -> IoUtil.downloadArchive(downloadUrl, temporaryFolder))
                .withMessageContaining("Got (404) while downloading from URL ");
        }
    }

}
