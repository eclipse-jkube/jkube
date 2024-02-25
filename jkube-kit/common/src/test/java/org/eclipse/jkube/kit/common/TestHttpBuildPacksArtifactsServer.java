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
import java.util.Objects;

public class TestHttpBuildPacksArtifactsServer implements Closeable {
  private final TestHttpStaticServer testHttpStaticServer;
  private static final String LINUX_ARTIFACT = "pack-v0.32.1-linux.tgz";
  private static final String LINUX_ARM64_ARTIFACT = "pack-v0.32.1-linux-arm64.tgz";
  private static final String MACOS_ARTIFACT = "pack-v0.32.1-macos.tgz";
  private static final String MACOS_ARM64_ARTIFACT = "pack-v0.32.1-macos-arm64.tgz";
  private static final String WINDOWS_ARTIFACT = "pack-v0.32.1-windows.zip";
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

  private String createUrlForArtifact(String artifactName) {
    return String.format("%s/%s", getBaseUrl(), artifactName);
  }

  private File createTemporaryArtifactsDir() {
    try {
      File artifactDir = FileUtil.createTempDirectory();

      FileUtils.copyInputStreamToFile(Objects.requireNonNull(TestHttpBuildPacksArtifactsServer.class.getResourceAsStream(String.format("/buildpack-download-artifacts/%s", LINUX_ARTIFACT))), new File(artifactDir, LINUX_ARTIFACT));
      FileUtils.copyInputStreamToFile(Objects.requireNonNull(TestHttpBuildPacksArtifactsServer.class.getResourceAsStream(String.format("/buildpack-download-artifacts/%s", LINUX_ARM64_ARTIFACT))), new File(artifactDir, LINUX_ARM64_ARTIFACT));
      FileUtils.copyInputStreamToFile(Objects.requireNonNull(TestHttpBuildPacksArtifactsServer.class.getResourceAsStream(String.format("/buildpack-download-artifacts/%s", MACOS_ARTIFACT))), new File(artifactDir, MACOS_ARTIFACT));
      FileUtils.copyInputStreamToFile(Objects.requireNonNull(TestHttpBuildPacksArtifactsServer.class.getResourceAsStream(String.format("/buildpack-download-artifacts/%s", MACOS_ARM64_ARTIFACT))), new File(artifactDir, MACOS_ARM64_ARTIFACT));
      FileUtils.copyInputStreamToFile(Objects.requireNonNull(TestHttpBuildPacksArtifactsServer.class.getResourceAsStream(String.format("/buildpack-download-artifacts/%s", WINDOWS_ARTIFACT))), new File(artifactDir, WINDOWS_ARTIFACT));
      return artifactDir;
    } catch (IOException ioException) {
      throw new IllegalStateException("Failure in creating build pack artifacts server : ", ioException);
    }
  }

  @Override
  public void close() throws IOException {
    testHttpStaticServer.close();
    FileUtil.cleanDirectory(remoteBuildPackArtifactsDir);
  }
}
