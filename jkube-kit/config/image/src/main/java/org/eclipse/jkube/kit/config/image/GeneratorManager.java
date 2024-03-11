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
   * Customize a given list of image configurations
   *
   * @param imageConfigs list of image configurations
   * @return Modified list of image configurations
   */
  List<ImageConfiguration> generate(List<ImageConfiguration> imageConfigs);
}
