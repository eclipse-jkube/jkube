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
package org.eclipse.jkube.kit.service.buildpacks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.eclipse.jkube.kit.common.KitLogger;

import org.eclipse.jkube.kit.common.TestHttpBuildPacksArtifactsServer;
import org.eclipse.jkube.kit.common.util.EnvUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

abstract class AbstractBuildPackCliDownloaderTest {
  private static final String TEST_PACK_VERSION = "0.32.1";
  private KitLogger kitLogger;
  @TempDir
  private File temporaryFolder;
  private File oldPackCliInJKubeDir;
  private TestHttpBuildPacksArtifactsServer server;
  private BuildPackCliDownloader buildPackCliDownloader;
  private Properties packProperties;

  abstract String getApplicablePackBinary();
  abstract String getInvalidApplicablePackBinary();
  abstract String getPlatform();
  abstract String getProcessorArchitecture();

  @BeforeEach
  void setUp() {
    kitLogger = spy(new KitLogger.SilentLogger());
    Map<String, String> overriddenSystemProperties = new HashMap<>();
    Map<String, String> overriddenEnvironmentVariables = new HashMap<>();
    overriddenSystemProperties.put("user.home", temporaryFolder.getAbsolutePath());
    overriddenSystemProperties.put("os.name", getPlatform());
    overriddenSystemProperties.put("os.arch", getProcessorArchitecture());
    overriddenEnvironmentVariables.put("HOME", temporaryFolder.getAbsolutePath());
    overriddenEnvironmentVariables.put("PATH", temporaryFolder.toPath().resolve("bin").toFile().getAbsolutePath());
    EnvUtil.overridePropertyGetter(overriddenSystemProperties::get);
    EnvUtil.overrideEnvGetter(overriddenEnvironmentVariables::get);
    server = new TestHttpBuildPacksArtifactsServer();
    packProperties = new Properties();
    packProperties.put("version", TEST_PACK_VERSION);
    packProperties.put("linux.artifact", server.getLinuxArtifactUrl());
    packProperties.put("linux-arm64.artifact", server.getLinuxArm64ArtifactUrl());
    packProperties.put("macos.artifact", server.getMacosArtifactUrl());
    packProperties.put("macos-arm64.artifact", server.getMacosArm64ArtifactUrl());
    packProperties.put("windows.artifact", server.getWindowsArtifactUrl());
    packProperties.put("windows.binary-extension", "bat");
    buildPackCliDownloader = new BuildPackCliDownloader(kitLogger, packProperties);
  }

  @AfterEach
  void tearDown() throws IOException {
    server.close();
    EnvUtil.overridePropertyGetter(System::getProperty);
    EnvUtil.overrideEnvGetter(System::getenv);
  }

  @Test
  @DisplayName("pack binary exists and is valid, then use already downloaded cli")
  void givenOldPackCliExistsAndIsValid_thenAlreadyExistingPackCliReturned() throws IOException {
    // Given
    oldPackCliInJKubeDir = new File(Objects.requireNonNull(getClass().getResource(String.format("/%s", getApplicablePackBinary()))).getFile());
    givenPackCliAlreadyDownloaded();

    // When
    File downloadedCli = buildPackCliDownloader.getPackCLIIfPresentOrDownload();

    // Then
    assertThat(downloadedCli.toPath()).isEqualTo(temporaryFolder.toPath().resolve(".jkube").resolve(getApplicablePackBinary()));
    assertThat(downloadedCli).hasSameTextualContentAs(oldPackCliInJKubeDir);
  }

  @SuppressWarnings("unused")
  @Nested
  @DisplayName("pack binary doesn't exist")
  class PackBinaryDoesNotExist {
    @Nested
    @DisplayName("download initiated due to no existing pack binary")
    class DownloadDueToNoExistingPackBinary extends PackBinaryDownload { }
  }

  @Nested
  @DisplayName("pack binary exists in .jkube but is not valid")
  class PackBinaryExistsButIsNotValid {
    @BeforeEach
    void setUp() throws IOException {
      oldPackCliInJKubeDir = new File(Objects.requireNonNull(getClass().getResource(String.format("/%s", getInvalidApplicablePackBinary()))).getFile());
      givenPackCliAlreadyDownloaded();
    }

    @SuppressWarnings("unused")
    @Nested
    @DisplayName("download initiated due to invalid existing pack binary")
    class DownloadDueToInvalidPackBinary extends PackBinaryDownload { }
  }

  private abstract class PackBinaryDownload {
    @Nested
    @DisplayName("download succeeds")
    class DownloadSucceeds {
      private File pack;

      @BeforeEach
      void download() {
        pack = buildPackCliDownloader.getPackCLIIfPresentOrDownload();
      }

