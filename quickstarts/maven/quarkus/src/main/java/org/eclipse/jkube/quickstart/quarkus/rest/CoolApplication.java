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
package org.eclipse.jkube.quickstart.quarkus.rest;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class CoolApplication {

  private final String applicationName;
  private final String message;

  public CoolApplication(String applicationName, String message) {
    this.applicationName = applicationName;
    this.message = message;
  }

  public String getApplicationName() {
    return applicationName;
  }

  public String getMessage() {
    return message;
  }
}
