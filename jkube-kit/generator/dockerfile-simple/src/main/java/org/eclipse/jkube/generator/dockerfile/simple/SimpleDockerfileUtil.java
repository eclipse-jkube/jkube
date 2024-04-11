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
package org.eclipse.jkube.generator.dockerfile.simple;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import static org.eclipse.jkube.kit.build.api.helper.DockerFileUtil.extractLines;

public class SimpleDockerfileUtil {
  private SimpleDockerfileUtil() {
  }

  public static boolean isSimpleDockerFileMode(File projectBaseDirectory) {
    if (projectBaseDirectory != null) {
      return getTopLevelDockerfile(projectBaseDirectory).exists();
    }
    return false;
  }

  public static File getTopLevelDockerfile(File projectBaseDirectory) {
    return new File(projectBaseDirectory, "Dockerfile");
  }

  public static ImageConfiguration createSimpleDockerfileConfig(File dockerFile, String defaultImageName) {
    if (defaultImageName == null) {
      // Default name group/artifact:version (or 'latest' if SNAPSHOT)
      defaultImageName = "%g/%a:%l";
    }

    final BuildConfiguration buildConfig = BuildConfiguration.builder()
        .dockerFile(dockerFile.getPath())
        .ports(extractPorts(dockerFile))
        .build();

    return ImageConfiguration.builder()
        .name(defaultImageName)
        .build(buildConfig)
        .build();
  }

  public static ImageConfiguration addSimpleDockerfileConfig(ImageConfiguration image, File dockerfile) {
    final BuildConfiguration buildConfig = BuildConfiguration.builder()
        .dockerFile(dockerfile.getPath())
        .ports(extractPorts(dockerfile))
        .build();
    return image.toBuilder().build(buildConfig).build();
  }

  static List<String> extractPorts(File dockerFile) {
    Properties properties = new Properties();
    try {
      return extractPorts(extractLines(dockerFile, "EXPOSE", properties, null));
    } catch (IOException ioException) {
      throw new IllegalArgumentException("Error in reading Dockerfile", ioException);
    }
  }

  static List<String> extractPorts(List<String[]> dockerLinesContainingExpose) {
    Set<String> ports = new HashSet<>();
    dockerLinesContainingExpose.forEach(line -> Arrays.stream(line)
        .skip(1)
        .filter(Objects::nonNull)
        .filter(StringUtils::isNotBlank)
        .forEach(ports::add));
    return new ArrayList<>(ports);
  }
}
