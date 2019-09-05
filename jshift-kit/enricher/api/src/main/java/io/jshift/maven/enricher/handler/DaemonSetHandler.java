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
package io.jshift.maven.enricher.handler;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.api.model.apps.DaemonSetBuilder;
import io.fabric8.kubernetes.api.model.apps.DaemonSetSpec;
import io.fabric8.kubernetes.api.model.apps.DaemonSetSpecBuilder;
import io.jshift.kit.build.service.docker.ImageConfiguration;
import io.jshift.kit.config.resource.ResourceConfig;
import io.jshift.kit.common.util.KubernetesHelper;

import java.util.List;

/**
 * Created by matthew on 26/10/16.
 */
public class DaemonSetHandler {

    private final PodTemplateHandler podTemplateHandler;

    DaemonSetHandler(PodTemplateHandler podTemplateHandler) {
        this.podTemplateHandler = podTemplateHandler;
    }

    public DaemonSet getDaemonSet(ResourceConfig config,
                                  List<ImageConfiguration> images) {
        return new DaemonSetBuilder()
                .withMetadata(createDaemonSetMetaData(config))
                .withSpec(createDaemonSetSpec(config, images))
                .build();
    }

    // ===========================================================

    private ObjectMeta createDaemonSetMetaData(ResourceConfig config) {
        return new ObjectMetaBuilder()
                .withName(KubernetesHelper.validateKubernetesId(config.getControllerName(), "controller name"))
                .build();
    }

    private DaemonSetSpec createDaemonSetSpec(ResourceConfig config, List<ImageConfiguration> images) {
        return new DaemonSetSpecBuilder()
                .withTemplate(podTemplateHandler.getPodTemplate(config,images))
                .build();
    }

}
