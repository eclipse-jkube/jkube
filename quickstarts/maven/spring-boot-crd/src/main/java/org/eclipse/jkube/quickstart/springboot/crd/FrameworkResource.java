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
package org.eclipse.jkube.quickstart.springboot.crd;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class FrameworkResource {

  private final KubernetesClient kubernetesClient;
  private final CustomResourceDefinitionContext crdContext;

  @Autowired
  public FrameworkResource(KubernetesClient kubernetesClient, CustomResourceDefinitionContext crdContext) {
    this.kubernetesClient = kubernetesClient;
    this.crdContext = crdContext;
  }

  @SuppressWarnings("unchecked")
  @GetMapping
  public List<String> get() {
    return ((List<Map<String, Object>>)kubernetesClient.customResource(crdContext).list().get("items")).stream()
        .map(m -> (String)m.get("name")).collect(Collectors.toList());
  }
}
