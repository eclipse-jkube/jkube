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
package org.eclipse.jkube.helidon.generator;

import org.eclipse.jkube.generator.api.FromSelector;
import org.eclipse.jkube.generator.javaexec.JavaExecGenerator;
import org.eclipse.jkube.kit.common.Arguments;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;

import java.util.function.Function;

import static org.eclipse.jkube.generator.javaexec.JavaExecGenerator.JOLOKIA_PORT_DEFAULT;
import static org.eclipse.jkube.generator.javaexec.JavaExecGenerator.PROMETHEUS_PORT_DEFAULT;
import static org.eclipse.jkube.helidon.HelidonUtils.hasHelidonGraalNativeImageExtension;

public enum HelidonGenerators {

  NATIVE(
    HelidonFromSelector.NATIVE,
    HelidonAssembly.NATIVE,
    helidonGenerator -> "/",
    helidonGenerator -> Arguments.builder()
      .execArgument("./" + helidonGenerator.getContext().getProject().getArtifactId())
      .build(),
    "0",
    "0"),
  STANDARD(
    HelidonFromSelector.STANDARD,
    HelidonAssembly.STANDARD,
    helidonGenerator -> helidonGenerator.getConfig(JavaExecGenerator.Config.TARGET_DIR),
    helidonGenerator -> null,
    JOLOKIA_PORT_DEFAULT,
    PROMETHEUS_PORT_DEFAULT);

  private final HelidonFromSelector fromSelector;
  private final HelidonAssembly assembly;
  private final Function<HelidonGenerator, String> buildWorkdir;
  private final Function<HelidonGenerator, Arguments> buildEntryPoint;
  private final String jolokiaPort;
  private final String prometheusPort;

  HelidonGenerators(
    HelidonFromSelector fromSelector, HelidonAssembly assembly, Function<HelidonGenerator, String> buildWorkdir,
    Function<HelidonGenerator, Arguments> buildEntryPoint, String jolokiaPort, String prometheusPort) {
    this.fromSelector = fromSelector;
    this.assembly = assembly;
    this.buildWorkdir = buildWorkdir;
    this.buildEntryPoint = buildEntryPoint;
    this.jolokiaPort = jolokiaPort;
    this.prometheusPort = prometheusPort;
  }


  public FromSelector fromSelector(HelidonGenerator helidonGenerator) {
    return fromSelector.fromSelector(helidonGenerator);
  }

  public AssemblyConfiguration createAssemblyConfiguration(HelidonGenerator helidonGenerator) {
    return assembly.createAssemblyConfiguration(helidonGenerator);
  }

  public String getBuildWorkdir(HelidonGenerator helidonGenerator) {
    return buildWorkdir.apply(helidonGenerator);
  }

  public Arguments getBuildEntryPoint(HelidonGenerator helidonGenerator) {
    return buildEntryPoint.apply(helidonGenerator);
  }

  public String getDefaultJolokiaPort() {
    return jolokiaPort;
  }

  public String getDefaultPrometheusPort() {
    return prometheusPort;
  }

  public static HelidonGenerators from(JavaProject project) {
    if (hasHelidonGraalNativeImageExtension(project)) {
      return NATIVE;
    }
    return STANDARD;
  }
}
