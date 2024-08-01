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
package org.eclipse.jkube.micronaut.generator;

import org.eclipse.jkube.generator.api.GeneratorConfig;
import org.eclipse.jkube.generator.api.GeneratorContext;

import java.util.Map;
import java.util.function.Function;

public class FatJarGenerator extends AbstractMicronautNestedGenerator {
  public FatJarGenerator(GeneratorContext generatorContext, GeneratorConfig generatorConfig) {
    super(generatorContext, generatorConfig);
  }

  @Override
  public Map<String, String> getEnv(Function<Boolean, Map<String, String>> javaExecEnvSupplier, boolean prePackagePhase) {
    return javaExecEnvSupplier.apply(prePackagePhase);
  }
}
