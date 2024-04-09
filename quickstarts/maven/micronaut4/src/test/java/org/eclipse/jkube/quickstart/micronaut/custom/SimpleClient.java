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
package org.eclipse.jkube.quickstart.micronaut.custom;

import io.micronaut.http.annotation.Get;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.core.async.annotation.SingleResult;
import org.reactivestreams.Publisher;

@Client("/")
public interface SimpleClient {

  @Get
  @SingleResult
  Publisher<String> get();

  @Get(uri = "hello/{name}")
  @SingleResult
  Publisher<String> hello(String name);
}