      @Test
      @DisplayName("copy downloaded binary to user's .jkube folder and return path")
      void jKubeDirPathIsReturned() {
        assertThat(pack.toPath())
            .isEqualTo(temporaryFolder.toPath().resolve(".jkube").resolve(getApplicablePackBinary()));
      }

      @Test
      @DisplayName("copied downloaded binary exists and has the right size")
      void fileExistsAndHasTheRightSize() {
        assertThat(pack)
            .exists()
            .satisfies(p -> assertThat(p).isNotEmpty());
      }
    }

    @Nested
    @DisplayName("download fails")
    class DownloadFails {
      @BeforeEach
      void setUp() {
        packProperties.put("linux.artifact", server.getBaseUrl() + "invalid-artifacts/pack-v" + TEST_PACK_VERSION + "-linux.tgz");
        packProperties.put("linux-arm64.artifact", server.getBaseUrl() +  "invalid-artifacts/pack-v" + TEST_PACK_VERSION + "-linux-arm64.tgz");
        packProperties.put("macos.artifact", server.getBaseUrl() +  "invalid-artifacts/pack-v" + TEST_PACK_VERSION + "-macos.tgz");
        packProperties.put("macos-arm64.artifact", server.getBaseUrl() +  "invalid-artifacts/pack-v" + TEST_PACK_VERSION + "-macos-arm64.tgz");
        packProperties.put("windows.artifact", server.getBaseUrl() +  "invalid-artifacts/pack-v" + TEST_PACK_VERSION + "-windows.zip");
      }

      @Test
      @DisplayName("warning is logged indicating that pack binary download failed")
      void logWarningAboutDownloadFailure() {
        // Given + When
        assertThatIllegalStateException().isThrownBy(buildPackCliDownloader::getPackCLIIfPresentOrDownload);

        // Then
        ArgumentCaptor<String> downloadFailureMessage = ArgumentCaptor.forClass(String.class);
        verify(kitLogger).warn(downloadFailureMessage.capture());
        assertThat(downloadFailureMessage.getValue()).contains("Not able to download pack CLI : ");
      }

      @Test
      @DisplayName("info is logged indicating we attempt to use a local pack binary as a fallback")
      void logInfoIndicatingFallbackToLocalPackCli() {
        // Given + When
        assertThatIllegalStateException().isThrownBy(buildPackCliDownloader::getPackCLIIfPresentOrDownload);

        // Then
        verify(kitLogger).info("Checking for local pack CLI");
      }

      @Test
      @DisplayName("local pack binary doesn't exist, then throw exception")
      void givenNoLocalPackBinaryInUserPath_thenThrowException() {
        // When + Then
        assertThatIllegalStateException()
            .isThrownBy(buildPackCliDownloader::getPackCLIIfPresentOrDownload)
            .withMessage("No local pack binary found");
      }

      @Test
      @DisplayName("local pack binary exists and is valid, then return local pack binary path")
      void givenLocalPackCliExistsAndIsValid_thenReturnPathToLocalPackBinary() throws IOException {
        // Given
        givenPackCliPresentOnUserPath(String.format("/%s", getApplicablePackBinary()));

        // When
        File downloadedCli = buildPackCliDownloader.getPackCLIIfPresentOrDownload();

        // Then
        assertThat(downloadedCli).isEqualTo(temporaryFolder.toPath().resolve("bin").resolve(getApplicablePackBinary()).toFile());
      }

      @Test
      @DisplayName("local pack binary exists but invalid, then throw exception")
      void localPackCliCorrupt_thenThrowException() throws IOException {
        // Given
        givenPackCliPresentOnUserPath(String.format("/%s", getInvalidApplicablePackBinary()));

        // When + Then
        assertThatIllegalStateException()
            .isThrownBy(buildPackCliDownloader::getPackCLIIfPresentOrDownload)
            .withMessage("No local pack binary found");
      }

      private void givenPackCliPresentOnUserPath(String packResource) throws IOException {
        File bin = new File(temporaryFolder, "bin");
        File pack = new File(Objects.requireNonNull(getClass().getResource(packResource)).getFile());
        Files.createDirectory(bin.toPath());
        Files.copy(pack.toPath(), bin.toPath().resolve(pack.getName()), COPY_ATTRIBUTES);
      }
    }
  }

  private void givenPackCliAlreadyDownloaded() throws IOException {
    File jKubeDownloadDir = new File(temporaryFolder, ".jkube");
    Files.createDirectory(jKubeDownloadDir.toPath());
    Files.copy(oldPackCliInJKubeDir.toPath(), jKubeDownloadDir.toPath().resolve(oldPackCliInJKubeDir.getName()), COPY_ATTRIBUTES);
  }
}
