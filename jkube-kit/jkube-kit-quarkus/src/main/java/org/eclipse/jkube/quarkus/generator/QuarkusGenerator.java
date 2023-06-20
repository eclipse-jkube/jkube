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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.generator.javaexec.JavaExecGenerator;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.common.Arguments;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import static org.eclipse.jkube.quarkus.QuarkusUtils.extractPort;
import static org.eclipse.jkube.quarkus.QuarkusUtils.getQuarkusConfiguration;
import static org.eclipse.jkube.quarkus.QuarkusUtils.hasQuarkusPlugin;

public class QuarkusGenerator extends JavaExecGenerator {

  public static final String QUARKUS = "quarkus";

  private final QuarkusNestedGenerator nestedGenerator;

  public QuarkusGenerator(GeneratorContext context) {
    super(context, QUARKUS);
    nestedGenerator = QuarkusNestedGenerator.from(context, getGeneratorConfig());
  }

  @AllArgsConstructor
  public enum Config implements Configs.Config {

    /**
     * Whether to add native image or plain java image
     * @deprecated no longer necessary, inferred from Quarkus properties
     */
    @Deprecated
    NATIVE_IMAGE("nativeImage", "false");

    @Getter
    protected String key;
    @Getter
    protected String defaultValue;
  }

  @Override
  public boolean isApplicable(List<ImageConfiguration> configs) {
    return shouldAddGeneratedImageConfiguration(configs) && hasQuarkusPlugin(getProject());
  }

  @Override
  protected String getDefaultWebPort() {
    return extractPort(getProject(), getQuarkusConfiguration(getProject()), super.getDefaultWebPort());
  }

  @Override
  protected String getDefaultJolokiaPort() {
    return nestedGenerator.getDefaultJolokiaPort();
  }

  @Override
  protected String getDefaultPrometheusPort() {
    return nestedGenerator.getDefaultPrometheusPort();
  }

  @Override
  protected String getFromAsConfigured() {
    return Optional.ofNullable(super.getFromAsConfigured()).orElse(nestedGenerator.getFrom());
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
  protected Arguments getBuildEntryPoint() {
    return nestedGenerator.getBuildEntryPoint();
  }

  @Override
  protected Map<String, String> getEnv(boolean prePackagePhase) {
    final Map<String, String> env = new HashMap<>();
    env.put(JAVA_OPTIONS, StringUtils.join(getJavaOptions(), " "));
    return env;
  }

  private static List<String> getJavaOptions() {
    return Collections.singletonList("-Dquarkus.http.host=0.0.0.0");
  }

  @Override
  protected boolean isFatJar() {
    return nestedGenerator.isFatJar();
  }

}
