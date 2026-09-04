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
package org.eclipse.jkube.kit.common;

import org.apache.commons.io.FileUtils;
import org.eclipse.jkube.kit.common.util.FileUtil;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public class TestHttpBuildPacksArtifactsServer implements Closeable {
  private final TestHttpStaticServer testHttpStaticServer;
  private static final String LINUX_ARTIFACT = "pack-v0.32.1-linux.tgz";
  private static final String LINUX_ARM64_ARTIFACT = "pack-v0.32.1-linux-arm64.tgz";
  private static final String MACOS_ARTIFACT = "pack-v0.32.1-macos.tgz";
  private static final String MACOS_ARM64_ARTIFACT = "pack-v0.32.1-macos-arm64.tgz";
  private static final String WINDOWS_ARTIFACT = "pack-v0.32.1-windows.zip";
  private static final String SHA256_SUFFIX = ".sha256";
  private final File remoteBuildPackArtifactsDir;

  public TestHttpBuildPacksArtifactsServer() {
    remoteBuildPackArtifactsDir = createTemporaryArtifactsDir();
    testHttpStaticServer = new TestHttpStaticServer(remoteBuildPackArtifactsDir);
  }

  public String getLinuxArtifactUrl() {
    return createUrlForArtifact(LINUX_ARTIFACT);
  }

  public String getLinuxArm64ArtifactUrl() {
    return createUrlForArtifact(LINUX_ARM64_ARTIFACT);
  }

  public String getMacosArtifactUrl() {
    return createUrlForArtifact(MACOS_ARTIFACT);
  }

  public String getMacosArm64ArtifactUrl() {
    return createUrlForArtifact(MACOS_ARM64_ARTIFACT);
  }

  public String getWindowsArtifactUrl() {
    return createUrlForArtifact(WINDOWS_ARTIFACT);
  }

  public String getBaseUrl() {
    return String.format("http://localhost:%d", testHttpStaticServer.getPort());
  }
  public File getArtifactsDir() {
    return remoteBuildPackArtifactsDir;
  }

  private String createUrlForArtifact(String artifactName) {
    return String.format("%s/%s", getBaseUrl(), artifactName);
  }

  private File createTemporaryArtifactsDir() {
    try {
      File artifactDir = FileUtil.createTempDirectory();

      copyArtifactWithChecksum(artifactDir, LINUX_ARTIFACT);
      copyArtifactWithChecksum(artifactDir, LINUX_ARM64_ARTIFACT);
      copyArtifactWithChecksum(artifactDir, MACOS_ARTIFACT);
      copyArtifactWithChecksum(artifactDir, MACOS_ARM64_ARTIFACT);
      copyArtifactWithChecksum(artifactDir, WINDOWS_ARTIFACT);
      return artifactDir;
    } catch (IOException ioException) {
      throw new IllegalStateException("Failure in creating build pack artifacts server : ", ioException);
    }
  }

  private void copyArtifactWithChecksum(File artifactDir, String artifactName) throws IOException {
    File artifactFile = new File(artifactDir, artifactName);
    FileUtils.copyInputStreamToFile(
            Objects.requireNonNull(TestHttpBuildPacksArtifactsServer.class.getResourceAsStream(
                    String.format("/buildpack-download-artifacts/%s", artifactName))),
            artifactFile);

    File checksumFile = new File(artifactDir, artifactName + SHA256_SUFFIX);
    String checksum = calculateSha256(artifactFile);
    Files.write(checksumFile.toPath(),
            (checksum + "  " + artifactName + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));
  }

  private static String calculateSha256(File file) throws IOException {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm not available", e);
    }
    digest.update(Files.readAllBytes(file.toPath()));
    StringBuilder hexString = new StringBuilder();
    for (byte b : digest.digest()) {
      String hex = Integer.toHexString(0xff & b);
      if (hex.length() == 1) {
        hexString.append('0');
      }
      hexString.append(hex);
    }
    return hexString.toString();
  }

  @Override
  public void close() throws IOException {
    testHttpStaticServer.close();
    FileUtil.cleanDirectory(remoteBuildPackArtifactsDir);
  }
}