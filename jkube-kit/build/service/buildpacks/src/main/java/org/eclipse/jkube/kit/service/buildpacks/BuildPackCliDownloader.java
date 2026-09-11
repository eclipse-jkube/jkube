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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.FileUtil;
import org.eclipse.jkube.kit.common.util.PropertiesUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.eclipse.jkube.kit.common.archive.ArchiveDecompressor.extractArchive;
import static org.eclipse.jkube.kit.common.util.EnvUtil.findBinaryFileInUserPath;
import static org.eclipse.jkube.kit.common.util.EnvUtil.getProcessorArchitecture;
import static org.eclipse.jkube.kit.common.util.EnvUtil.isMacOs;
import static org.eclipse.jkube.kit.common.util.EnvUtil.isWindows;
import static org.eclipse.jkube.kit.common.util.EnvUtil.getUserHome;
import static org.eclipse.jkube.kit.common.util.IoUtil.downloadFile;
import static org.eclipse.jkube.kit.common.util.SemanticVersionUtil.removeBuildMetadata;

public class BuildPackCliDownloader {
  private static final String JKUBE_PACK_DIR = ".jkube";
  private static final String PACK_UNIX_CLI_NAME = "pack";
  private static final String PACK_DEFAULT_CLI_VERSION_PROPERTY = "version";
  private static final String PACK_CLI_LINUX_ARTIFACT = "linux.artifact";
  private static final String PACK_CLI_LINUX_ARM64_ARTIFACT = "linux-arm64.artifact";
  private static final String PACK_CLI_MACOS_ARTIFACT = "macos.artifact";
  private static final String PACK_CLI_MACOS_ARM64_ARTIFACT = "macos-arm64.artifact";
  private static final String PACK_CLI_WINDOWS_ARTIFACT = "windows.artifact";
  private static final String SHA256_SUFFIX = ".sha256";
  private static final String SHA256_ALGORITHM = "SHA-256";

  private final KitLogger kitLogger;
  private final String packCliVersion;
  private final Properties packProperties;
  private final File jKubeUserHomeDir;

  public BuildPackCliDownloader(KitLogger kitLogger) {
    this(kitLogger, null);
  }

  public BuildPackCliDownloader(KitLogger kitLogger, Properties packProperties) {
    this.kitLogger = kitLogger;
    if (packProperties != null) {
      this.packProperties = packProperties;
    } else {
      this.packProperties = PropertiesUtil
        .getPropertiesFromResource(BuildPackCliDownloader.class.getResource("/META-INF/jkube/pack-cli.properties"));
    }
    packCliVersion = this.packProperties.getProperty(PACK_DEFAULT_CLI_VERSION_PROPERTY);
    jKubeUserHomeDir = new File(getUserHome(), JKUBE_PACK_DIR);
  }

  public File getPackCLIIfPresentOrDownload() {
    try {
      File pack = resolveBinaryLocation().toFile();
      if (!(pack.exists() && isValid(pack))) {
        downloadPackCli();
      }
      return pack;
    } catch (IOException ioException) {
      kitLogger.warn("Not able to download pack CLI : " + ioException.getMessage());
      kitLogger.info("Checking for local pack CLI");
      return getLocalPackCLI();
    }
  }

  private void downloadPackCli() throws IOException {
    File tempDownloadDirectory = FileUtil.createTempDirectory();
    FileUtil.createDirectory(jKubeUserHomeDir);
    String artifactUrlString = inferApplicableDownloadArtifactUrl();
    URL downloadUrl = new URL(artifactUrlString);
    Path packInJKubeDir = resolveBinaryLocation();
    kitLogger.info("Downloading pack CLI %s", packCliVersion);

    File downloadedArchive = new File(tempDownloadDirectory, new File(downloadUrl.getPath()).getName());
    downloadFile(downloadUrl, downloadedArchive);
    verifyChecksum(downloadUrl, downloadedArchive);

    extractArchive(downloadedArchive, tempDownloadDirectory);

    File packInExtractedArchive = new File(tempDownloadDirectory, packInJKubeDir.toFile().getName());
    if (!packInExtractedArchive.exists()) {
      throw new IllegalStateException("Unable to find " + packInJKubeDir.toFile().getName() + " in downloaded artifact");
    }
    if (!packInExtractedArchive.canExecute() && !packInExtractedArchive.setExecutable(true)) {
      throw new IllegalStateException("Failure in setting execute permission in " + packInExtractedArchive.getAbsolutePath());
    }
    Files.copy(packInExtractedArchive.toPath(), packInJKubeDir, REPLACE_EXISTING, COPY_ATTRIBUTES);
    FileUtil.cleanDirectory(tempDownloadDirectory);
  }

