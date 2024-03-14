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
package org.eclipse.jkube.kit.config.image;

import java.util.List;

public interface GeneratorManager {
  /**
   * Process provided set of ImageConfigurations. This processing may include filtering, merging with
   * opinionated defaults and validating finally generated ImageConfigurations.
   *
   * @param imageConfigs list of ImageConfigurations to process
   * @return processed list of ImageConfigurations
   */
  List<ImageConfiguration> generateAndMerge(List<ImageConfiguration> imageConfigs);
}
