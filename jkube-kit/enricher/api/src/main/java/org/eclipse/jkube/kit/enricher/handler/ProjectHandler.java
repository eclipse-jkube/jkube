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
package org.eclipse.jkube.kit.enricher.handler;

import org.eclipse.jkube.kit.common.util.KubernetesHelper;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.openshift.api.model.Project;
import io.fabric8.openshift.api.model.ProjectBuilder;

public class ProjectHandler {
    public Project getProject(String namespace) {
        return new ProjectBuilder().withMetadata(createProjectMetaData(namespace)).withNewStatus().withPhase("Active").endStatus().build();
    }

    private ObjectMeta createProjectMetaData(String namespace) {
        return new ObjectMetaBuilder()
                .withName(KubernetesHelper.validateKubernetesId(namespace, "project name"))
                .build();
    }
}
