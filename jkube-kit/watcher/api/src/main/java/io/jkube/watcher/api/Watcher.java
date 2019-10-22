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
package io.jkube.watcher.api;

import java.util.List;
import java.util.Set;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.jkube.kit.build.service.docker.ImageConfiguration;
import io.jkube.kit.common.Named;
import io.jkube.kit.config.resource.PlatformMode;


public interface Watcher extends Named {

    /**
     * Check whether this watcher should kick in.
     *
     * @return true if the watcher is applicable
     * @param configs all image configurations
     */
    boolean isApplicable(List<ImageConfiguration> configs, Set<HasMetadata> resources, PlatformMode mode);


    /**
     * Watch the resources and kick a rebuild when they change.
     *
     * @param configs all image configurations
     */
    void watch(List<ImageConfiguration> configs, Set<HasMetadata> resources, PlatformMode mode) throws Exception;

}
