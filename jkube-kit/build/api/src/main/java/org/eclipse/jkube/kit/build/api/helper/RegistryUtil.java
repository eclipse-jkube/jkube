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
package org.eclipse.jkube.kit.build.api.helper;

import org.eclipse.jkube.kit.common.RegistryConfig;
import org.eclipse.jkube.kit.common.util.EnvUtil;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.ImageName;

public class RegistryUtil {
  private RegistryUtil() { }

  public static String getApplicablePushRegistryFrom(ImageConfiguration imageConfiguration, RegistryConfig registryConfig) {
    ImageName imageName = new ImageName(imageConfiguration.getName());
    return EnvUtil.firstRegistryOf(imageName.getRegistry(),
        imageConfiguration.getRegistry(),
        registryConfig.getRegistry());
  }

  public static String getApplicablePullRegistryFrom(String fromImage, RegistryConfig registryConfig) {
    ImageName imageName = new ImageName(fromImage);
    return EnvUtil.firstRegistryOf(imageName.getRegistry(), registryConfig.getRegistry());
  }
}
