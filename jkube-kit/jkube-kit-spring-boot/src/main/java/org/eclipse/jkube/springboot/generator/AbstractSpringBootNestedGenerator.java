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
import org.eclipse.jkube.generator.api.GeneratorMode;
import org.eclipse.jkube.generator.javaexec.JavaExecGenerator;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;
import org.eclipse.jkube.kit.common.util.SpringBootUtil;

import java.util.Map;
import java.util.function.Function;

public abstract class AbstractSpringBootNestedGenerator implements SpringBootNestedGenerator {

  private final GeneratorContext generatorContext;
  private final GeneratorConfig generatorConfig;

  AbstractSpringBootNestedGenerator(GeneratorContext generatorContext, GeneratorConfig generatorConfig) {
    this.generatorContext = generatorContext;
    this.generatorConfig = generatorConfig;
  }

  @Override
  public final JavaProject getProject() {
    return generatorContext.getProject();
  }

  @Override
  public String getBuildWorkdir() {
    return generatorConfig.get(JavaExecGenerator.Config.TARGET_DIR);
  }

  @Override
  public String getTargetDir() {
    return generatorConfig.get(JavaExecGenerator.Config.TARGET_DIR);
  }

  @Override
  public Map<String, String> getEnv(Function<Boolean, Map<String, String>> javaExecEnvSupplier, boolean prePackagePhase) {
    final Map<String, String> res = javaExecEnvSupplier.apply(prePackagePhase);
    if (generatorContext.getGeneratorMode() == GeneratorMode.WATCH) {
      // adding dev tools token to env variables to prevent override during recompile
      final String secret = SpringBootUtil.getSpringBootApplicationProperties(
          SpringBootUtil.getSpringBootActiveProfile(getProject()),
          JKubeProjectUtil.getClassLoader(getProject()))
        .getProperty(SpringBootUtil.DEV_TOOLS_REMOTE_SECRET);
      if (secret != null) {
        res.put(SpringBootUtil.DEV_TOOLS_REMOTE_SECRET_ENV, secret);
      }
    }
    return res;
  }

  protected KitLogger getLogger() {
    return generatorContext.getLogger();
  }
}
