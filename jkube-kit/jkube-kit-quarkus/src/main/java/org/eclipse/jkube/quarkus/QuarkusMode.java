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
package org.eclipse.jkube.quarkus;

import static org.eclipse.jkube.quarkus.QuarkusUtils.getQuarkusConfiguration;
import static org.eclipse.jkube.quarkus.QuarkusUtils.runnerSuffix;

import java.io.File;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;

import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.quarkus.generator.QuarkusAssemblies;

public enum QuarkusMode {

  NATIVE("native", false, QuarkusAssemblies.NATIVE),
  FAST_JAR("fast-jar", false, QuarkusAssemblies.FAST_JAR),
  LEGACY_JAR("legacy-jar", false, QuarkusAssemblies.LEGACY_JAR),
  UBER_JAR("uber-jar", true, QuarkusAssemblies.UBER_JAR);

  private final String packageType;
  private final boolean isFatJar;
  private final QuarkusAssemblies.QuarkusAssembly assembly;

  QuarkusMode(String packageType, boolean isFatJar, QuarkusAssemblies.QuarkusAssembly assembly) {
    this.packageType = packageType;
    this.isFatJar = isFatJar;
    this.assembly = assembly;
  }

  public boolean isFatJar() {
    return isFatJar;
  }

  public QuarkusAssemblies.QuarkusAssembly getAssembly() {
    return assembly;
  }

  public static QuarkusMode from(JavaProject project) {
    final Properties quarkusConfiguration = getQuarkusConfiguration(project);
    return from(quarkusConfiguration).orElseGet(() -> fromFiles(project, quarkusConfiguration));
  }

  private static Optional<QuarkusMode> from(Properties properties) {
    return Optional.ofNullable(properties.getProperty("quarkus.package.type"))
        .flatMap(packageType -> Arrays.stream(QuarkusMode.values())
            .filter(qm -> qm.packageType.equalsIgnoreCase(packageType))
            .findFirst());
  }

  private static QuarkusMode fromFiles(JavaProject project, Properties properties) {
    final String runnerSuffix = runnerSuffix(properties);
    if (hasFileThatEndsWith(project, runnerSuffix + ".jar") && new File(project.getBuildDirectory(), "lib").exists()) {
      return QuarkusMode.LEGACY_JAR;
    } else if(hasFileThatEndsWith(project, runnerSuffix + ".jar")) {
      return QuarkusMode.UBER_JAR;
    } else if(hasFileThatEndsWith(project, runnerSuffix)) {
      return QuarkusMode.NATIVE;
    }
    return QuarkusMode.FAST_JAR;
  }

  private static boolean hasFileThatEndsWith(JavaProject project, String suffix) {
    File buildDir = project.getBuildDirectory();
    String[] file = buildDir.list((dir, name) -> name.endsWith(suffix));
    return file != null && file.length > 0;
  }
}
