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

package io.jshift.enricher.generic.openshift;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.jshift.kit.config.resource.PlatformMode;
import io.jshift.maven.enricher.api.BaseEnricher;
import io.jshift.maven.enricher.api.MavenEnricherContext;
import io.jshift.kit.common.util.KubernetesHelper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Enricher for enabling exposing of HTTP / HTTPS based services
 */
public class ExposeEnricher extends BaseEnricher {

    public ExposeEnricher(MavenEnricherContext buildContext) {
        super(buildContext, "jshift-openshift-service-expose");
    }

    private Set<Integer> webPorts = new HashSet<>(Arrays.asList(80, 443, 8080, 9090));

    public static final String EXPOSE_LABEL = "expose";

    @Override
    public void create(PlatformMode platformMode, KubernetesListBuilder builder) {
        List<HasMetadata> items = builder.getItems();
        if (items != null) {
            for (HasMetadata item : items) {
                if (item instanceof Service) {
                    Service service = (Service) item;
                    enrichService(service);
                }
            }
        }
    }

    private void enrichService(Service service) {
        if (hasWebPort(service)) {
            ObjectMeta metadata = service.getMetadata();
            if (metadata == null) {
                metadata = new ObjectMeta();
                service.setMetadata(metadata);
            }
            Map<String, String> labels = KubernetesHelper.getOrCreateLabels(service);
            if (!labels.containsKey(EXPOSE_LABEL)) {
                labels.put(EXPOSE_LABEL, "true");
                log.verbose("Adding Service label '%s:true' on service %s" +
                            " so that it is exposed by the exposecontroller microservice." +
                            " To disable use the maven argument: '-Dfabric8.profile=internal-microservice'",
                            EXPOSE_LABEL, KubernetesHelper.getName(service));
            }
        }
    }

    private boolean hasWebPort(Service service) {
        ServiceSpec spec = service.getSpec();
        if (spec != null) {
            List<ServicePort> ports = spec.getPorts();
            if (ports != null) {
                for (ServicePort port : ports) {
                    Integer portNumber = port.getPort();
                    if (portNumber != null) {
                        if (webPorts.contains(portNumber)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
