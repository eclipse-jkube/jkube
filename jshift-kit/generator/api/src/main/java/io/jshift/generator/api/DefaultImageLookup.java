/**
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.jshift.generator.api;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

/**
 * @author roland
 * @since 05/10/16
 */
public class DefaultImageLookup {

    public static final String DEFAULT_IMAGES_PROPERTIES = "META-INF/jshift/default-images.properties";

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
