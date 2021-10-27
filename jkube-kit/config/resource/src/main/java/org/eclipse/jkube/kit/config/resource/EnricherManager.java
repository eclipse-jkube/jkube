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
package org.eclipse.jkube.kit.config.resource;

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;

public interface EnricherManager {

  void createDefaultResources(PlatformMode platformMode, final KubernetesListBuilder builder);
  void createDefaultResources(PlatformMode platformMode, ProcessorConfig enricherConfig, final KubernetesListBuilder builder);

  void enrich(PlatformMode platformMode, final KubernetesListBuilder builder);
  void enrich(PlatformMode platformMode, final ProcessorConfig enricherConfig, final KubernetesListBuilder builder);

}
