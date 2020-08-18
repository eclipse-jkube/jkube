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
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteBuilder;
import io.fabric8.openshift.api.model.RoutePort;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.common.util.FileUtil;
import org.eclipse.jkube.kit.config.resource.JKubeAnnotations;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.enricher.api.BaseEnricher;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil.containsLabelInMetadata;
import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil.removeLabel;

/**
 * Enricher which generates a Route for each exposed Service
 */
public class RouteEnricher extends BaseEnricher {

    private static final String GENERATE_ROUTE_PROPERTY = "jkube.openshift.generateRoute";
    private static final String GENERATE_TLS_TERMINATION_PROPERTY = "jkube.openshift.generateRoute.tls.termination";
    private static final String GENERATE_TLS_INSECURE_EDGE_TERMINATION_POLICY_PROPERTY = "jkube.openshift.generateRoute.tls.insecure_edge_termination_policy";
    public static final String EXPOSE_LABEL = "expose";

    @AllArgsConstructor
    private enum Config implements Configs.Config {
        GENERATE_ROUTE("generateRoute", "true"),
        //TARGET_PORT("targetPort", "8080"),
        TLS_TERMINATION("termination",null),
        INSECURE_EDGE_TERMINATION_POLICY("insecureEdgeTerminationPolicy",null);

        @Getter
        protected String key;
        @Getter
        protected String defaultValue;
    }

    private String routeDomainPostfix;

    public RouteEnricher(JKubeEnricherContext buildContext) {
        super(buildContext, "jkube-openshift-route");
    }

    private boolean isGenerateRoute() {
        return Configs.asBoolean(getConfigWithFallback(Config.GENERATE_ROUTE, GENERATE_ROUTE_PROPERTY, null));
    }

    private boolean isRouteWithTLS(){
        String propval = getConfigWithFallback(Config.TLS_TERMINATION, GENERATE_TLS_TERMINATION_PROPERTY, null);
        return (propval != null && !propval.isEmpty());
    }

    @Override
    public void create(PlatformMode platformMode, final KubernetesListBuilder listBuilder) {
        ResourceConfig resourceConfig = getConfiguration().getResource();

        if (resourceConfig != null && resourceConfig.getRouteDomain() != null) {
            routeDomainPostfix = resourceConfig.getRouteDomain();
        }

        if(platformMode == PlatformMode.openshift && isGenerateRoute()) {
            final List<Route> routes = new ArrayList<>();
            listBuilder.accept(new TypedVisitor<ServiceBuilder>() {

                @Override
                public void visit(ServiceBuilder serviceBuilder) {
                    addRoute(listBuilder, serviceBuilder, routes);
                }
            });

            if (!routes.isEmpty()) {
                Route[] routeArray = new Route[routes.size()];
                routes.toArray(routeArray);
                listBuilder.addToItems(routeArray);
            }
        }
    }

    @Override
    public void enrich(PlatformMode platformMode, final KubernetesListBuilder listBuilder) {
        if (platformMode == PlatformMode.openshift && isRouteWithTLS()) {
            listBuilder.accept(new TypedVisitor<RouteBuilder>() {
                @Override
                public void visit(RouteBuilder route) {
                    route.editOrNewSpec()
                            .editOrNewTls()
                            .withInsecureEdgeTerminationPolicy(getConfigWithFallback(Config.INSECURE_EDGE_TERMINATION_POLICY, GENERATE_TLS_INSECURE_EDGE_TERMINATION_POLICY_PROPERTY, "Allow"))
                            .withTermination(getConfigWithFallback(Config.TLS_TERMINATION, GENERATE_TLS_TERMINATION_PROPERTY, "edge"))
                            .endTls()
                            .endSpec();
                }
            });
        }
    }


