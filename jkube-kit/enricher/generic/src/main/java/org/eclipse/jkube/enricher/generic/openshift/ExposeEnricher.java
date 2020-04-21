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
package org.eclipse.jkube.enricher.generic.openshift;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.BaseEnricher;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Enricher for enabling exposing of HTTP / HTTPS based services
 */
public class ExposeEnricher extends BaseEnricher {

    public ExposeEnricher(JKubeEnricherContext buildContext) {
        super(buildContext, "jkube-openshift-service-expose");
    }

    private Set<Integer> webPorts = new HashSet<>(Arrays.asList(80, 443, 8080, 9080, 9090, 9443));

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
