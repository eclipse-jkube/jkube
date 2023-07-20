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
import org.eclipse.jkube.kit.common.JsonFactory;

/**
 * Simple interface for a ImagePullCache manager, to load and persist the cache.
 */
public class ImagePullCache {

    // Key for the previously used image cache
    private static final String CONTEXT_KEY_PREVIOUSLY_PULLED = "CONTEXT_KEY_PREVIOUSLY_PULLED";

    private final Backend backend;

    public ImagePullCache(Backend backend) {
        this.backend = backend;
    }

    public boolean hasAlreadyPulled(String image) {
        return load().has(image);
    }

    public void pulled(String image) {
        save(load().add(image));
    }

    // Store to use for the cached
    public interface Backend {
        String get(String key);
        void put(String key, String value);
    }

    // ======================================================================================

    private ImagePullCacheStore load() {

        String pullCacheJson = backend.get(CONTEXT_KEY_PREVIOUSLY_PULLED);

        ImagePullCacheStore cache = new ImagePullCacheStore(pullCacheJson);

        if (pullCacheJson == null) {
            save(cache);
        }
        return cache;
    }

    private void save(ImagePullCacheStore cache) {
        backend.put(CONTEXT_KEY_PREVIOUSLY_PULLED, cache.toString());
    }

    /**
     * Simple serializable cache for holding image names
     *
     * @author roland
     * @since 20/07/16
     */
    class ImagePullCacheStore {

        private JsonObject cache;


        ImagePullCacheStore(String json) {
            cache = json != null ? JsonFactory.newJsonObject(json) : new JsonObject();
        }

        public boolean has(String imageName) {
            return cache.has(imageName);
        }

        public ImagePullCacheStore add(String image) {
            cache.addProperty(image, Boolean.TRUE);
            return this;
        }

        @Override
        public String toString() {
            return cache.toString();
        }
    }
}
