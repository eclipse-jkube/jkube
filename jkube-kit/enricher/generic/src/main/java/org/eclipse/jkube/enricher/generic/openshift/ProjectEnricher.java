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
package org.eclipse.jkube.enricher.generic.openshift;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceSpec;
import io.fabric8.openshift.api.model.Project;
import io.fabric8.openshift.api.model.ProjectBuilder;
import io.fabric8.openshift.api.model.ProjectSpec;
import io.fabric8.openshift.api.model.ProjectStatus;
import io.fabric8.openshift.api.model.ProjectStatusBuilder;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.BaseEnricher;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil.removeItemFromKubernetesBuilder;


public class ProjectEnricher extends BaseEnricher {
    static final String ENRICHER_NAME = "jkube-openshift-project";

    public ProjectEnricher(JKubeEnricherContext context) {
        super(context, ENRICHER_NAME);
    }

    @Override
    public void create(PlatformMode platformMode, KubernetesListBuilder builder) {
        if(platformMode == PlatformMode.openshift) {
            for(HasMetadata item : builder.buildItems()) {
                if(item instanceof Namespace) {
                    Project project = convertToProject((Namespace) item);
                    removeItemFromKubernetesBuilder(builder, item);
                    builder.addToProjectItems(project);
                }
            }
        }
    }

    private Project convertToProject(Namespace namespace) {

        ProjectBuilder builder = new ProjectBuilder();
        builder.withMetadata(namespace.getMetadata());

        if (namespace.getSpec() != null) {
            NamespaceSpec namespaceSpec = namespace.getSpec();
            ProjectSpec projectSpec = new ProjectSpec();
            if (namespaceSpec.getFinalizers() != null) {
                projectSpec.setFinalizers(namespaceSpec.getFinalizers());
            }
            builder.withSpec(projectSpec);
        }

        if (namespace.getStatus() != null) {
            ProjectStatus status = new ProjectStatusBuilder()
                    .withPhase(namespace.getStatus().getPhase()).build();

            builder.withStatus(status);
        }

        return builder.build();
    }
}