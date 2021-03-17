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

import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.BaseEnricher;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil.containsLabelInMetadata;


/**
 * Enricher for enabling exposing of HTTP / HTTPS based services
 */
public class ExposeEnricher extends BaseEnricher {
    public static final String EXPOSE_LABEL = "expose";

    public ExposeEnricher(JKubeEnricherContext buildContext) {
        super(buildContext, "jkube-openshift-service-expose");
    }

    private static final Set<Integer> WEB_PORTS = new HashSet<>(Arrays.asList(80, 443, 8080, 9080, 9090, 9443));

    @Override
    public void create(PlatformMode platformMode, KubernetesListBuilder builder) {
        builder.accept(new TypedVisitor<ServiceBuilder>() {

            @Override
            public void visit(ServiceBuilder serviceBuilder) {
                enrichService(serviceBuilder);
            }
        });
    }

    private void enrichService(ServiceBuilder serviceBuilder) {
        if (hasWebPort(serviceBuilder)) {
            ObjectMeta serviceMetadata = serviceBuilder.buildMetadata();
            if (! containsLabelInMetadata(serviceMetadata, EXPOSE_LABEL, "false")) {
                log.verbose("Adding Service label '%s:true' on service %s" +
                                " so that it is exposed by the exposecontroller microservice." +
                                " To disable use the maven argument: '-Dfabric8.profile=internal-microservice'",
                        EXPOSE_LABEL, KubernetesHelper.getName(serviceMetadata));
                serviceBuilder.editOrNewMetadata().addToLabels(EXPOSE_LABEL, "true").endMetadata();
            }
        }
    }

    private boolean hasWebPort(ServiceBuilder serviceBuilder) {
        ServiceSpec spec = serviceBuilder.buildSpec();
        if (spec != null) {
            List<ServicePort> ports = spec.getPorts();
            if (ports != null) {
                for (ServicePort port : ports) {
                    Integer portNumber = port.getPort();
                    if (portNumber != null && WEB_PORTS.contains(portNumber)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