  private void verifyChecksum(URL archiveUrl, File downloadedArchive) throws IOException {
    URL checksumUrl = new URL(archiveUrl.toExternalForm() + SHA256_SUFFIX);
    File checksumFile = new File(downloadedArchive.getParentFile(), downloadedArchive.getName() + SHA256_SUFFIX);
    downloadFile(checksumUrl, checksumFile);

    String expectedChecksum = parseExpectedChecksum(
        new String(Files.readAllBytes(checksumFile.toPath()), StandardCharsets.UTF_8));
    String actualChecksum = calculateSha256(downloadedArchive);

    if (!expectedChecksum.equalsIgnoreCase(actualChecksum)) {
      throw new IOException(String.format(
          "Checksum mismatch for downloaded pack CLI archive %s: expected %s but got %s",
          downloadedArchive.getName(), expectedChecksum, actualChecksum));
    }
    kitLogger.debug("Checksum verified for pack CLI archive %s", downloadedArchive.getName());
  }

  static String parseExpectedChecksum(String sha256FileContent) {
    if (StringUtils.isBlank(sha256FileContent)) {
      throw new IllegalStateException("Checksum file is empty");
    }
    String firstLine = sha256FileContent.trim().split("\\r?\\n")[0].trim();
    String[] parts = firstLine.split("\\s+");
    if (parts.length == 0 || StringUtils.isBlank(parts[0])) {
      throw new IllegalStateException("Unable to parse checksum from: " + sha256FileContent);
    }
    return parts[0];
  }

  static String calculateSha256(File file) throws IOException {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance(SHA256_ALGORITHM);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm not available", e);
    }
    try (InputStream inputStream = Files.newInputStream(file.toPath())) {
      byte[] buffer = new byte[8192];
      int bytesRead;
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        digest.update(buffer, 0, bytesRead);
      }
    }
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

  private String inferApplicableDownloadArtifactUrl() {
    boolean isProcessorArchitectureArm = getProcessorArchitecture().equals("aarch64");
    if (isWindows()) {
      return (String) packProperties.get(PACK_CLI_WINDOWS_ARTIFACT);
    } else if (isMacOs() && isProcessorArchitectureArm) {
      return (String) packProperties.get(PACK_CLI_MACOS_ARM64_ARTIFACT);
    } else if(isMacOs()) {
      return (String) packProperties.get(PACK_CLI_MACOS_ARTIFACT);
    } else if (isProcessorArchitectureArm) {
      return (String) packProperties.get(PACK_CLI_LINUX_ARM64_ARTIFACT);
    }
    return (String) packProperties.get(PACK_CLI_LINUX_ARTIFACT);
  }

  private File getLocalPackCLI() {
    File packCliFoundOnUserPath = checkPackCLIPresentOnMachine();
    if (packCliFoundOnUserPath == null) {
      throw new IllegalStateException("No local pack binary found");
    }
    return packCliFoundOnUserPath;
  }

  private File checkPackCLIPresentOnMachine() {
    File packCliFoundOnUserPath = findBinaryFileInUserPath(resolveBinaryLocation().toFile().getName());
    if (packCliFoundOnUserPath != null && isValid(packCliFoundOnUserPath)) {
      return packCliFoundOnUserPath;
    }
    return null;
  }

  public boolean isValid(File packCli) {
    AtomicReference<String> versionRef = new AtomicReference<>();
    BuildPackCommand versionCommand = new BuildPackCommand(kitLogger, packCli, Collections.singletonList("--version"), versionRef::set);
    try {
      versionCommand.execute();
      String version = removeBuildMetadata(versionRef.get());
      return StringUtils.isNotBlank(version) && packCliVersion.equals(version);
    } catch (IOException e) {
      return false;
    }
  }

  private Path resolveBinaryLocation() {
    String binaryName = PACK_UNIX_CLI_NAME;
    if (isWindows()) {
      binaryName = PACK_UNIX_CLI_NAME + "." + packProperties.getProperty("windows.binary-extension");
    }
    return jKubeUserHomeDir.toPath().resolve(binaryName);
  }
}
