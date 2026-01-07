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
package org.eclipse.jkube.kit.build.service.docker;

/**
 * Default implementation of {@link Environment} that delegates to {@code System.getenv()}.
 */
public class SystemEnvironment implements Environment {
  private static final SystemEnvironment INSTANCE = new SystemEnvironment();

  /**
   * Returns the singleton instance of SystemEnvironment.
   *
   * @return the singleton instance
   */
  public static SystemEnvironment getInstance() {
    return INSTANCE;
  }

  /**
   * Package-private constructor to allow testing while discouraging external instantiation.
   * Use {@link #getInstance()} instead.
   */
  SystemEnvironment() {
  }

  @Override
  public String getEnv(String name) {
    return System.getenv(name);
  }
}