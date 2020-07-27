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
package org.eclipse.jkube.kit.config.image.build;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.eclipse.jkube.kit.config.image.RegistryConfig;
import org.eclipse.jkube.kit.common.JavaProject;

/**
 * @author roland
 */
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@EqualsAndHashCode
public class JKubeConfiguration implements Serializable {

  private static final long serialVersionUID = 7459084747241070651L;

  private JavaProject project;
  private String sourceDirectory;
  private String outputDirectory;
  private Map<String, String> buildArgs;
  private RegistryConfig registryConfig;
  private List<JavaProject> reactorProjects;

  public File getBasedir() {
    return project.getBaseDirectory();
  }

  public Properties getProperties() {
    return project.getProperties();
  }

  public File inOutputDir(String path) {
    return inDir(getOutputDirectory(), path);
  }

  public File inSourceDir(String path) {
    return inDir(getSourceDirectory(), path);
  }

  public File inDir(String dir, String path) {
    File file = new File(path);
    if (file.isAbsolute()) {
      return file;
    }
    File absoluteSourceDir = new File(getBasedir(), dir);
    return new File(absoluteSourceDir, path);
  }

}
