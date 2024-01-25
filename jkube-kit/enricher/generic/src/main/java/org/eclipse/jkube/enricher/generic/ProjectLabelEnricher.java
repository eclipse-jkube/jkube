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
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.config.resource.GroupArtifactVersion;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Add project labels to any object.
 * For selectors, the 'version' part is removed.
 * <p> The following labels are added:
 * <ul>
 *   <li>version</li>
 *   <li>app</li>
 *   <li>group</li>
 *   <li>provider (is set to jkube)</li>
 * </ul>
 *
 * <p> The "app" label can be replaced with the (old) "project" label using the "useProjectLabel" configuration option.
 *
 * <p> The project labels which are already specified in the input fragments are not overridden by the enricher.
 *
 * @author roland
 */
public class ProjectLabelEnricher extends AbstractLabelEnricher {

    public static final String LABEL_PROVIDER = "provider";

    @AllArgsConstructor
    private enum Config implements Configs.Config {
        USE_PROJECT_LABEL("useProjectLabel", "false"),
        APP("app", null),
        GROUP("group", null),
        VERSION("version", null),
        PROVIDER(LABEL_PROVIDER, "jkube");

        @Getter
        protected String key;
        @Getter
        protected String defaultValue;
    }

    public ProjectLabelEnricher(JKubeEnricherContext buildContext) {
        super(buildContext, "jkube-project-label");
    }

    @Override
    public Map<String, String> createLabels(boolean withoutVersion, Map<String, String> labelsViaResourceConfig) {
        Map<String, String> ret = new HashMap<>();

        boolean enableProjectLabel = Configs.asBoolean(getConfig(Config.USE_PROJECT_LABEL));
        final GroupArtifactVersion groupArtifactVersion = getContext().getGav();
        if (enableProjectLabel) {
            ret.putAll(addProjectLabelFromApplicableSource(null, "project", groupArtifactVersion.getArtifactId(), labelsViaResourceConfig));
        } else {
            ret.putAll(addProjectLabelFromApplicableSource(Config.APP, "app", groupArtifactVersion.getArtifactId(), labelsViaResourceConfig));
        }

        ret.putAll(addProjectLabelFromApplicableSource(Config.GROUP, "group", groupArtifactVersion.getGroupId(), labelsViaResourceConfig));
        ret.putAll(addProjectLabelFromApplicableSource(Config.PROVIDER, LABEL_PROVIDER, null, labelsViaResourceConfig));
        if (!withoutVersion) {
            ret.putAll(addProjectLabelFromApplicableSource(Config.VERSION, "version", groupArtifactVersion.getVersion(), labelsViaResourceConfig));
        }
        return ret;
    }

    private Map<String, String> addProjectLabelFromApplicableSource(Configs.Config key, String labelKey, String defaultValue, Map<String, String> labelsViaResourceConfig) {
        Map<String, String> entryMap = new HashMap<>();
        String appLabelValueFromConfig = Optional.ofNullable(key)
            .map(k -> getConfig(k, defaultValue))
            .orElse(defaultValue);
        entryMap.put(labelKey, labelsViaResourceConfig.getOrDefault(labelKey, appLabelValueFromConfig));
        return entryMap;
    }
}
