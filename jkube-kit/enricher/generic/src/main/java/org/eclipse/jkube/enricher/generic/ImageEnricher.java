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
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.config.resource.ControllerResourceConfig;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.BaseEnricher;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil.extractContainerName;
import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil.isContainerImage;

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
 */
public class ImageEnricher extends BaseEnricher {

    public ImageEnricher(JKubeEnricherContext buildContext) {
        super(buildContext, "jkube-image");
    }

    @AllArgsConstructor
    private enum Config implements Configs.Config {
        // What pull policy to use when fetching images
        PULL_POLICY("pullPolicy");

        @Getter
        protected String key;
    }

    @Override
    public void create(PlatformMode platformMode, KubernetesListBuilder builder) {
        if (!hasImageConfiguration()) {
            log.verbose("No images resolved. Skipping ...");
            return;
        }

        // Ensure that all controller have template specs
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
                    item.buildSpec() == null ? item.withNewSpec() : item.editSpec();
                ReplicationControllerSpecFluent.TemplateNested<ReplicationControllerFluent.SpecNested<ReplicationControllerBuilder>>
                    template =
                    spec.buildTemplate() == null ? spec.withNewTemplate() : spec.editTemplate();
                template.endTemplate().endSpec();
            }
        });
    }

    private void ensureTemplateSpecsInRelicaSet(KubernetesListBuilder builder) {
        builder.accept(new TypedVisitor<ReplicaSetBuilder>() {
            @Override
            public void visit(ReplicaSetBuilder item) {
                ReplicaSetFluent.SpecNested<ReplicaSetBuilder> spec =
                    item.buildSpec() == null ? item.withNewSpec() : item.editSpec();
                ReplicaSetSpecFluent.TemplateNested<ReplicaSetFluent.SpecNested<ReplicaSetBuilder>> template =
                    spec.buildTemplate() == null ? spec.withNewTemplate() : spec.editTemplate();
                template.endTemplate().endSpec();
            }
        });
    }

    private void ensureTemplateSpecsInDeployments(KubernetesListBuilder builder) {
        builder.accept(new TypedVisitor<DeploymentBuilder>() {
            @Override
            public void visit(DeploymentBuilder item) {
                DeploymentFluent.SpecNested<DeploymentBuilder> spec =
                    item.buildSpec() == null ? item.withNewSpec() : item.editSpec();
                DeploymentSpecFluent.TemplateNested<DeploymentFluent.SpecNested<DeploymentBuilder>> template =
                    spec.buildTemplate() == null ? spec.withNewTemplate() : spec.editTemplate();
                template.endTemplate().endSpec();
            }
        });
    }

    private void ensureTemplateSpecsInDaemonSet(KubernetesListBuilder builder) {
        builder.accept(new TypedVisitor<DaemonSetBuilder>() {
            @Override
            public void visit(DaemonSetBuilder item) {
                DaemonSetFluent.SpecNested<DaemonSetBuilder> spec =
                        item.buildSpec() == null ? item.withNewSpec() : item.editSpec();
                DaemonSetSpecFluent.TemplateNested<DaemonSetFluent.SpecNested<DaemonSetBuilder>> template =
                        spec.buildTemplate() == null ? spec.withNewTemplate() : spec.editTemplate();
                template.endTemplate().endSpec();
            }
        });
    }

    private void ensureTemplateSpecsInStatefulSet(KubernetesListBuilder builder) {
        builder.accept(new TypedVisitor<StatefulSetBuilder>() {
            @Override
            public void visit(StatefulSetBuilder item) {
                StatefulSetFluent.SpecNested<StatefulSetBuilder> spec =
                        item.buildSpec() == null ? item.withNewSpec() : item.editSpec();
                StatefulSetSpecFluent.TemplateNested<StatefulSetFluent.SpecNested<StatefulSetBuilder>> template =
                        spec.buildTemplate() == null ? spec.withNewTemplate() : spec.editTemplate();
                template.endTemplate().endSpec();
            }
        });
    }

    private void ensureTemplateSpecsInDeploymentConfig(KubernetesListBuilder builder) {
        builder.accept(new TypedVisitor<DeploymentConfigBuilder>() {
            @Override
            public void visit(DeploymentConfigBuilder item) {
                DeploymentConfigFluent.SpecNested<DeploymentConfigBuilder> spec =
                        item.buildSpec() == null ? item.withNewSpec() : item.editSpec();
                DeploymentConfigSpecFluent.TemplateNested<DeploymentConfigFluent.SpecNested<DeploymentConfigBuilder>> template =
                        spec.buildTemplate() == null ? spec.withNewTemplate() : spec.editTemplate();
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
                    templateBuilder.buildSpec() == null ? templateBuilder.withNewSpec() : templateBuilder.editSpec();

                List<Container> containers = podSpec.buildContainers();
                if (containers == null) {
                    containers = new ArrayList<>();
                }
                getContext().getSummaryService().addToEnrichers(getName());
                mergeImageConfigurationWithContainerSpec(containers);
                podSpec.withContainers(containers).endSpec();
            }
        });
    }

    // Add missing information to the given containers as found
    // configured
    private void mergeImageConfigurationWithContainerSpec(List<Container> containers) {
        int idx = 0;
        for (ImageConfiguration image : getImages()) {
            if (isContainerImage(image, getControllerResourceConfig())) {
                Container container = getContainer(idx, containers);
                mergeImagePullPolicy(image, container);
                mergeImage(image, container);
                mergeContainerName(image, container);
                mergeEnvVariables(container);
                idx++;
            }
        }
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
            if (StringUtils.isNotBlank(imageConfiguration.getRegistry())) {
                log.verbose("Using registry %s for the image", imageConfiguration.getRegistry());
            }
            final String imageFullName = containerImageName(imageConfiguration);
            log.verbose("Setting image %s", imageFullName);
            container.setImage(imageFullName);
        }
    }

    private void mergeImagePullPolicy(ImageConfiguration imageConfiguration, Container container) {
        if (StringUtils.isBlank(container.getImagePullPolicy())) {
            String policy = getConfig(Config.PULL_POLICY);
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
        Optional.ofNullable(getControllerResourceConfig()).map(ControllerResourceConfig::getEnv).ifPresent(resourceEnv -> {
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

                EnvVar oldEnvVar = containerEnvVars.stream()
                  .filter(e -> e.getName().equals(newEnvVar.getName()))
                  .findFirst().orElse(null);
                if (oldEnvVar == null) {
                    containerEnvVars.add(newEnvVar);
                } else if (!newEnvVar.getValue().equals(oldEnvVar.getValue())) {
                    log.warn(
                      "Environment variable %s will not be overridden: trying to set the value %s, but its actual value is %s",
                      newEnvVar.getName(), newEnvVar.getValue(), oldEnvVar.getValue());
                }
            }
        });
    }

    static String containerImageName(ImageConfiguration imageConfiguration) {
        String prefix = "";
        if (StringUtils.isNotBlank(imageConfiguration.getRegistry())) {
            prefix = imageConfiguration.getRegistry() + "/";
        }
        return prefix + imageConfiguration.getName();
    }
}
