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
package org.eclipse.jkube.helidon.generator;

import org.eclipse.jkube.generator.api.GeneratorConfig;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.generator.javaexec.JavaExecGenerator;
import org.eclipse.jkube.kit.common.JavaProject;

public abstract class AbstractHelidonNestedGenerator implements HelidonNestedGenerator {

  private final GeneratorContext generatorContext;
  private final GeneratorConfig generatorConfig;

  AbstractHelidonNestedGenerator(GeneratorContext generatorContext, GeneratorConfig generatorConfig) {
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
}