    private RoutePort createRoutePort(ServiceBuilder serviceBuilder) {
        RoutePort routePort = null;
        ServiceSpec spec = serviceBuilder.buildSpec();
        if (spec != null) {
            List<ServicePort> ports = spec.getPorts();
            if (ports != null && !ports.isEmpty()) {
                ServicePort servicePort = ports.get(0);
                if (servicePort != null) {
                    IntOrString targetPort = servicePort.getTargetPort();
                    if (targetPort != null) {
                        routePort = new RoutePort();
                        routePort.setTargetPort(targetPort);
                    }
                }
            }
        }
        return routePort;
    }

    private String prepareHostForRoute(String routeDomainPostfix, String name) {
        String ret = FileUtil.stripPostfix(name,"-service");
        ret = FileUtil.stripPostfix(ret,".");
        ret += ".";
        ret += FileUtil.stripPrefix(routeDomainPostfix, ".");
        return ret;
    }

    private Set<Integer> getPorts(ServiceBuilder service) {
        Set<Integer> answer = new HashSet<>();
        if (service != null) {
            ServiceSpec spec = getOrCreateSpec(service);
            for (ServicePort port : spec.getPorts()) {
                answer.add(port.getPort());
            }
        }
        return answer;
    }

    public static ServiceSpec getOrCreateSpec(ServiceBuilder entity) {
        ServiceSpec spec = entity.buildSpec();
        if (spec == null) {
            spec = new ServiceSpec();
            entity.editOrNewSpec().endSpec();
        }
        return spec;
    }

    private boolean hasExactlyOneServicePort(ServiceBuilder service, String id) {
        Set<Integer> ports = getPorts(service);
        if (ports.size() != 1) {
            log.info("Not generating route for service " + id + " as only single port services are supported. Has ports: " +
                    ports);
            return false;
        } else {
            return true;
        }
    }

    private void addRoute(KubernetesListBuilder listBuilder, ServiceBuilder serviceBuilder, List<Route> routes) {
        ObjectMeta serviceMetadata = serviceBuilder.buildMetadata();

        if (serviceMetadata != null && StringUtils.isNotBlank(serviceMetadata.getName())
                && hasExactlyOneServicePort(serviceBuilder, serviceMetadata.getName()) && isExposedService(serviceMetadata)) {
            String name = serviceMetadata.getName();
            if (!hasRoute(listBuilder, name)) {
                if (StringUtils.isNotBlank(routeDomainPostfix)) {
                    routeDomainPostfix = prepareHostForRoute(routeDomainPostfix, name);
                } else {
                    routeDomainPostfix = "";
                }

                RoutePort routePort = createRoutePort(serviceBuilder);
                if (routePort != null) {
                    RouteBuilder routeBuilder = new RouteBuilder().
                            withMetadata(serviceMetadata).
                            withNewSpec().
                            withPort(routePort).
                            withNewTo().withKind("Service").withName(name).endTo().
                            withHost(routeDomainPostfix.isEmpty() ? null : routeDomainPostfix).
                            endSpec();

                    // removing `expose : true` label from metadata.
                    removeLabel(routeBuilder.buildMetadata(), EXPOSE_LABEL, "true");
                    removeLabel(routeBuilder.buildMetadata(), JKubeAnnotations.SERVICE_EXPOSE_URL.value(), "true");
                    routeBuilder.withNewMetadataLike(routeBuilder.buildMetadata());
                    routes.add(routeBuilder.build());
                }
            }
        }
    }

    /**
     * Returns true if we already have a route created for the given name
     */
    private boolean hasRoute(final KubernetesListBuilder listBuilder, final String name) {
        final AtomicBoolean answer = new AtomicBoolean(false);
        listBuilder.accept(new TypedVisitor<RouteBuilder>() {

            @Override
            public void visit(RouteBuilder builder) {
                ObjectMeta metadata = builder.buildMetadata();
                if (metadata != null && name.equals(metadata.getName())) {
                    answer.set(true);
                }
            }
        });
        return answer.get();
    }

    private static boolean isExposedService(ObjectMeta objectMeta) {
        return containsLabelInMetadata(objectMeta, EXPOSE_LABEL, "true") ||
                containsLabelInMetadata(objectMeta, JKubeAnnotations.SERVICE_EXPOSE_URL.value(), "true");
    }

}
