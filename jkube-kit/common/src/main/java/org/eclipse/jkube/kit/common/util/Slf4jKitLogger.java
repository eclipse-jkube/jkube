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
package org.eclipse.jkube.kit.common.util;

import org.eclipse.jkube.kit.common.KitLogger;
import org.slf4j.Logger;

@SuppressWarnings("squid:S2629")
public class Slf4jKitLogger implements KitLogger {

  private final Logger delegate;

  public Slf4jKitLogger(Logger delegate) {
    this.delegate = delegate;
  }

  @Override
  public void debug(String format, Object... params) {
    delegate.debug(String.format(format, params));
  }

  @Override
  public void info(String format, Object... params) {
    delegate.info(String.format(format, params));
  }

  @Override
  public void warn(String format, Object... params) {
    delegate.warn(String.format(format, params));
  }

  @Override
  public void error(String format, Object... params) {
    delegate.error(String.format(format, params));
  }

  @Override
  public boolean isDebugEnabled() {
    return delegate.isDebugEnabled();
  }

}
