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
package io.jkube.kit.build.service.docker.config.handler;

import java.util.*;

import io.jkube.kit.build.service.docker.ImageConfiguration;
import io.jkube.kit.build.service.docker.config.ConfigHelper;
import io.jkube.kit.build.service.docker.config.handler.compose.DockerComposeConfigHandler;
import io.jkube.kit.build.service.docker.config.handler.property.PropertyConfigHandler;
import io.jkube.kit.common.KitLogger;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;

/**
 * Manager holding all config handlers for external configuration
 *
 * @author roland
 * @since 18/11/14
 */

@Component(role = ImageConfigResolver.class, instantiationStrategy = "singleton")
public class ImageConfigResolver implements Initializable {
    // Map type to handler
    private Map<String,ExternalConfigHandler> registry;

    // No List<ExternalConfigHandler> injection possible currently with Plexus.
    // Strangely, only the first element is injected in the list.
    // So the elements are injected via scalar field injection and collected later.
    // Very ugly, but I dont see any other solution until Plexus is fixed.

    @Requirement(role = PropertyConfigHandler.class)
    private ExternalConfigHandler propertyConfigHandler;

    @Requirement(role = DockerComposeConfigHandler.class)
    private ExternalConfigHandler dockerComposeConfigHandler;

    private KitLogger log;

    @Override
    public void initialize() throws InitializationException {
        this.registry = new HashMap<>();
        for (ExternalConfigHandler handler : new ExternalConfigHandler[] { propertyConfigHandler, dockerComposeConfigHandler }) {
            if (handler != null) {
                registry.put(handler.getType(), handler);
            }
        }
    }

    public void setLog(KitLogger log) {
        this.log = log;
    }

    /**
     * Resolve an image configuration. If it contains a reference to an external configuration
     * the corresponding resolver is called and the resolved image configurations are returned (can
     * be multiple).
     *
     * If no reference to an external configuration is found, the original configuration
     * is returned directly.
     *
     * @param unresolvedConfig the configuration to resolve
     * @param project project used for resolving
     * @param session maven session
     * @return list of resolved image configurations
     * @throws IllegalArgumentException if no type is given when an external reference configuration is provided
     * or when the type is not known (i.e. no handler is registered for this type).
     */
    public List<ImageConfiguration> resolve(ImageConfiguration unresolvedConfig, MavenProject project, MavenSession session) {
        injectExternalConfigActivation(unresolvedConfig, project);
        Map<String,String> externalConfig = unresolvedConfig.getExternalConfig();
        if (externalConfig != null) {
            String type = externalConfig.get("type");
            if (type == null) {
                throw new IllegalArgumentException(unresolvedConfig.getDescription() + ": No config type given");
            }
            ExternalConfigHandler handler = registry.get(type);
            if (handler == null) {
                throw new IllegalArgumentException(unresolvedConfig.getDescription() + ": No handler for type " + type + " given");
            }
            return handler.resolve(unresolvedConfig, project, session);
        } else {
            return Collections.singletonList(unresolvedConfig);
        }
    }

    private void injectExternalConfigActivation(ImageConfiguration unresolvedConfig, MavenProject project) {
        // Allow external activation of property configuration
        String mode = ConfigHelper.getExternalConfigActivationProperty(project);

        if(mode == null) {
            return;
        }

        Map<String, String> externalConfig = unresolvedConfig.getExternalConfig();
        if(externalConfig == null) {
            externalConfig = new HashMap<>();
            externalConfig.put("type", propertyConfigHandler.getType());
            externalConfig.put("mode", mode);
            unresolvedConfig.setExternalConfiguration(externalConfig);

            log.verbose("Global %s=%s property activates property configuration for image", ConfigHelper.EXTERNALCONFIG_ACTIVATION_PROPERTY, mode);
        }
        else
        {
            log.verbose("Ignoring %s=%s property, image has <external> in POM which takes precedence", ConfigHelper.EXTERNALCONFIG_ACTIVATION_PROPERTY, mode);
        }
    }
}
