/*
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
package org.eclipse.jkube.kit.build.api.helper;

import org.eclipse.jkube.kit.build.api.config.handler.property.PropertyConfigHandler;
import org.eclipse.jkube.kit.build.api.config.handler.property.PropertyMode;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;

import java.util.List;
import java.util.Properties;

/**
 * Utility class which helps in resolving, customizing, initializing and validating
 * image configuration.
 *
 * @author roland
 */
public class ConfigHelper {
    // Property which can be set to activate externalConfiguration through properties.
    // Only works for single image project.
    public static final String EXTERNALCONFIG_ACTIVATION_PROPERTY = "docker.imagePropertyConfiguration";

    private ConfigHelper() {}

    public static void validateExternalPropertyActivation(JavaProject project, List<ImageConfiguration> images) {
        String prop = getExternalConfigActivationProperty(project);
        if(prop == null) {
            return;
        }

        if(images.size() == 1) {
            return;
        }

        // With more than one image, externally activating propertyConfig get's tricky. We can only allow it to affect
        // one single image. Go through each image and check if they will be controlled by default properties.
        // If more than one image matches, fail.
        int imagesWithoutExternalConfig = 0;
        for (ImageConfiguration image : images) {
            if(PropertyConfigHandler.canCoexistWithOtherPropertyConfiguredImages(image.getExternalConfig())) {
                continue;
            }

            // else, it will be affected by the external property.
            imagesWithoutExternalConfig++;
        }

        if(imagesWithoutExternalConfig > 1) {
            throw new IllegalStateException("Configuration error: Cannot use property "+EXTERNALCONFIG_ACTIVATION_PROPERTY+" on projects with multiple images without explicit image external configuration.");
        }
    }

    public static String getExternalConfigActivationProperty(JavaProject project) {
        Properties properties = JKubeProjectUtil.getPropertiesWithSystemOverrides(project);
        String value = properties.getProperty(EXTERNALCONFIG_ACTIVATION_PROPERTY);

        // This can be used to disable in a more "local" context, if set globally
        if(PropertyMode.Skip.name().equalsIgnoreCase(value)) {
            return null;
        }

        return value;
    }

    // ===========================================================================================================

    /**
     * Format an image name by replacing certain placeholders
     */
    public interface NameFormatter {
        String format(String name);
    }


}
