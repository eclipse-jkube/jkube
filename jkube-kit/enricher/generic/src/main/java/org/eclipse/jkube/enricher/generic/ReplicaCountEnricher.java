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
package org.eclipse.jkube.enricher.generic;

import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.BaseEnricher;
import org.eclipse.jkube.kit.enricher.api.EnricherContext;

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;

/**
 * This enricher fixes replica count for Kubernetes/Openshift resources whenever a -Djkube.replicas=n parameter is
 * provided.
 */
public class ReplicaCountEnricher extends BaseEnricher {

  public ReplicaCountEnricher(EnricherContext context) {
    super(context, "jkube-replicas");
  }

  @Override
  public void enrich(PlatformMode platformMode, KubernetesListBuilder builder) {
    Integer replicas = Configs.asInteger(getValueFromConfig(JKUBE_ENFORCED_REPLICAS, null));
    if (replicas != null) {
      getContext().getHandlerHub().getControllerHandlers()
          .forEach(controller -> controller.get().overrideReplicas(builder, replicas));
    }
  }

}
