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
package io.jkube.maven.enricher.handler;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceStatus;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.jkube.kit.common.util.KubernetesHelper;

public class NamespaceHandler {

    public Namespace getNamespace(String ns) {
        Namespace namespace = new Namespace();
        namespace.setMetadata(createProjectMetaData(ns));
        namespace.setStatus(new NamespaceStatus("active"));
        return namespace;
    }

    private ObjectMeta createProjectMetaData(String namespace) {
        return new ObjectMetaBuilder()
                .withName(KubernetesHelper.validateKubernetesId(namespace, "namespace name"))
                .build();
    }

}
