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
package org.eclipse.jkube.generator.api;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

/**
 * @author roland
 * @since 05/10/16
 */
public class DefaultImageLookup {

    public static final String DEFAULT_IMAGES_PROPERTIES = "META-INF/jkube/default-images.properties";

    private final Properties defaultImageProps;

    public DefaultImageLookup(Class realm) {
        defaultImageProps = new Properties();
        try {
            Enumeration<URL> resourceUrls = realm.getClassLoader().getResources(DEFAULT_IMAGES_PROPERTIES);
            while (resourceUrls.hasMoreElements()) {
                URL resourceUrl = resourceUrls.nextElement();
                defaultImageProps.load(resourceUrl.openStream());
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot load default images properties " + DEFAULT_IMAGES_PROPERTIES + ": " + e, e);
        }
    }

    public String getImageName(String key) {
        String val = defaultImageProps.getProperty(key);
        if (val == null) {
            throw new IllegalArgumentException("No such key " + key + " contained in " + DEFAULT_IMAGES_PROPERTIES + " for fetching the default image names");
        }
        return val;
    }
}
