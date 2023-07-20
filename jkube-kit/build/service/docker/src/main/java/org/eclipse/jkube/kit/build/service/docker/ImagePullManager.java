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
package org.eclipse.jkube.kit.build.service.docker;

import com.google.gson.JsonObject;

import org.eclipse.jkube.kit.build.service.docker.helper.AutoPullMode;
import org.eclipse.jkube.kit.common.JsonFactory;
import org.eclipse.jkube.kit.config.image.build.ImagePullPolicy;

import java.util.Properties;

/**
 * Simple interface for a ImagePullCache manager, to load and persist the cache.
 */
public class ImagePullManager {

    // Key for the previously used image cache
    private static final String CONTEXT_KEY_PREVIOUSLY_PULLED = "CONTEXT_KEY_PREVIOUSLY_PULLED";

    // image pull policy
    private final ImagePullPolicy imagePullPolicy;

    private final CacheStore cacheStore;

    public ImagePullManager(CacheStore cacheStore, String imagePullPolicy, String autoPull) {
        this.cacheStore = cacheStore;
        this.imagePullPolicy = createPullPolicy(imagePullPolicy, autoPull);
    }

    ImagePullPolicy getImagePullPolicy() {
        return imagePullPolicy;
    }

    public ImagePullPolicy createPullPolicy(String imagePullPolicy, String autoPull) {
        if (imagePullPolicy != null) {
            return ImagePullPolicy.fromString(imagePullPolicy);
        }
        if (autoPull != null) {
            AutoPullMode autoPullMode = AutoPullMode.fromString(autoPull);
            switch(autoPullMode) {
                case OFF:
                    return ImagePullPolicy.Never;
                case ALWAYS:
                    return ImagePullPolicy.Always;
                case ON:
                case ONCE:
                    return ImagePullPolicy.IfNotPresent;
            }
        }
        return ImagePullPolicy.IfNotPresent;
    }

    public boolean hasAlreadyPulled(String image) {
        return load().has(image);
    }

    public void pulled(String image) {
        save(load().add(image));
    }

    public static ImagePullManager createImagePullManager(String imagePullPolicy, String autoPull, Properties properties) {
      return new ImagePullManager(new PropertyCacheStore(properties), imagePullPolicy, autoPull);
    }

    public interface CacheStore {
        String get(String key);

        void put(String key, String value);
    }

    public ImagePullCache load() {

        String pullCacheJson = cacheStore.get(CONTEXT_KEY_PREVIOUSLY_PULLED);

        ImagePullCache cache = new ImagePullCache(pullCacheJson);

        if (pullCacheJson == null) {
            save(cache);
            cacheStore.put(CONTEXT_KEY_PREVIOUSLY_PULLED, cache.toString());
        }
        return cache;
    }

    public void save(ImagePullCache cache) {
        cacheStore.put(CONTEXT_KEY_PREVIOUSLY_PULLED, cache.toString());
    }

    /**
     * Simple serializable cache for holding image names
     *
     * @author roland
     * @since 20/07/16
     */
    public static class ImagePullCache {

      private final JsonObject cache;

        public ImagePullCache(String json) {
            cache = json != null ? JsonFactory.newJsonObject(json) : new JsonObject();
        }

        public boolean has(String imageName) {
            return cache.has(imageName);
        }

        public ImagePullCache add(String image) {
            cache.addProperty(image, Boolean.TRUE);
            return this;
        }

        @Override
        public String toString() {
            return cache.toString();
        }
    }

    public static class PropertyCacheStore implements CacheStore {
      private final Properties properties;

      public PropertyCacheStore(Properties properties) {
        this.properties = properties;
      }

      @Override
      public String get(String key) {
        return properties.getProperty(key);
      }

      @Override
      public void put(String key, String value) {
        properties.setProperty(key, value);
      }
    }
}
