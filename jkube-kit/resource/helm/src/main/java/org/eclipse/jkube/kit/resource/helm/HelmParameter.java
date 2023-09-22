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
package org.eclipse.jkube.kit.resource.helm;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.fabric8.openshift.api.model.Parameter;
import lombok.Getter;

public class HelmParameter {

  @Getter
  private final Parameter parameter;

  @JsonCreator
  public HelmParameter(Parameter parameter) {
    this.parameter = parameter;
  }

  public String getHelmName() {
    return parameter.getName();
  }
}
