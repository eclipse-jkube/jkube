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
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

public class SpringBootLayeredJar {

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
      kitLogger.debug("Couldn't determine if Spring Boot jar %s is layered", layeredJar.getName(), e);
    }
    return false;
  }

  public String getMainClass() {
    try (JarFile jarFile = new JarFile(layeredJar)) {
      final ZipEntry manifest = jarFile.getEntry("META-INF/MANIFEST.MF");
      if (manifest == null) {
        return null;
      }
      final Properties properties = new Properties();
      try (InputStream manifestInputStream = jarFile.getInputStream(manifest)) {
        properties.load(manifestInputStream);
        return properties.getProperty("Main-Class");
      }
    } catch(Exception e) {
      kitLogger.debug("Couldn't determine Spring Boot jar's (%s) main class ", layeredJar.getName(), e);
    }
    return null;
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
    try {
      new LayerToolsCommand(kitLogger, extractionDir, layeredJar,  "extract").execute();
    } catch (IOException ioException) {
      throw new IllegalStateException("Failure in extracting spring boot jar layers", ioException);
    }
  }

  private static class LayerToolsCommand extends ExternalCommand {
    private final File layeredJar;
    private final String[] args;

    protected LayerToolsCommand(KitLogger log, File workDir, File layeredJar, String... args) {
      super(log, workDir);
      this.layeredJar = layeredJar;
      this.args = args;
    }

    @Override
    protected String[] getArgs() {
      return ArrayUtils.addAll(new String[] { "java", "-Djarmode=layertools", "-jar", layeredJar.getAbsolutePath()}, args);
    }
  }

}
