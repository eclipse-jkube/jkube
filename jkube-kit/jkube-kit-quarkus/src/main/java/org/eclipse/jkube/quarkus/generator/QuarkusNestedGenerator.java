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
package org.eclipse.jkube.quarkus.generator;

import org.eclipse.jkube.generator.api.GeneratorConfig;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.Arguments;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;

import java.io.File;
import java.util.Optional;
import java.util.Properties;
import java.util.function.BiFunction;

import static org.eclipse.jkube.generator.javaexec.JavaExecGenerator.JOLOKIA_PORT_DEFAULT;
import static org.eclipse.jkube.generator.javaexec.JavaExecGenerator.PROMETHEUS_PORT_DEFAULT;
import static org.eclipse.jkube.quarkus.QuarkusUtils.getQuarkusConfiguration;
import static org.eclipse.jkube.quarkus.QuarkusUtils.runnerSuffix;

public interface QuarkusNestedGenerator {

  JavaProject getProject();

  RuntimeMode getRuntimeMode();

  AssemblyConfiguration createAssemblyConfiguration();

  default boolean isFatJar() {
    return false;
  }

  default String getFrom() {
    return null;
  }

  default String getDefaultJolokiaPort() {
    return JOLOKIA_PORT_DEFAULT;
  }

  default String getDefaultPrometheusPort() {
    return PROMETHEUS_PORT_DEFAULT;
  }

  default Arguments getBuildEntryPoint() {
    return null;
  }

  String getBuildWorkdir();

  String getTargetDir();

  static QuarkusNestedGenerator from(GeneratorContext context, GeneratorConfig config) {
    // Legacy (Quarkus 1.0) settings support
    if (Boolean.parseBoolean(config.get(QuarkusGenerator.Config.NATIVE_IMAGE))) {
      return new NativeGenerator(context, config);
    }
    final Properties quarkusConfiguration = getQuarkusConfiguration(context.getProject());
    return from(quarkusConfiguration).orElseGet(() -> fromFiles(context.getProject(), quarkusConfiguration))
      .apply(context, config);
  }

  static Optional<BiFunction<GeneratorContext, GeneratorConfig, ? extends QuarkusNestedGenerator>> from(Properties properties) {
    final String packageType = properties.getProperty("quarkus.package.type");
    if (packageType == null) {
      return Optional.empty();
    }
    switch (packageType) {
      case "native":
        return Optional.of(NativeGenerator::new);
      case "fast-jar":
        return Optional.of(FastJarGenerator::new);
      case "legacy-jar":
        return Optional.of(LegacyJarGenerator::new);
      case "uber-jar":
        return Optional.of(UberJarGenerator::new);
      default:
        return Optional.empty();
    }
  }

  static BiFunction<GeneratorContext, GeneratorConfig, ? extends QuarkusNestedGenerator> fromFiles(JavaProject project, Properties properties) {
    final String runnerSuffix = runnerSuffix(properties);
    if (hasFileThatEndsWith(project, runnerSuffix + ".jar") && new File(project.getBuildDirectory(), "lib").exists()) {
      return LegacyJarGenerator::new;
    } else if(hasFileThatEndsWith(project, runnerSuffix + ".jar")) {
      return UberJarGenerator::new;
    } else if(hasFileThatEndsWith(project, runnerSuffix)) {
      return NativeGenerator::new;
    }
    return FastJarGenerator::new;
  }

  static boolean hasFileThatEndsWith(JavaProject project, String suffix) {
    File buildDir = project.getBuildDirectory();
    String[] file = buildDir.list((dir, name) -> name.endsWith(suffix));
    return file != null && file.length > 0;
  }
}
