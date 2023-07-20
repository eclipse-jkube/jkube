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
package org.eclipse.jkube.kit.enricher.handler;

import java.util.function.Supplier;

import io.fabric8.kubernetes.api.model.HasMetadata;
import lombok.Getter;
import org.eclipse.jkube.kit.common.util.LazyBuilder;

@Getter
public class ControllerHandlerLazyBuilder<T extends HasMetadata> extends LazyBuilder.VoidLazyBuilder<ControllerHandler<T>> {

  private final Class<T> controllerHandlerType;

  public ControllerHandlerLazyBuilder(Class<T> controllerHandlerType, Supplier<ControllerHandler<T>> build) {
    super(build);
    this.controllerHandlerType = controllerHandlerType;
  }
}
