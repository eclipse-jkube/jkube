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

public class JKubeException extends RuntimeException {

  public JKubeException(String message) {
    super(message);
  }

  public JKubeException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Convert exception into an instance of JKubeException
   *
   * @param message message of exception
   * @param cause actual cause of exception
   * @return {@link RuntimeException} wrapping actual cause
   */
  public static RuntimeException launderThrowable(String message, Throwable cause) {
    if (cause instanceof RuntimeException) {
      return ((RuntimeException) cause);
    } else if (cause instanceof Error) {
      throw ((Error) cause);
    } else if (cause instanceof InterruptedException) {
      Thread.currentThread().interrupt();
    }
    throw new JKubeException(message, cause);
  }
}
