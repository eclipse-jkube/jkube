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
package io.jkube.enricher.generic;

import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpecFluent;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.ReplicationControllerFluent;
import io.fabric8.kubernetes.api.model.ReplicationControllerSpecFluent;
import io.fabric8.kubernetes.api.model.apps.DaemonSetBuilder;
import io.fabric8.kubernetes.api.model.apps.DaemonSetFluent;
import io.fabric8.kubernetes.api.model.apps.DaemonSetSpecFluent;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentFluent;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpecFluent;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetBuilder;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetFluent;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetSpecFluent;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetFluent;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpecFluent;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.fabric8.openshift.api.model.DeploymentConfigFluent;
import io.fabric8.openshift.api.model.DeploymentConfigSpecFluent;
import io.jkube.kit.build.service.docker.ImageConfiguration;
import io.jkube.kit.common.Configs;
import io.jkube.kit.config.resource.PlatformMode;
import io.jkube.kit.config.resource.ResourceConfig;
import io.jkube.maven.enricher.api.BaseEnricher;
import io.jkube.maven.enricher.api.MavenEnricherContext;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static io.jkube.maven.enricher.api.util.KubernetesResourceUtil.extractContainerName;


/**
 * Merge in image configuration like the image name into ReplicaSet and ReplicationController's
 * Pod specification.
 *
 * <ul>
 *     <li>The full image name is set as <code>image</code></li>
 *     <li>An image alias is set as <code>name</code></li>
 *     <li>The pull policy <code>imagePullPolicy</code> is set according to the given configuration. If no
 *         configuration is set, the default is "IfNotPresent" for release versions, and "Always" for snapshot versions</li>
 * </ul>
 *
 * Any already configured container in the pod spec is updated if the property is not set.
 *
 * @author roland
 * @since 25/05/16
 */
public class ImageEnricher extends BaseEnricher {

    public ImageEnricher(MavenEnricherContext buildContext) {
        super(buildContext, "jkube-image");
    }

    // Available configuration keys
    private enum Config implements Configs.Key {
        // What pull policy to use when fetching images
        pullPolicy;

        public String def() { return d; } protected String d;
    }

    @Override
    public void create(PlatformMode platformMode, KubernetesListBuilder builder) {
        if (!hasImageConfiguration()) {
            log.verbose("No images resolved. Skipping ...");
            return;
        }

        // Ensure that all contoller have template specs
        ensureTemplateSpecs(builder);

        // Update containers in template specs
        updateContainers(builder);
    }

    // ============================================================================================================

    private void ensureTemplateSpecs(KubernetesListBuilder builder) {
        ensureTemplateSpecsInReplicationControllers(builder);
        ensureTemplateSpecsInRelicaSet(builder);
        ensureTemplateSpecsInDeployments(builder);
        ensureTemplateSpecsInDaemonSet(builder);
        ensureTemplateSpecsInStatefulSet(builder);
        ensureTemplateSpecsInDeploymentConfig(builder);
    }

    private void ensureTemplateSpecsInReplicationControllers(KubernetesListBuilder builder) {
        builder.accept(new TypedVisitor<ReplicationControllerBuilder>() {
            @Override
            public void visit(ReplicationControllerBuilder item) {
                ReplicationControllerFluent.SpecNested<ReplicationControllerBuilder> spec =
                    item.getSpec() == null ? item.withNewSpec() : item.editSpec();
                ReplicationControllerSpecFluent.TemplateNested<ReplicationControllerFluent.SpecNested<ReplicationControllerBuilder>>
                    template =
                    spec.getTemplate() == null ? spec.withNewTemplate() : spec.editTemplate();
                template.endTemplate().endSpec();
            }
        });
    }

    private void ensureTemplateSpecsInRelicaSet(KubernetesListBuilder builder) {
        builder.accept(new TypedVisitor<ReplicaSetBuilder>() {
            @Override
            public void visit(ReplicaSetBuilder item) {
                ReplicaSetFluent.SpecNested<ReplicaSetBuilder> spec =
                    item.getSpec() == null ? item.withNewSpec() : item.editSpec();
                ReplicaSetSpecFluent.TemplateNested<ReplicaSetFluent.SpecNested<ReplicaSetBuilder>> template =
                    spec.getTemplate() == null ? spec.withNewTemplate() : spec.editTemplate();
                template.endTemplate().endSpec();
            }
        });
    }

    private void ensureTemplateSpecsInDeployments(KubernetesListBuilder builder) {
        builder.accept(new TypedVisitor<DeploymentBuilder>() {
            @Override
            public void visit(DeploymentBuilder item) {
                DeploymentFluent.SpecNested<DeploymentBuilder> spec =
                    item.getSpec() == null ? item.withNewSpec() : item.editSpec();
                DeploymentSpecFluent.TemplateNested<DeploymentFluent.SpecNested<DeploymentBuilder>> template =
                    spec.getTemplate() == null ? spec.withNewTemplate() : spec.editTemplate();
                template.endTemplate().endSpec();
            }
        });
    }

    private void ensureTemplateSpecsInDaemonSet(KubernetesListBuilder builder) {
        builder.accept(new TypedVisitor<DaemonSetBuilder>() {
            @Override
            public void visit(DaemonSetBuilder item) {
                DaemonSetFluent.SpecNested<DaemonSetBuilder> spec =
                        item.getSpec() == null ? item.withNewSpec() : item.editSpec();
                DaemonSetSpecFluent.TemplateNested<DaemonSetFluent.SpecNested<DaemonSetBuilder>> template =
                        spec.getTemplate() == null ? spec.withNewTemplate() : spec.editTemplate();
                template.endTemplate().endSpec();
            }
        });
    }

