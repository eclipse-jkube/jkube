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
package org.eclipse.jkube.gradle.plugin.task;

import org.eclipse.jkube.gradle.plugin.KubernetesExtension;
import org.eclipse.jkube.kit.resource.helm.HelmConfig;

import javax.inject.Inject;

import static org.eclipse.jkube.kit.resource.helm.HelmServiceUtil.initHelmConfig;

public class KubernetesHelmDependencyUpdateTask extends AbstractJKubeTask {
    @Inject
    public KubernetesHelmDependencyUpdateTask(Class<? extends KubernetesExtension> extensionClass) {
        super(extensionClass);
        setDescription("Update the on-disk dependencies to mirror Chart.yaml");
    }

    @Override
    public void run() {
        if (kubernetesExtension.getSkipOrDefault()) {
            return;
        }
        try {
            final HelmConfig helm = initHelmConfig(kubernetesExtension.getDefaultHelmType(), kubernetesExtension.javaProject,
                    kubernetesExtension.getKubernetesTemplateOrDefault(),
                    kubernetesExtension.helm)
                    .build();
            jKubeServiceHub.getHelmService().dependencyUpdate(helm);
        } catch (Exception exp) {
            kitLogger.error("Error performing helm dependency update", exp);
            throw new IllegalStateException(exp.getMessage(), exp);
        }
    }
}