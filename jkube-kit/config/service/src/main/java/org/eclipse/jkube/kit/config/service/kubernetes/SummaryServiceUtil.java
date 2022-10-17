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
package org.eclipse.jkube.kit.config.service.kubernetes;

import org.eclipse.jkube.kit.config.service.JKubeServiceHub;

public class SummaryServiceUtil {
  private SummaryServiceUtil() { }

  public static void printSummary(JKubeServiceHub jKubeServiceHub) {
    if (jKubeServiceHub != null) {
      jKubeServiceHub.getSummaryService().printSummary(jKubeServiceHub.getConfiguration().getBasedir());
      jKubeServiceHub.getSummaryService().clear();
    }
  }

  public static void printSummaryIfLastExecuting(JKubeServiceHub jKubeServiceHub, String current, String lastExecuting) {
    if (lastExecuting != null && lastExecuting.equals(current)) {
      printSummary(jKubeServiceHub);
    }
  }

  public static void handleExceptionAndSummary(JKubeServiceHub jKubeServiceHub, Exception exception) {
    if (jKubeServiceHub != null) {
      jKubeServiceHub.getSummaryService().setFailureAndCause(exception.getMessage());
      printSummary(jKubeServiceHub);
    }
  }
}
