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
package org.eclipse.jkube.springboot;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.generator.javaexec.FatJarDetector;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.util.FileUtil;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;
import org.eclipse.jkube.kit.common.util.SpringBootUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.eclipse.jkube.kit.common.util.SpringBootUtil.DEV_TOOLS_REMOTE_SECRET;
import static org.eclipse.jkube.kit.common.util.SpringBootUtil.SPRING_BOOT_DEVTOOLS_ARTIFACT_ID;
import static org.eclipse.jkube.kit.common.util.SpringBootUtil.SPRING_BOOT_GROUP_ID;

public class SpringBootDevtoolsUtils {
  private SpringBootDevtoolsUtils() { }

  public static boolean ensureSpringDevToolSecretToken(JavaProject project) {
    Properties properties = SpringBootUtil.getSpringBootApplicationProperties(
        SpringBootUtil.getSpringBootActiveProfile(project),
        JKubeProjectUtil.getClassLoader(project));
    String remoteSecret = properties.getProperty(DEV_TOOLS_REMOTE_SECRET);
    if (StringUtils.isBlank(remoteSecret)) {
      addSecretTokenToApplicationProperties(project);
      return false;
    }
    return true;
  }

  private static void addSecretTokenToApplicationProperties(JavaProject project) {
    String newToken = UUID.randomUUID().toString();

    // We always add to application.properties, even when an application.yml exists, since both
    // files are evaluated by Spring Boot.
    appendSecretTokenToFile(project, "target/classes/application.properties", newToken);
    appendSecretTokenToFile(project, "src/main/resources/application.properties", newToken);
  }

  private static void appendSecretTokenToFile(JavaProject project, String path, String token) {
    File file = new File(project.getBaseDirectory(), path);
    try {
      FileUtil.createDirectory(file.getParentFile());
    } catch (IOException ioException) {
      throw new IllegalStateException("Failure in creating directory " + file.getParentFile().getAbsolutePath());
    }
    writeRemoteSecretToFile(file, token);
  }

  private static void writeRemoteSecretToFile(File file, String token) {
    String text = String.format("%s" +
            "# Remote secret added by jkube-kit-plugin\n" +
            "%s=%s\n",
        file.exists() ? "\n" : "", DEV_TOOLS_REMOTE_SECRET, token);

    try (FileWriter writer = new FileWriter(file, true)) {
      writer.append(text);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to append to file: " + file + ". " + e, e);
    }
  }

  public static void addDevToolsFilesToFatJar(JavaProject project, FatJarDetector.Result fatJarDetectResult) {
    File target = getFatJarFile(fatJarDetectResult);
    try {
      File devToolsFile = getSpringBootDevToolsJar(project);
      File applicationPropertiesFile = new File(project.getBaseDirectory(), "target/classes/application.properties");
      copyFilesToFatJar(Collections.singletonList(devToolsFile), Collections.singletonList(applicationPropertiesFile), target);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to add devtools files to fat jar " + target + ". " + e, e);
    }
  }

  private static void copyFilesToFatJar(List<File> libs, List<File> classes, File target) throws IOException {
    File tmpZip = File.createTempFile(target.getName(), null);
    Files.delete(tmpZip.toPath());

    // Using Apache commons rename, because renameTo has issues across file systems
    FileUtils.moveFile(target, tmpZip);

    byte[] buffer = new byte[8192];
    try (ZipInputStream zin = new ZipInputStream(new FileInputStream(tmpZip));
         ZipOutputStream out = new ZipOutputStream(new FileOutputStream(target))) {
      for (ZipEntry ze = zin.getNextEntry(); ze != null; ze = zin.getNextEntry()) {
        if (matchesFatJarEntry(libs, ze.getName(), true) || matchesFatJarEntry(classes, ze.getName(), false)) {
          continue;
        }
        out.putNextEntry(ze);
        for(int read = zin.read(buffer); read > -1; read = zin.read(buffer)){
          out.write(buffer, 0, read);
        }
        out.closeEntry();
      }

      for (File lib : libs) {
        try (InputStream in = new FileInputStream(lib)) {
          out.putNextEntry(createZipEntry(lib, getFatJarFullPath(lib, true)));
          for (int read = in.read(buffer); read > -1; read = in.read(buffer)) {
            out.write(buffer, 0, read);
          }
          out.closeEntry();
        }
      }

      for (File cls : classes) {
        try (InputStream in = new FileInputStream(cls)) {
          out.putNextEntry(createZipEntry(cls, getFatJarFullPath(cls, false)));
          for (int read = in.read(buffer); read > -1; read = in.read(buffer)) {
            out.write(buffer, 0, read);
          }
          out.closeEntry();
        }
      }
    }
    Files.delete(tmpZip.toPath());
  }

  private static boolean matchesFatJarEntry(List<File> fatJarEntries, String path, boolean lib) {
    for (File e : fatJarEntries) {
      String fullPath = getFatJarFullPath(e, lib);
      if (fullPath.equals(path)) {
        return true;
      }
    }
    return false;
  }

  private static String getFatJarFullPath(File file, boolean lib) {
    if (lib) {
      return "BOOT-INF/lib/" + file.getName();
    }
    return "BOOT-INF/classes/" + file.getName();
  }

  private static ZipEntry createZipEntry(File file, String fullPath) throws IOException {
    ZipEntry entry = new ZipEntry(fullPath);

    byte[] buffer = new byte[8192];
    int bytesRead = -1;
    try (InputStream is = new FileInputStream(file)) {
      CRC32 crc = new CRC32();
      int size = 0;
      while ((bytesRead = is.read(buffer)) != -1) {
        crc.update(buffer, 0, bytesRead);
        size += bytesRead;
      }
      entry.setSize(size);
      entry.setCompressedSize(size);
      entry.setCrc(crc.getValue());
      entry.setMethod(ZipEntry.STORED);
      return entry;
    }
  }
  private static File getFatJarFile(FatJarDetector.Result fatJarDetectResult) {
    if (fatJarDetectResult == null) {
      throw new IllegalStateException("No fat jar built yet. Please ensure that the 'package' phase has run");
    }
    return fatJarDetectResult.getArchiveFile();
  }

  public static File getSpringBootDevToolsJar(JavaProject project) {
    String version = SpringBootUtil.getSpringBootVersion(project)
        .orElseThrow(() -> new IllegalStateException("Unable to find the spring-boot version"));
    final File devToolsJar = JKubeProjectUtil.resolveArtifact(project, SPRING_BOOT_GROUP_ID, SPRING_BOOT_DEVTOOLS_ARTIFACT_ID, version, "jar");
    if (!devToolsJar.exists()) {
      throw new IllegalArgumentException("devtools need to be included in repacked archive, please set <excludeDevtools> to false in plugin configuration");
    }
    return devToolsJar;
  }
}
