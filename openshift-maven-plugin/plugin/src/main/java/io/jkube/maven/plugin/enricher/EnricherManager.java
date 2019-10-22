package io.jkube.maven.plugin.enricher;

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.jkube.kit.common.KitLogger;
import io.jkube.kit.common.util.ClassUtil;
import io.jkube.kit.common.util.PluginServiceFactory;
import io.jkube.kit.config.resource.PlatformMode;
import io.jkube.kit.config.resource.ProcessorConfig;
import io.jkube.kit.config.resource.ResourceConfig;
import io.jkube.maven.enricher.api.Enricher;
import io.jkube.maven.enricher.api.EnricherContext;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static io.jkube.maven.enricher.api.util.Misc.filterEnrichers;

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

        this.enrichers = pluginFactory.createServiceObjects("META-INF/jkube-enricher-default",
                "META-INF/jkube/enricher-default",
                "META-INF/jkube-enricher",
                "META-INF/jkube/enricher");

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