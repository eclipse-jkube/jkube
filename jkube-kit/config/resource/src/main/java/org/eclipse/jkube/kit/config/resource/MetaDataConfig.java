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

import java.util.Properties;

/**
 * Configuration for labels or annotations
 *
 * @author roland
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@EqualsAndHashCode
public class MetaDataConfig {
  /**
   * Labels or annotations which should be applied to every object
   */
  private Properties all;

  /**
   * Labels or annotation for a Pod within a controller or deployment
   */
  private Properties pod;

  /**
   * Labels or annotations for replica sets (or replication controller)
   */
  private Properties replicaSet;

  /**
   * Labels or annotation for services
   */
  private Properties service;

  /**
   * Labels or annotations for deployment or deployment configs
   */
  private Properties ingress;

  /**
   * Labels or annotations for deployment or deployment configs
   */
  private Properties deployment;

}
