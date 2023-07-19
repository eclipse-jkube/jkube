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
package org.eclipse.jkube.springboot.generator;

import org.eclipse.jkube.generator.api.GeneratorConfig;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.Arguments;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;

import static org.eclipse.jkube.generator.javaexec.JavaExecGenerator.JOLOKIA_PORT_DEFAULT;
import static org.eclipse.jkube.generator.javaexec.JavaExecGenerator.PROMETHEUS_PORT_DEFAULT;

public interface SpringBootNestedGenerator {
  JavaProject getProject();

  default AssemblyConfiguration createAssemblyConfiguration() {
    return null;
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

  static SpringBootNestedGenerator from(GeneratorContext generatorContext, GeneratorConfig generatorConfig) {
    return new FatJarGenerator(generatorContext, generatorConfig);
  }
}
