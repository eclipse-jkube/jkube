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

import java.util.Map;

/**
 * Default implementation of {@link Environment} that delegates to {@code System.getenv()}.
 *
 * <p>Uses the initialization-on-demand holder pattern for lazy, thread-safe singleton initialization.
 */
public class SystemEnvironment implements Environment {

  /**
   * Returns the singleton instance of SystemEnvironment.
   *
   * @return the singleton instance
   */
  public static SystemEnvironment getInstance() {
    return SystemEnvironmentHolder.INSTANCE;
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

  @Override
  public Map<String, String> getEnvMap() {
    return System.getenv();
  }

  /**
   * Initialization-on-demand holder idiom.
   * The JVM defers initialization of the holder class until it is actually used,
   * and because the class initialization is thread-safe, this provides a lazy,
   * thread-safe singleton without requiring synchronization.
   *
   * @see <a href="https://en.wikipedia.org/wiki/Initialization-on-demand_holder_idiom">Initialization-on-demand holder idiom</a>
   */
  private static final class SystemEnvironmentHolder {
    static final SystemEnvironment INSTANCE = new SystemEnvironment();

    private SystemEnvironmentHolder() {
    }
  }
}

