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
package org.eclipse.jkube.kit.config.resource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Singular;

import java.util.List;
import java.util.Map;

@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@EqualsAndHashCode
public class ControllerResourceConfig {
  private Map<String, String> env;
  @Singular
  private List<VolumeConfig> volumes;
  @Singular
  private List<InitContainerConfig> initContainers;
  private String controllerName;
  private ProbeConfig liveness;
  private ProbeConfig readiness;
  private ProbeConfig startup;
  private boolean containerPrivileged;
  private String imagePullPolicy;
  private Integer replicas;
  private String restartPolicy;
  private RequestsLimitsConfig resourceRequestsLimits;
}
