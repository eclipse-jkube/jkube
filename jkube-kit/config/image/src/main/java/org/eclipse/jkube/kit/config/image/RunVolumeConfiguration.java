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
package org.eclipse.jkube.kit.config.image;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.*;

/**
 * Run configuration for volumes.
 *
 * @author roland
 */
@SuppressWarnings("JavaDoc")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@EqualsAndHashCode
public class RunVolumeConfiguration implements Serializable {

  /**
   * List of images names from where volumes are mounted
   *
   * @return images
   */
  private List<String> from;
  /**
   * List of bind parameters for binding/mounting host directories
   * into the container
   *
   * @return list of bind specs
   */
  private List<String> bind;

  public static class RunVolumeConfigurationBuilder {
    public RunVolumeConfigurationBuilder from(List<String> args) {
      if (args != null) {
        if (from == null) {
          from = new ArrayList<>();
        }
        from.addAll(args);
      }
      return this;
    }

    public RunVolumeConfigurationBuilder bind(List<String> args) {
      if (args != null) {
        if (bind == null) {
          bind = new ArrayList<>();
        }
        bind.addAll(args);
      }
      return this;
    }
  }
}

