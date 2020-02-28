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
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.common.JKubeProjectDependency;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.maven.enricher.api.BaseEnricher;
import org.eclipse.jkube.maven.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.maven.enricher.api.model.KindAndName;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.maven.enricher.api.util.KubernetesResourceUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
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
 * @since 14/07/16
 */
public class DependencyEnricher extends BaseEnricher {
    private static String DEPENDENCY_KUBERNETES_YAML = "META-INF/jkube/kubernetes.yml";
    private static String DEPENDENCY_KUBERNETES_TEMPLATE_YAML = "META-INF/jkube/k8s-template.yml";
    private static String DEPENDENCY_OPENSHIFT_YAML = "META-INF/jkube/openshift.yml";

    private Set<URL> kubernetesDependencyArtifacts = new HashSet<>();
    private Set<URL> kubernetesTemplateDependencyArtifacts = new HashSet<>();
    private Set<URL> openshiftDependencyArtifacts = new HashSet<>();

    // Available configuration keys
    private enum Config implements Configs.Key {

        includeTransitive {{
            d = "true";
        }},
        includePlugin {{
            d = "true";
        }};

        protected String d;

        public String def() {
            return d;
        }
    }

    public DependencyEnricher(JKubeEnricherContext buildContext) {
        super(buildContext, "jkube-dependency");

        addArtifactsWithYaml(kubernetesDependencyArtifacts, DEPENDENCY_KUBERNETES_YAML);
        addArtifactsWithYaml(kubernetesTemplateDependencyArtifacts, DEPENDENCY_KUBERNETES_TEMPLATE_YAML);
        addArtifactsWithYaml(openshiftDependencyArtifacts, DEPENDENCY_OPENSHIFT_YAML);

    }

    private void addArtifactsWithYaml(Set<URL> artifactSet, String dependencyYaml) {
        final List<JKubeProjectDependency> artifacts = getContext().getDependencies(isIncludeTransitive());

        for (JKubeProjectDependency artifact : artifacts) {
            if ("compile".equals(artifact.getScope()) && "jar".equals(artifact.getType())) {
                File file = artifact.getFile();
                try {
                    URL url = new URL("jar:" + file.toURI().toURL() + "!/" + dependencyYaml);
                    artifactSet.add(url);
                } catch (MalformedURLException e) {
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
                    artifactSet.add(url);
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
        for(int index = 0; index < builder.getItems().size(); index++, nItems++) {
            HasMetadata aItem = builder.getItems().get(index);
            KindAndName aKey = new KindAndName(aItem);
            aIndexMap.put(aKey, index);
        }

        for(HasMetadata item : items) {
            KindAndName aKey = new KindAndName(item);

            if(aIndexMap.containsKey(aKey)) { // Merge the override fragments, and remove duplicate
                HasMetadata duplicateItem = builder.getItems().get(aIndexMap.get(aKey));
                item = KubernetesResourceUtil.mergeResources(item, duplicateItem, log, false);
                builder.setToItems(aIndexMap.get(aKey), item);
            }
            else {
                aIndexMap.put(aKey, nItems++);
                builder.addToItems(item);
            }
        }
    }

    private void processArtifactSetResources(Set<URL> artifactSet, Function<List<HasMetadata>, Void> function) {
        for (URL url : artifactSet) {
            try {
                InputStream is = url.openStream();
                if (is != null) {
                    log.debug("Processing Kubernetes YAML in at: %s", url);

                    KubernetesList resources = new ObjectMapper(new YAMLFactory()).readValue(is, KubernetesList.class);
                    List<HasMetadata> items = resources.getItems();
                    if (items.size() == 0 && Objects.equals("Template", resources.getKind())) {
                        is = url.openStream();
                        Template template = new ObjectMapper(new YAMLFactory()).readValue(is, Template.class);
                        if (template != null) {
                            items.add(template);
                        }
                    }
                    for (HasMetadata item : items) {
                        KubernetesResourceUtil.setSourceUrlAnnotationIfNotSet(item, url.toString());
                        log.debug("  found %s  %s", KubernetesHelper.getKind(item), KubernetesHelper.getName(item));
                    }
                    function.apply(items);
                }
            } catch (IOException e) {
                getLog().debug("Skipping %s: %s", url, e);
            }
        }
    }

    protected boolean isIncludePlugin() {
        return Configs.asBoolean(getConfig(Config.includePlugin));
    }

    protected boolean isIncludeTransitive() {
        return Configs.asBoolean(getConfig(Config.includeTransitive));
    }


}
