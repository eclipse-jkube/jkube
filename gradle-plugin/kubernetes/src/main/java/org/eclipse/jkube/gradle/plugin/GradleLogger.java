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
package org.eclipse.jkube.gradle.plugin;

import org.eclipse.jkube.kit.common.util.Slf4jKitLogger;

import org.gradle.api.logging.Logger;

public class GradleLogger extends Slf4jKitLogger {

  private final Logger delegate;
  private final String prefix;

  public GradleLogger(Logger delegate, String prefix) {
    super(delegate);
    this.delegate = delegate;
    this.prefix = prefix;
  }

  @Override
  public void info(String format, Object... params) {
    delegate.lifecycle(withPrefix(String.format(format, params)));
  }

  @Override
  public void debug(String format, Object... params) {
    super.debug(withPrefix(format), params);
  }

  @Override
  public void warn(String format, Object... params) {
    super.warn(withPrefix(format), params);
  }

  @Override
  public void error(String format, Object... params) {
    super.error(withPrefix(format), params);
  }

  private String withPrefix(String message) {
    return prefix + message;
  }
}
