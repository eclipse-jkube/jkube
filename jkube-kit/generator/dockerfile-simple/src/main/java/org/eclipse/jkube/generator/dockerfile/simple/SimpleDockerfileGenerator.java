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
package org.eclipse.jkube.generator.dockerfile.simple;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.generator.api.support.BaseGenerator;
import org.eclipse.jkube.kit.build.api.helper.ImageNameFormatter;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;

import java.io.File;
import java.util.List;

import static org.eclipse.jkube.generator.dockerfile.simple.SimpleDockerfileUtil.addSimpleDockerfileConfig;
import static org.eclipse.jkube.generator.dockerfile.simple.SimpleDockerfileUtil.createSimpleDockerfileConfig;
import static org.eclipse.jkube.generator.dockerfile.simple.SimpleDockerfileUtil.getTopLevelDockerfile;
import static org.eclipse.jkube.generator.dockerfile.simple.SimpleDockerfileUtil.isSimpleDockerFileMode;
import static org.eclipse.jkube.kit.common.util.BuildReferenceDateUtil.getBuildTimestamp;
import static org.eclipse.jkube.kit.common.util.PropertiesUtil.getValueFromProperties;

public class SimpleDockerfileGenerator extends BaseGenerator {
  public SimpleDockerfileGenerator(GeneratorContext context) {
    super(context, "dockerfile-simple");
  }

  @Override
  public boolean isApplicable(List<ImageConfiguration> configs) {
    return shouldAddGeneratedImageConfiguration(configs) &&
        isSimpleDockerFileMode(getContext().getProject().getBaseDirectory());
  }

  @Override
  public List<ImageConfiguration> customize(List<ImageConfiguration> configs, boolean prePackagePhase) {
    ImageNameFormatter imageNameFormatter = new ImageNameFormatter(getContext().getProject(),
        getBuildTimestamp(null, null, getContext().getProject().getBuildDirectory().getAbsolutePath(),
        "docker/build.timestamp"));
    File topDockerfile = getTopLevelDockerfile(getContext().getProject().getBaseDirectory());
    String defaultImageName = imageNameFormatter.format(getValueFromProperties(getContext().getProject().getProperties(),
      PROPERTY_JKUBE_IMAGE_NAME, PROPERTY_JKUBE_GENERATOR_NAME));
    if (configs.isEmpty()) {
      configs.add(createSimpleDockerfileConfig(topDockerfile, defaultImageName));
    } else if (configs.size() == 1 && configs.get(0).getBuildConfiguration() == null) {
      configs.set(0, addSimpleDockerfileConfig(configs.get(0), topDockerfile));
    }
    return configs;
  }
}
