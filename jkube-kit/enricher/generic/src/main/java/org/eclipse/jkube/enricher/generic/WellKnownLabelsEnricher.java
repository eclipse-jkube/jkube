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
package org.eclipse.jkube.enricher.generic;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.config.resource.GroupArtifactVersion;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

public class WellKnownLabelsEnricher extends AbstractLabelEnricher {
  public static final String KUBERNETES_APP_LABEL = "app.kubernetes.io/";
  private static final String WELL_KNOWN_LABELS_PROPERTY = "jkube.kubernetes.well-known-labels";

  @AllArgsConstructor
  private enum Config implements Configs.Config {
    ENABLED("enabled", "true"),
    APP_NAME("name", null),
    APP_VERSION("version", null),
    APP_COMPONENT("component", null),
    APP_PART_OF("partOf", null),
    APP_MANAGED_BY("managedBy", "jkube");

    @Getter
    protected String key;
    @Getter
    protected String defaultValue;
  }

  public WellKnownLabelsEnricher(JKubeEnricherContext buildContext) {
    super(buildContext, "jkube-well-known-labels");
  }

  private boolean shouldAddWellKnownLabels() {
    return Configs.asBoolean(getConfigWithFallback(Config.ENABLED, WELL_KNOWN_LABELS_PROPERTY, "true"));
  }

  @Override
  public Map<String, String> createLabels(boolean withoutVersion) {
    Map<String, String> ret = new HashMap<>();
    if (!shouldAddWellKnownLabels()) {
      return ret;
    }

    final GroupArtifactVersion groupArtifactVersion = getContext().getGav();
    ret.putAll(addWellKnownLabelFromConfig(Config.APP_NAME, "name", groupArtifactVersion.getArtifactId()));
    if (!withoutVersion) {
      ret.putAll(addWellKnownLabelFromConfig(Config.APP_VERSION, "version", groupArtifactVersion.getVersion()));
    }
    ret.putAll(addWellKnownLabelFromConfig(Config.APP_PART_OF, "part-of", groupArtifactVersion.getGroupId()));
    ret.putAll(addWellKnownLabelFromConfig(Config.APP_MANAGED_BY, "managed-by", "jkube"));
    ret.putAll(addWellKnownLabelFromConfig(Config.APP_COMPONENT, "component", null));
    return ret;
  }

  private Map<String, String> addWellKnownLabelFromConfig(Configs.Config key, String labelKey, String defaultValue) {
    Map<String, String> entryMap = new HashMap<>();
    String value = getConfig(key, defaultValue);
    if (StringUtils.isNotBlank(value)) {
      entryMap.put(KUBERNETES_APP_LABEL + labelKey, value);
    }
    return entryMap;
  }
}
