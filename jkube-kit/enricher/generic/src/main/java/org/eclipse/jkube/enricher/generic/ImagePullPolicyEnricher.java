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

import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.BaseEnricher;
import org.eclipse.jkube.kit.enricher.api.EnricherContext;

public class ImagePullPolicyEnricher extends BaseEnricher {
  public ImagePullPolicyEnricher(EnricherContext context) {
    super(context, "jkube-imagepullpolicy");
  }

  @Override
  public void enrich(PlatformMode platformMode, KubernetesListBuilder builder) {
    String imagePullPolicy = getValueFromConfig(JKUBE_ENFORCED_IMAGE_PULL_POLICY, null);
    if (StringUtils.isNotBlank(imagePullPolicy)) {
      builder.accept(new TypedVisitor<ContainerBuilder>() {
        @Override
        public void visit(ContainerBuilder containerBuilder) {
          containerBuilder.withImagePullPolicy(imagePullPolicy);
        }
      });
    }
  }

}
