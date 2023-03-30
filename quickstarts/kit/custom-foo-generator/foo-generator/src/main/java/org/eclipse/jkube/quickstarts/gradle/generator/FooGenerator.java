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
package org.eclipse.jkube.quickstarts.gradle.generator;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.generator.api.support.BaseGenerator;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;

import java.util.List;

public class FooGenerator extends BaseGenerator {

  public FooGenerator(GeneratorContext context) {
    super(context, "foo");
  }

  @AllArgsConstructor
  public enum Config implements Configs.Config {
    ENABLED("enabled", "true");

    @Getter
    protected String key;
    @Getter(AccessLevel.PUBLIC)
    protected String defaultValue;
  }

  @Override
  public boolean isApplicable(List<ImageConfiguration> configs) {
    return Boolean.parseBoolean(getConfig(Config.ENABLED));
  }

  @Override
  public List<ImageConfiguration> customize(List<ImageConfiguration> existingConfigs, boolean prePackagePhase) {
    for (ImageConfiguration imageConfiguration : existingConfigs) {
      getContext().getLogger().info("Add Environment variable to ImageConfigurations");
      imageConfiguration.setBuild(addFooEnvVariable(imageConfiguration.getBuildConfiguration()));
    }
    return existingConfigs;
  }

  private BuildConfiguration addFooEnvVariable(BuildConfiguration buildConfiguration) {
    return buildConfiguration.toBuilder()
        .putEnv("foo", "fooval")
        .build();
  }
}
