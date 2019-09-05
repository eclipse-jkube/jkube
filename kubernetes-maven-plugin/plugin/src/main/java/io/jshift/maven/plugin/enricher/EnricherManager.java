package io.jshift.maven.plugin.enricher;

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.jshift.kit.common.KitLogger;
import io.jshift.kit.common.util.ClassUtil;
import io.jshift.kit.common.util.PluginServiceFactory;
import io.jshift.kit.config.resource.PlatformMode;
import io.jshift.kit.config.resource.ProcessorConfig;
import io.jshift.kit.config.resource.ResourceConfig;
import io.jshift.maven.enricher.api.Enricher;
import io.jshift.maven.enricher.api.EnricherContext;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static io.jshift.maven.enricher.api.util.Misc.filterEnrichers;

/**
 * @author roland
 * @since 08/04/16
 */
public class EnricherManager {

    // List of enrichers used for customizing the generated deployment descriptors
    private List<Enricher> enrichers;

    // context used by enrichers
    private final ProcessorConfig defaultEnricherConfig;

    private KitLogger log;

    public EnricherManager(ResourceConfig resourceConfig, EnricherContext enricherContext, Optional<List<String>> extraClasspathElements) {
        PluginServiceFactory<EnricherContext> pluginFactory = new PluginServiceFactory<>(enricherContext);

        extraClasspathElements.ifPresent(
                cpElements -> pluginFactory.addAdditionalClassLoader(ClassUtil.createProjectClassLoader(cpElements, enricherContext.getLog())));

        this.log = enricherContext.getLog();
        this.defaultEnricherConfig = enricherContext.getConfiguration().getProcessorConfig().orElse(ProcessorConfig.EMPTY);

        this.enrichers = pluginFactory.createServiceObjects("META-INF/jshift-enricher-default",
                "META-INF/jshift/enricher-default",
                "META-INF/jshift-enricher",
                "META-INF/jshift/enricher");

        logEnrichers(filterEnrichers(defaultEnricherConfig, enrichers));

    }

    public void createDefaultResources(PlatformMode platformMode, final KubernetesListBuilder builder) {
        createDefaultResources(platformMode, defaultEnricherConfig, builder);
    }

    public void createDefaultResources(PlatformMode platformMode, ProcessorConfig enricherConfig, final KubernetesListBuilder builder) {
        // Add default resources
        loop(enricherConfig, enricher -> {
            enricher.create(platformMode, builder);
            return null;
        });
    }

    public void enrich(PlatformMode platformMode, KubernetesListBuilder builder) {
        enrich(platformMode, defaultEnricherConfig, builder);
    }

    public void enrich(PlatformMode platformMode, ProcessorConfig config, KubernetesListBuilder builder) {
        enrich(platformMode, config, builder, enrichers);
    }

    /**
     * Allow enricher to add Metadata to the resources.
     *
     * @param builder builder to customize
     * @param enricherList list of enrichers
     */
    private void enrich(PlatformMode platformMode, final ProcessorConfig enricherConfig, final KubernetesListBuilder builder, final List<Enricher> enricherList) {
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