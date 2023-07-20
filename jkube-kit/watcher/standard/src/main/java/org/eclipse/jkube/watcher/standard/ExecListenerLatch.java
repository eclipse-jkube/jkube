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
package org.eclipse.jkube.watcher.standard;

import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ExecListenerLatch implements ExecListener {

  private final CountDownLatch cdl;
  private final AtomicInteger closeCode;
  private final AtomicReference<String> closeReason;
  private final AtomicReference<Status> exitStatus;

  public ExecListenerLatch() {
    cdl = new CountDownLatch(1);
    closeCode = new AtomicInteger(-1);
    closeReason = new AtomicReference<>();
    exitStatus = new AtomicReference<>();
  }

  @Override
  public void onFailure(Throwable t, Response response) {
    cdl.countDown();
  }

  @Override
  public void onClose(int code, String reason) {
    closeCode.set(code);
    closeReason.set(reason);
    cdl.countDown();
  }

  @Override
  public void onExit(int code, Status status) {
    exitStatus.set(status);
  }

  public boolean await(long timeout, TimeUnit timeUnit) throws InterruptedException {
    return cdl.await(timeout, timeUnit);
  }

  /**
   * Endpoints MAY use the following pre-defined status codes when sending
   * a Close frame:
   *
   * <p> - 1000 indicates a normal closure, meaning that the purpose for
   * which the connection was established has been fulfilled.
   *
   * <p> - 1001 indicates that an endpoint is "going away", such as a server
   * going down or a browser having navigated away from a page.
   *
   * <p> - 1002 indicates that an endpoint is terminating the connection due
   * to a protocol error.
   *
   * <p> - 1003 indicates that an endpoint is terminating the connection
   * because it has received a type of data it cannot accept (e.g., an
   * endpoint that understands only text data MAY send this if it
   * receives a binary message).
   *
   * @return The <a href="http://tools.ietf.org/html/rfc6455#section-7.4.1">RFC-compliant</a>
   */
  public int getCloseCode() {
    return closeCode.get();
  }

  public String getCloseReason() {
    return closeReason.get();
  }

  public Status getExitStatus() {
    return exitStatus.get();
  }

}
