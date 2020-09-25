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
package org.eclipse.jkube.maven.plugin.enricher;

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.ClassUtil;
import org.eclipse.jkube.kit.common.util.PluginServiceFactory;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.config.service.EnricherManager;
import org.eclipse.jkube.kit.enricher.api.Enricher;
import org.eclipse.jkube.kit.enricher.api.EnricherContext;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.eclipse.jkube.kit.enricher.api.util.Misc.filterEnrichers;

/**
 * @author roland
 */
public class DefaultEnricherManager implements EnricherManager {

    // List of enrichers used for customizing the generated deployment descriptors
    private final List<Enricher> enrichers;

    // context used by enrichers
    private final ProcessorConfig defaultEnricherConfig;

    private final KitLogger log;

    public DefaultEnricherManager(EnricherContext enricherContext, Optional<List<String>> extraClasspathElements) {
        PluginServiceFactory<EnricherContext> pluginFactory = new PluginServiceFactory<>(enricherContext);

        extraClasspathElements.ifPresent(
                cpElements -> pluginFactory.addAdditionalClassLoader(ClassUtil.createProjectClassLoader(cpElements, enricherContext.getLog())));

        this.log = enricherContext.getLog();
        this.defaultEnricherConfig = Optional.ofNullable(enricherContext.getConfiguration().getProcessorConfig())
            .orElse(ProcessorConfig.EMPTY);

        this.enrichers = pluginFactory.createServiceObjects("META-INF/jkube-enricher-default",
                "META-INF/jkube/enricher-default",
                "META-INF/jkube-enricher",
                "META-INF/jkube/enricher");

        logEnrichers(filterEnrichers(defaultEnricherConfig, enrichers));
    }

    @Override
    public void createDefaultResources(PlatformMode platformMode, final KubernetesListBuilder builder) {
        createDefaultResources(platformMode, defaultEnricherConfig, builder);
    }

    @Override
    public void createDefaultResources(PlatformMode platformMode, ProcessorConfig enricherConfig, final KubernetesListBuilder builder) {
        // Add default resources
        loop(enricherConfig, enricher -> {
            enricher.create(platformMode, builder);
            return null;
        });
    }

    @Override
    public void enrich(PlatformMode platformMode, KubernetesListBuilder builder) {
        enrich(platformMode, defaultEnricherConfig, builder);
    }

    /**
     * Allow enricher to add Metadata to the resources.
     *
     * @param builder builder to customize
     */
    @Override
    public void enrich(PlatformMode platformMode, final ProcessorConfig enricherConfig, final KubernetesListBuilder builder) {
        loop(enricherConfig, enricher -> {
                enricher.enrich(platformMode, builder);
                return null;
            });
    }

    // =============================================================================================
    private void logEnrichers(List<Enricher> enrichers) {
        log.verbose("Enrichers:");
        for (Enricher enricher : enrichers) {
            log.verbose("- %s", enricher.getName());
        }
    }

    private void loop(ProcessorConfig config, Function<Enricher, Void> function) {
        for (Enricher enricher : filterEnrichers(config, enrichers)) {
            function.apply(enricher);
        }
    }
}