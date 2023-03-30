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
package org.eclipse.jkube.quickstarts.gradle.micronaut;

import javax.validation.constraints.Size;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;

@Controller
public class Resource {

  @Get
  @Produces(MediaType.TEXT_PLAIN)
  public String get() {
    return "Hello from Micronaut deployed with JKube!";
  }

  @Get(uri = "hello/{name}")
  @Produces(MediaType.TEXT_PLAIN)
  public String get(@Size(min = 3) String name) {
    return String.format("Hello %s!", name);
  }
}
