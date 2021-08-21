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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.openshift.api.model.Template;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.BaseEnricher;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.api.model.KindAndName;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Enricher for embedding dependency descriptors to single package.
 *
 * @author jimmidyson
 */
public class DependencyEnricher extends BaseEnricher {
    private static final String DEPENDENCY_KUBERNETES_YAML = "META-INF/jkube/kubernetes.yml";
    private static final String DEPENDENCY_KUBERNETES_TEMPLATE_YAML = "META-INF/jkube/k8s-template.yml";
    private static final String DEPENDENCY_OPENSHIFT_YAML = "META-INF/jkube/openshift.yml";

    private Set<URI> kubernetesDependencyArtifacts = new HashSet<URI>();
    private Set<URI> kubernetesTemplateDependencyArtifacts = new HashSet<URI>();
    private Set<URI> openshiftDependencyArtifacts = new HashSet<URI>();

    @AllArgsConstructor
    private enum Config implements Configs.Config {

        INCLUDE_TRANSITIVE("includeTransitive", "true"),
        INCLUDE_PLUGIN("includePlugin", "true");

        @Getter
        protected String key;
        @Getter
        protected String defaultValue;
    }

    public DependencyEnricher(JKubeEnricherContext buildContext) throws URISyntaxException {
        super(buildContext, "jkube-dependency");

        addArtifactsWithYaml(kubernetesDependencyArtifacts, DEPENDENCY_KUBERNETES_YAML);
        addArtifactsWithYaml(kubernetesTemplateDependencyArtifacts, DEPENDENCY_KUBERNETES_TEMPLATE_YAML);
        addArtifactsWithYaml(openshiftDependencyArtifacts, DEPENDENCY_OPENSHIFT_YAML);

    }

    private void addArtifactsWithYaml(Set<URI> artifactSet, String dependencyYaml) throws URISyntaxException {
        final List<Dependency> artifacts = getContext().getDependencies(isIncludeTransitive());

        for (Dependency artifact : artifacts) {
            if ("compile".equals(artifact.getScope()) && "jar".equals(artifact.getType())) {
                File file = artifact.getFile();
                try {
                    URI uri = new URI("jar:" + file.toURI() + "!/" + dependencyYaml);
                    artifactSet.add(uri);
                } catch (URISyntaxException e) {
                    getLog().debug("Failed to create URL for %s: %s", file, e);
                }
            }
        }
        // lets look on the current plugin classpath too
        if (isIncludePlugin()) {
            Enumeration<URL> resources = null;
            try {
                resources = getClass().getClassLoader().getResources(dependencyYaml);
            } catch (IOException e) {
                getLog().error("Could not find %s on the classpath: %s", dependencyYaml, e);
            }
            if (resources != null) {
                while (resources.hasMoreElements()) {
                    URL url = resources.nextElement();
                    URI uri = url.toURI();
                    artifactSet.add(uri);
                }
            }
        }
    }

    @Override
    public void enrich(PlatformMode platformMode, final KubernetesListBuilder builder) {
        switch (platformMode) {
            case kubernetes:
                enrichKubernetes(builder);
                break;
            case openshift:
                enrichOpenShift(builder);
                break;
            default:
                break;
        }
    }

    private void enrichKubernetes(final KubernetesListBuilder builder) {
        final List<HasMetadata> kubernetesItems = new ArrayList<>();
        processArtifactSetResources(this.kubernetesDependencyArtifacts, items -> {
            kubernetesItems.addAll(Arrays.asList(items.toArray(new HasMetadata[items.size()])));
            return null;
        });
        processArtifactSetResources(this.kubernetesTemplateDependencyArtifacts, items -> {
            List<HasMetadata> templates = Arrays.asList(items.toArray(new HasMetadata[items.size()]));

            // lets remove all the plain resources (without any ${PARAM} expressions) which match objects
            // in the Templates found from the k8s-templates.yml files which still contain ${PARAM} expressions
            // to preserve the parameter expressions for dependent kubernetes resources
            for (HasMetadata resource : templates) {
                if (resource instanceof Template) {
                    Template template = (Template) resource;
                    List<HasMetadata> objects = template.getObjects();
                    if (objects != null) {
                        removeTemplateObjects(kubernetesItems, objects);
                        kubernetesItems.addAll(objects);
                    }
                }
            }
            return null;
        });
        filterAndAddItemsToBuilder(builder, kubernetesItems);
    }

    private void enrichOpenShift(final KubernetesListBuilder builder) {
        final List<HasMetadata> openshiftItems = new ArrayList<>();
        processArtifactSetResources(this.openshiftDependencyArtifacts, items -> {
            openshiftItems.addAll(Arrays.asList(items.toArray(new HasMetadata[0])));
            return null;
        });
        filterAndAddItemsToBuilder(builder, openshiftItems);
    }

    private void removeTemplateObjects(List<HasMetadata> list, List<HasMetadata> objects) {
        for (HasMetadata object : objects) {
            List<HasMetadata> copy = new ArrayList<>(list);
            for (HasMetadata resource : copy) {
                if (Objects.equals(resource.getKind(), object.getKind()) &&
                        Objects.equals(KubernetesHelper.getName(object), KubernetesHelper.getName(resource))) {
                    list.remove(resource);
                }
            }
        }

    }

    public void filterAndAddItemsToBuilder(KubernetesListBuilder builder, List<HasMetadata> items) {
        Map<KindAndName, Integer> aIndexMap = new HashMap<>();
        int nItems = 0;

        // Populate map with existing items in the builder
        for(int index = 0; index < builder.buildItems().size(); index++, nItems++) {
            HasMetadata aItem = builder.buildItems().get(index);
            KindAndName aKey = new KindAndName(aItem);
            aIndexMap.put(aKey, index);
        }

        for(HasMetadata item : items) {
            KindAndName aKey = new KindAndName(item);

            if(aIndexMap.containsKey(aKey)) { // Merge the override fragments, and remove duplicate
                HasMetadata duplicateItem = builder.buildItems().get(aIndexMap.get(aKey));
                item = KubernetesResourceUtil.mergeResources(item, duplicateItem, log, false);
                builder.setToItems(aIndexMap.get(aKey), item);
            }
            else {
                aIndexMap.put(aKey, nItems++);
                builder.addToItems(item);
            }
        }
    }

    private void processArtifactSetResources(Set<URI> artifactSet, Function<List<HasMetadata>, Void> function) {
        for (URI uri : artifactSet) {
            try {
                log.debug("Processing Kubernetes YAML in at: %s", uri);
                KubernetesList resources = new ObjectMapper(new YAMLFactory()).readValue(uri.toURL(), KubernetesList.class);
                List<HasMetadata> items = resources.getItems();
                if (items.isEmpty() && Objects.equals("Template", resources.getKind())) {
                    Template template = new ObjectMapper(new YAMLFactory()).readValue(uri.toURL(), Template.class);
                    if (template != null) {
                        items.add(template);
                    }
                }
                for (HasMetadata item : items) {
                    KubernetesResourceUtil.setSourceUrlAnnotationIfNotSet(item, uri.toString());
                    log.debug("  found %s  %s", KubernetesHelper.getKind(item), KubernetesHelper.getName(item));
                }
                function.apply(items);
            } catch (IOException e) {
                getLog().debug("Skipping %s: %s", uri, e);
            }
        }
    }

    protected boolean isIncludePlugin() {
        return Configs.asBoolean(getConfig(Config.INCLUDE_PLUGIN));
    }

    protected boolean isIncludeTransitive() {
        return Configs.asBoolean(getConfig(Config.INCLUDE_TRANSITIVE));
    }


}
