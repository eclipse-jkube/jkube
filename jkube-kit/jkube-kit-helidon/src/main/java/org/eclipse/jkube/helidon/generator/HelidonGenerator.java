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

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.generator.javaexec.JavaExecGenerator;
import org.eclipse.jkube.kit.common.Arguments;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;

import java.util.List;
import java.util.Optional;

import static org.eclipse.jkube.helidon.HelidonUtils.extractPort;
import static org.eclipse.jkube.helidon.HelidonUtils.getHelidonConfiguration;
import static org.eclipse.jkube.helidon.HelidonUtils.hasHelidonDependencies;

public class HelidonGenerator extends JavaExecGenerator {
  public static final String HELIDON = "helidon";
  private final HelidonNestedGenerator nestedGenerator;

  public HelidonGenerator(GeneratorContext context) {
    super(context, HELIDON);
    nestedGenerator = HelidonNestedGenerator.from(context, getGeneratorConfig());
  }

  @Override
  public boolean isApplicable(List<ImageConfiguration> configs) {
    return shouldAddGeneratedImageConfiguration(configs) && hasHelidonDependencies(getProject());
  }

  @Override
  protected AssemblyConfiguration createAssembly() {
    return nestedGenerator.createAssemblyConfiguration();
  }

  @Override
  protected String getBuildWorkdir() {
    return nestedGenerator.getBuildWorkdir();
  }

  @Override
  protected String getFromAsConfigured() {
    return Optional.ofNullable(super.getFromAsConfigured()).orElse(nestedGenerator.getFrom());
  }

  @Override
  protected Arguments getBuildEntryPoint() {
    return nestedGenerator.getBuildEntryPoint();
  }

  @Override
  protected String getDefaultWebPort() {
    return extractPort(getHelidonConfiguration(getProject()), super.getDefaultWebPort());
  }

  @Override
  protected String getDefaultJolokiaPort() {
    return nestedGenerator.getDefaultJolokiaPort();
  }

  @Override
  protected String getDefaultPrometheusPort() {
    return nestedGenerator.getDefaultPrometheusPort();
  }
}
