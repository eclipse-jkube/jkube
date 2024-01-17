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
package org.eclipse.jkube.kit.service.buildpacks;

import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@EnabledOnOs({OS.LINUX, OS.MAC})
public class LinuxArm64BuildPackCliDownloaderTest extends AbstractBuildPackCliDownloaderTest {
  @Override
  String getApplicablePackBinary() {
    return "pack";
  }

  @Override
  String getInvalidApplicablePackBinary() {
    return "invalid-pack";
  }

  @Override
  String getPlatform() {
    return "Linux";
  }

  @Override
  String getProcessorArchitecture() {
    return "aarch64";
  }
}
