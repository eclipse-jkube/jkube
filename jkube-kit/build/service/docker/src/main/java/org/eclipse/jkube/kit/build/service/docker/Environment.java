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
 * Abstraction for accessing environment variables.
 * This interface allows for easier testing by providing a mockable/stubbable alternative
 * to direct {@code System.getenv()} calls.
 */
public interface Environment {
  /**
   * Gets the value of the specified environment variable.
   * An environment variable is a system-dependent external named value.
   *
   * @param name the name of the environment variable
   * @return the string value of the variable, or {@code null} if the variable is not defined
   */
  String getEnv(String name);
}