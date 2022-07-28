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
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;
import org.eclipse.jkube.kit.common.util.SummaryUtil;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.BaseEnricher;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * Enricher for adding a "name" to the metadata to various objects we create.
 * The name is only added if not already set.
 *
 * @author roland
 */
public class NameEnricher extends BaseEnricher {

  public NameEnricher(JKubeEnricherContext buildContext) {
    super(buildContext, "jkube-name");
  }

  @AllArgsConstructor
  private enum Config implements Configs.Config {
    NAME("name");

    @Getter
    protected String key;
  }

  @Override
  public void create(PlatformMode platformMode, KubernetesListBuilder builder) {
    final String configuredName = getConfig(Config.NAME);
    final String defaultName = JKubeProjectUtil.createDefaultResourceName(getContext().getGav().getSanitizedArtifactId());
    SummaryUtil.addToEnrichers(getName());
    builder.accept(new TypedVisitor<ObjectMetaBuilder>() {
      @Override
      public void visit(ObjectMetaBuilder resource) {
        if (StringUtils.isNotBlank(configuredName)) {
          resource.withName(configuredName);
        } else if (StringUtils.isBlank(resource.getName())) {
          resource.withName(defaultName);
        }
      }
    });
  }

}
