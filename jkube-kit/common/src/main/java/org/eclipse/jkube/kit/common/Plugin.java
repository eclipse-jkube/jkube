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
package org.eclipse.jkube.kit.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Model class that represents a Maven compatible plugin.
 */
@SuppressWarnings("JavaDoc")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class Plugin implements Serializable {

  private static final long serialVersionUID = -7421259106320247639L;

  /**
   * Maven group ID.
   *
   * @param groupId New maven group ID for the plugin.
   * @return The maven group ID for the plugin.
   */
  private String groupId;
  /**
   * Maven artifact ID.
   *
   * @param artifactId New maven artifact ID for the plugin.
   * @return The maven artifact ID for the plugin.
   */
  private String artifactId;
  /**
   * Maven version.
   *
   * @param version New maven version for the plugin.
   * @return The maven version for the plugin.
   */
  private String version;
  /**
   * Plugin configuration.
   *
   * @param configuration New configuration for the plugin.
   * @return The configuration for the plugin.
   */
  private Map<String, Object> configuration;
  /**
   * Plugin executions. (i.e. Maven plugin executions).
   *
   * @param executions New executions for the plugin.
   * @return The plugin executions.
   */
  private List<String> executions;
}
