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

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.common.ExternalCommand;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.Serialization;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

public class SpringBootLayeredJar {

  static final String JARMODE_TOOLS = "tools";
  static final String JARMODE_LAYERTOOLS = "layertools";

  private final File layeredJar;
  private final KitLogger kitLogger;

  public SpringBootLayeredJar(File layeredJar, KitLogger kitLogger) {
    this.layeredJar = layeredJar;
    this.kitLogger = kitLogger;
  }

  public boolean isLayeredJar() {
    try (JarFile jarFile = new JarFile(layeredJar)) {
      return jarFile.getEntry("BOOT-INF/layers.idx") != null && StringUtils.isNotBlank(getMainClass());
    } catch(Exception e) {
      kitLogger.debug("Couldn't determine if Spring Boot jar %s is layered: %s", layeredJar.getName(), e.getMessage());
    }
    return false;
  }

  public String getMainClass() {
    return getManifestAttribute("Main-Class").orElse(null);
  }

  public Optional<String> getSpringBootVersion() {
    return getManifestAttribute("Spring-Boot-Version");
  }

  private Optional<String> getManifestAttribute(String attributeName) {
    try (JarFile jarFile = new JarFile(layeredJar)) {
      final Manifest manifest = jarFile.getManifest();
      if (manifest != null) {
        return Optional.ofNullable(manifest.getMainAttributes().getValue(attributeName));
      }
    } catch (IOException e) {
      kitLogger.debug("Couldn't read %s from %s: %s", attributeName, layeredJar.getName(), e.getMessage());
    }
    return Optional.empty();
  }

  public List<String> listLayers() {
    try (JarFile jarFile = new JarFile(layeredJar)) {
      List<Map<String, List<String>>> layers = Serialization.unmarshal(jarFile.getInputStream(jarFile.getEntry("BOOT-INF/layers.idx")), List.class);
      if (layers == null) {
        throw new IOException("Unable to find layers information in BOOT-INF/layers.idx file");
      }

      return layers.stream()
          .flatMap(m -> m.keySet().stream())
          .collect(Collectors.toList());
    } catch (IOException ioException) {
      throw new IllegalStateException("Failure in getting spring boot jar layers information", ioException);
    }
  }

  public void extractLayers(File extractionDir) {
    // Execute jarmode to extract layers
    // Note: Requires Maven/Gradle JDK to be compatible with application target JDK
    final Optional<String> jarMode = determineJarMode();
    IOException primaryException = null;

    if (jarMode.isPresent()) {
      try {
        String[] extractArgs = getExtractArgs(jarMode.get());
        executeLayerToolsCommand(extractionDir, jarMode.get(), extractArgs);
        kitLogger.info("Extracted Spring Boot layers using jarmode=%s", jarMode.get());
        return;
      } catch (IOException ioException) {
        kitLogger.debug("Failed with jarmode=%s: %s", jarMode.get(), ioException.getMessage());
        primaryException = ioException;
      }
    } else {
      // Version couldn't be detected: try both jarmodes, newest first
      for (String fallbackJarMode : new String[]{JARMODE_TOOLS, JARMODE_LAYERTOOLS}) {
        try {
          kitLogger.debug("Trying jarmode=%s for layer extraction", fallbackJarMode);
          String[] extractArgs = getExtractArgs(fallbackJarMode);
          executeLayerToolsCommand(extractionDir, fallbackJarMode, extractArgs);
          kitLogger.info("Extracted Spring Boot layers using jarmode=%s (fallback)", fallbackJarMode);
          return;
        } catch (IOException ioException) {
          kitLogger.debug("Failed with jarmode=%s: %s", fallbackJarMode, ioException.getMessage());
          if (primaryException == null) {
            primaryException = ioException;
          } else {
            primaryException.addSuppressed(ioException);
          }
        }
      }
    }
    throw new IllegalStateException("Failure in extracting spring boot jar layers", primaryException);
  }

  // Package-private seam for testing - allows tests to observe command invocations
  void executeLayerToolsCommand(File extractionDir, String jarMode, String[] extractArgs) throws IOException {
    new LayerToolsCommand(kitLogger, extractionDir, layeredJar, jarMode, extractArgs).execute();
  }

  // Package-private for testing
  String[] getExtractArgs(String jarMode) {
    // Spring Boot 4.1+ tools jarmode: requires --launcher and --layers for layered structure,
    // --destination to control output location, and --force for idempotent extraction
    if (JARMODE_TOOLS.equals(jarMode)) {
      return new String[]{"extract", "--launcher", "--layers", "--destination", ".", "--force"};
    } else {
      // Spring Boot < 4.1 layertools jarmode: supports --destination but not --force
      return new String[]{"extract", "--destination", "."};
    }
  }

  // Package-private for testing
  boolean isVersion410OrNewer(String version) {
    try {
      String[] parts = version.split("[.-]");
      if (parts.length < 2) {
        return false;
      }
      int major = Integer.parseInt(parts[0]);
      int minor = Integer.parseInt(parts[1]);

      return major > 4 || (major == 4 && minor >= 1);
    } catch (NumberFormatException e) {
      kitLogger.debug("Unable to parse Spring Boot version %s: %s", version, e.getMessage());
      return false;
    }
  }

  /**
   * The jarmode to use for layer extraction. Package-private for testing.
   *
   * @return the jarmode to use, or empty if the Spring Boot version can't be determined from the
   *         jar manifest, in which case {@link #extractLayers(File)} tries both jarmodes.
   */
  Optional<String> determineJarMode() {
    return getSpringBootVersion()
        .map(version -> isVersion410OrNewer(version) ? JARMODE_TOOLS : JARMODE_LAYERTOOLS);
  }

  /**
   * Command executor for both Spring Boot jarmodes (layertools and tools).
   * Runs java -Djarmode={jarMode} -jar {jar} {args}
   */
  private static class LayerToolsCommand extends ExternalCommand {
    private final File layeredJar;
    private final String[] args;
    private final String jarMode;

    protected LayerToolsCommand(KitLogger log, File workDir, File layeredJar, String jarMode, String... args) {
      super(log, workDir);
      this.layeredJar = layeredJar;
      this.jarMode = jarMode;
      this.args = args;
    }

    @Override
    protected String[] getArgs() {
      return ArrayUtils.addAll(new String[] { "java", "-Djarmode=" + jarMode, "-jar", layeredJar.getAbsolutePath()}, args);
    }
  }

}
