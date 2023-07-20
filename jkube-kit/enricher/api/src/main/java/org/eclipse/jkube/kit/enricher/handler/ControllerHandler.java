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

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.resource.ControllerResourceConfig;

import java.util.List;

public interface ControllerHandler<T extends HasMetadata> {

  T get(ControllerResourceConfig config, List<ImageConfiguration> images);

  PodTemplateSpec getPodTemplateSpec(ControllerResourceConfig config, List<ImageConfiguration> images);

  PodTemplateSpec getPodTemplate(T controller);

  void overrideReplicas(KubernetesListBuilder resources, int replicas);
}
