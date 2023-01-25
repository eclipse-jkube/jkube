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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.generator.javaexec.JavaExecGenerator;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.Arguments;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.quarkus.QuarkusMode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import static org.eclipse.jkube.quarkus.QuarkusUtils.extractPort;
import static org.eclipse.jkube.quarkus.QuarkusUtils.findSingleFileThatEndsWith;
import static org.eclipse.jkube.quarkus.QuarkusUtils.getQuarkusConfiguration;
import static org.eclipse.jkube.quarkus.QuarkusUtils.hasQuarkusPlugin;
import static org.eclipse.jkube.quarkus.QuarkusUtils.runnerSuffix;

public class QuarkusGenerator extends JavaExecGenerator {

  public static final String QUARKUS = "quarkus";

  public QuarkusGenerator(GeneratorContext context) {
    super(context, QUARKUS);
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
    if (isNativeImage()) {
      return "0";
    }
    return super.getDefaultJolokiaPort();
  }

  @Override
  protected String getDefaultPrometheusPort() {
    if (isNativeImage()) {
      return "0";
    }
    return super.getDefaultPrometheusPort();
  }

  @Override
  protected String getFromAsConfigured() {
    if (isNativeImage()) {
      return Optional.ofNullable(super.getFromAsConfigured()).orElse(getNativeFrom());
    }
    return super.getFromAsConfigured();
  }

  @Override
  protected AssemblyConfiguration createAssembly() {
    if (isNativeImage()) {
      return QuarkusMode.NATIVE.getAssembly().createAssemblyConfiguration(this);
    }
    return QuarkusMode.from(getProject()).getAssembly().createAssemblyConfiguration(this);
  }

  @Override
  protected String getBuildWorkdir() {
    if (isNativeImage()) {
      return "/";
    }
    return getConfig(JavaExecGenerator.Config.TARGET_DIR);
  }

  @Override
  protected Arguments getBuildEntryPoint() {
    if (isNativeImage()) {
      final Arguments.ArgumentsBuilder ab = Arguments.builder();
      ab.execArgument("./" + findSingleFileThatEndsWith(getProject(), runnerSuffix(getQuarkusConfiguration(getProject()))));
      getExtraJavaOptions().forEach(ab::execArgument);
      return ab.build();
    }
    return null;
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

  private boolean isNativeImage() {
    return Boolean.parseBoolean(getConfig(Config.NATIVE_IMAGE)) || QuarkusMode.from(getProject()) == QuarkusMode.NATIVE;
  }

  private String getNativeFrom() {
    if (getContext().getRuntimeMode() != RuntimeMode.OPENSHIFT) {
      return "registry.access.redhat.com/ubi8/ubi-minimal:8.6";
    }
    return "quay.io/quarkus/ubi-quarkus-native-binary-s2i:1.0";
  }

  @Override
  protected boolean isFatJar() {
    return QuarkusMode.from(getProject()).isFatJar();
  }

}
