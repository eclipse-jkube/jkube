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

import lombok.Getter;
import org.eclipse.jkube.kit.common.ExternalCommand;
import org.eclipse.jkube.kit.common.KitLogger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SpringBootLayeredJar {

  private final File layeredJar;
  private final KitLogger kitLogger;

  public SpringBootLayeredJar(File layeredJar, KitLogger kitLogger) {
    this.layeredJar = layeredJar;
    this.kitLogger = kitLogger;
  }

  public List<String> listLayers() {
    final LayerListCommand layerListCommand = new LayerListCommand(kitLogger, layeredJar);
    try {
      layerListCommand.execute();
      return layerListCommand.getLayers();
    } catch (IOException ioException) {
      throw new IllegalStateException("Failure in getting spring boot jar layers information", ioException);
    }
  }

  public void extractLayers(File extractionDir) {
    try {
      new LayerExtractorCommand(kitLogger, extractionDir, layeredJar).execute();
    } catch (IOException ioException) {
      throw new IllegalStateException("Failure in extracting spring boot jar layers", ioException);
    }
  }

  private static class LayerExtractorCommand extends ExternalCommand {
    private final File layeredJar;
    protected LayerExtractorCommand(KitLogger log, File workDir, File layeredJar) {
      super(log, workDir);
      this.layeredJar = layeredJar;
    }

    @Override
    protected String[] getArgs() {
      return new String[] { "java", "-Djarmode=layertools", "-jar", layeredJar.getAbsolutePath(), "extract"};
    }
  }

  private static class LayerListCommand extends ExternalCommand {
    private final File layeredJar;
    @Getter
    private final List<String> layers;

    protected LayerListCommand(KitLogger log, File layeredJar) {
      super(log);
      this.layeredJar = layeredJar;
      layers = new ArrayList<>();
    }

    @Override
    protected String[] getArgs() {
      return new String[] { "java", "-Djarmode=layertools", "-jar", layeredJar.getAbsolutePath(), "list"};
    }

    @Override
    protected void processLine(String line) {
      layers.add(line);
    }

  }
}
