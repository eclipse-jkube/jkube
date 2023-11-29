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

@EnabledOnOs(OS.WINDOWS)
class WindowsBuildPackCliDownloaderTest extends AbstractBuildPackCliDownloaderTest {
  @Override
  String getApplicablePackBinary() {
    return "pack.bat";
  }

  @Override
  String getInvalidApplicablePackBinary() {
    return "invalid-pack.bat";
  }

  @Override
  String getPlatform() {
    return "Windows 11";
  }

  @Override
  String getProcessorArchitecture() {
    return "amd64";
  }
}
