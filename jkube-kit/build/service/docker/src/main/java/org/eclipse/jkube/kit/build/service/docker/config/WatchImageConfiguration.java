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
package org.eclipse.jkube.kit.build.service.docker.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Optional;

/**
 * Configuration for watching on image changes
 */
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@EqualsAndHashCode
public class WatchImageConfiguration implements Serializable {

  private static final long serialVersionUID = -8837447095092135706L;

  private Integer interval;
  private WatchMode mode;
  private String postGoal;
  private String postExec;

  public int getInterval() {
    return interval != null ? interval : 5000;
  }

  public Integer getIntervalRaw() {
    return interval;
  }

  public static class WatchImageConfigurationBuilder {
    public WatchImageConfigurationBuilder modeString(String modeString) {
      mode = Optional.ofNullable(modeString).map(String::toLowerCase).map(WatchMode::valueOf).orElse(null);
      return this;
    }
  }

}