    private void ensureTemplateSpecsInStatefulSet(KubernetesListBuilder builder) {
        builder.accept(new TypedVisitor<StatefulSetBuilder>() {
            @Override
            public void visit(StatefulSetBuilder item) {
                StatefulSetFluent.SpecNested<StatefulSetBuilder> spec =
                        item.getSpec() == null ? item.withNewSpec() : item.editSpec();
                StatefulSetSpecFluent.TemplateNested<StatefulSetFluent.SpecNested<StatefulSetBuilder>> template =
                        spec.getTemplate() == null ? spec.withNewTemplate() : spec.editTemplate();
                template.endTemplate().endSpec();
            }
        });
    }

    private void ensureTemplateSpecsInDeploymentConfig(KubernetesListBuilder builder) {
        builder.accept(new TypedVisitor<DeploymentConfigBuilder>() {
            @Override
            public void visit(DeploymentConfigBuilder item) {
                DeploymentConfigFluent.SpecNested<DeploymentConfigBuilder> spec =
                        item.getSpec() == null ? item.withNewSpec() : item.editSpec();
                DeploymentConfigSpecFluent.TemplateNested<DeploymentConfigFluent.SpecNested<DeploymentConfigBuilder>> template =
                        spec.getTemplate() == null ? spec.withNewTemplate() : spec.editTemplate();
                template.endTemplate().endSpec();
            }
        });
    }


    // ============================================================================================================

    private void updateContainers(KubernetesListBuilder builder) {
        builder.accept(new TypedVisitor<PodTemplateSpecBuilder>() {
            @Override
            public void visit(PodTemplateSpecBuilder templateBuilder) {
                PodTemplateSpecFluent.SpecNested<PodTemplateSpecBuilder> podSpec =
                    templateBuilder.getSpec() == null ? templateBuilder.withNewSpec() : templateBuilder.editSpec();

                List<Container> containers = podSpec.getContainers();
                if (containers == null) {
                    containers = new ArrayList<Container>();
                }
                mergeImageConfigurationWithContainerSpec(containers);
                podSpec.withContainers(containers).endSpec();
            }
        });
    }

    // Add missing information to the given containers as found
    // configured
    private void mergeImageConfigurationWithContainerSpec(List<Container> containers) {
        getImages().ifPresent(images -> {
            int idx = 0;
            for (ImageConfiguration image : images) {
                Container container = getContainer(idx, containers);
                mergeImagePullPolicy(image, container);
                mergeImage(image, container);
                mergeContainerName(image, container);
                mergeEnvVariables(container);
                idx++;
            }
        });
    }

    private Container getContainer(int idx, List<Container> containers) {
        Container container;
        if (idx < containers.size()) {
            container = containers.get(idx);
        } else {
            // Pad with new containers if missing
            container = new Container();
            containers.add(container);
        }
        return container;
    }

    private void mergeContainerName(ImageConfiguration imageConfiguration, Container container) {
        if (StringUtils.isBlank(container.getName())) {
            String containerName = extractContainerName(getContext().getGav(), imageConfiguration);
            log.verbose("Setting container name %s",containerName);
            container.setName(containerName);
        }
    }

    private void mergeImage(ImageConfiguration imageConfiguration, Container container) {
        if (StringUtils.isBlank(container.getImage())) {
            String prefix = "";
            if (StringUtils.isNotBlank(imageConfiguration.getRegistry())) {
                log.verbose("Using registry %s for the image", imageConfiguration.getRegistry());
                prefix = imageConfiguration.getRegistry() + "/";
            }
            String imageFullName = prefix + imageConfiguration.getName();
            log.verbose("Setting image %s", imageFullName);
            container.setImage(imageFullName);
        }
    }

    private void mergeImagePullPolicy(ImageConfiguration imageConfiguration, Container container) {
        if (StringUtils.isBlank(container.getImagePullPolicy())) {
            String policy = getConfig(Config.pullPolicy);
            if (policy == null) {
                policy = "IfNotPresent";
                String imageName = imageConfiguration.getName();
                if (StringUtils.isNotBlank(imageName) && imageName.endsWith(":latest")) {
                    policy = "Always";
                }
            }
            container.setImagePullPolicy(policy);
        }
    }

    private void mergeEnvVariables(Container container) {
        getConfiguration().getResource().flatMap(ResourceConfig::getEnv).ifPresent(resourceEnv -> {
            List<EnvVar> containerEnvVars = container.getEnv();
            if (containerEnvVars == null) {
                containerEnvVars = new LinkedList<>();
                container.setEnv(containerEnvVars);
            }

            for (Map.Entry<String, String> resourceEnvEntry : resourceEnv.entrySet()) {
                EnvVar newEnvVar =
                    new EnvVarBuilder()
                        .withName(resourceEnvEntry.getKey())
                        .withValue(resourceEnvEntry.getValue())
                        .build();
                if (!hasEnvWithName(containerEnvVars, newEnvVar.getName())) {
                    containerEnvVars.add(newEnvVar);
                } else {
                    log.warn(
                        "Environment variable %s will not be overridden: trying to set the value %s, but its actual value is %s",
                        newEnvVar.getName(), newEnvVar.getValue(), getEnvValue(containerEnvVars, newEnvVar.getName()));
                }
            }
        });
    }

    private String getEnvValue(List<EnvVar> envVars, String name) {
        for (EnvVar var : envVars) {
            if (var.getName().equals(name)) {
                return var.getValue();
            }
        }
        return "(not found)";
    }

    private boolean hasEnvWithName(List<EnvVar> envVars, String name) {
        return envVars.stream().anyMatch(e -> e.getName().equals(name));
    }

}
