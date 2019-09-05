/**
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.jshift.enricher.generic.openshift;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceSpec;
import io.fabric8.openshift.api.model.Project;
import io.fabric8.openshift.api.model.ProjectBuilder;
import io.fabric8.openshift.api.model.ProjectSpec;
import io.fabric8.openshift.api.model.ProjectStatus;
import io.fabric8.openshift.api.model.ProjectStatusBuilder;
import io.jshift.kit.config.resource.PlatformMode;
import io.jshift.maven.enricher.api.BaseEnricher;
import io.jshift.maven.enricher.api.MavenEnricherContext;

import static io.jshift.maven.enricher.api.util.KubernetesResourceUtil.removeItemFromKubernetesBuilder;


public class ProjectEnricher extends BaseEnricher {
    static final String ENRICHER_NAME = "jshift-openshift-project";

    public ProjectEnricher(MavenEnricherContext context) {
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