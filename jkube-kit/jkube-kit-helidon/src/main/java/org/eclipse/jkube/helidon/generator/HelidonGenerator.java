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
import static org.eclipse.jkube.helidon.HelidonUtils.hasHelidonGraalNativeImageExtension;

public class HelidonGenerator extends JavaExecGenerator {
  public static final String HELIDON = "helidon";
  public static final String DEFAULT_NATIVE_BASE_IMAGE = "registry.access.redhat.com/ubi8/ubi-minimal:8.7-1107";

  public HelidonGenerator(GeneratorContext context) {
    super(context, HELIDON);
  }

  @Override
  public boolean isApplicable(List<ImageConfiguration> configs) {
    return shouldAddGeneratedImageConfiguration(configs) && hasHelidonDependencies(getProject());
  }

  @Override
  protected AssemblyConfiguration createAssembly() {
    if (isNativeImage()) {
      return HelidonAssemblies.NATIVE.createAssemblyConfiguration(this);
    }
    return HelidonAssemblies.STANDARD.createAssemblyConfiguration(this);
  }

  @Override
  protected String getBuildWorkdir() {
    if (isNativeImage()) {
      return "/";
    }
    return getConfig(JavaExecGenerator.Config.TARGET_DIR);
  }

  @Override
  protected String getFromAsConfigured() {
    if (isNativeImage()) {
      return Optional.ofNullable(super.getFromAsConfigured()).orElse(getNativeFrom());
    }
    return super.getFromAsConfigured();
  }

  @Override
  protected Arguments getBuildEntryPoint() {
    if (isNativeImage()) {
      final Arguments.ArgumentsBuilder ab = Arguments.builder();
      ab.execArgument("./" + getProject().getArtifactId());
      getExtraJavaOptions().forEach(ab::execArgument);
      return ab.build();
    }
    return null;
  }

  @Override
  protected String getDefaultWebPort() {
    return extractPort(getHelidonConfiguration(getProject()), super.getDefaultWebPort());
  }

  @Override
  protected String getDefaultJolokiaPort() {
    return isNativeImage() ? "0" : super.getDefaultJolokiaPort();
  }

  @Override
  protected String getDefaultPrometheusPort() {
    return isNativeImage() ? "0" : super.getDefaultPrometheusPort();
  }

  private String getNativeFrom() {
    return DEFAULT_NATIVE_BASE_IMAGE;
  }

  private boolean isNativeImage() {
    return hasHelidonGraalNativeImageExtension(getProject());
  }
}
