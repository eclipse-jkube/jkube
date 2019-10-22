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
package io.jkube.maven.enricher.api;

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.jkube.kit.common.Named;
import io.jkube.kit.config.resource.PlatformMode;

/**
 * Interface describing enrichers which add to kubernetes descriptors
 *
 * @author roland
 * @since 01/04/16
 */
public interface Enricher extends Named {

    /**
     * Add default resources when they are missing. Each enricher should be responsible
     * for a certain kind of resource and should detect whether a default resource
     * should be added. This should be done defensive, so that when an object is
     * already set it must not be overwritten. This method is only called on resources which are
     * associated with the artefact to build. This is determined that the resource is named like the artifact
     * to build.
     *
     * @param platformMode platform mode for generated resource descriptors
     * @param builder the build to examine and add to
     */
    void create(PlatformMode platformMode, KubernetesListBuilder builder);

    /**
     * Final customization of the overall resource descriptor. Fine tuning happens here.
     * @param platformMode platform mode for generated resource descriptors
     * @param builder list to customer used to customize
     */
    void enrich(PlatformMode platformMode, KubernetesListBuilder builder);
}
