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
package org.eclipse.jkube.quickstart.gradle.micronaut.custom;

import javax.validation.constraints.Size;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.reactivex.Single;

@Controller
public class Resource {

  @Get
  public Single<String> get() {
    return Single.just("Hello from Micronaut deployed with JKube!");
  }

  @Get(uri = "hello/{name}")
  public Single<String> get(@Size(min = 3) String name) {
    return Single.just(String.format("Hello %s!", name));
  }
}

