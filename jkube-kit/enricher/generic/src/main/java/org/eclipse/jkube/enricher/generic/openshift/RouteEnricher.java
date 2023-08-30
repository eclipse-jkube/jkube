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
package org.eclipse.jkube.enricher.generic.openshift;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.openshift.api.model.RouteSpec;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.common.util.FileUtil;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.enricher.api.BaseEnricher;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteBuilder;
import io.fabric8.openshift.api.model.RoutePort;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.enricher.api.ServiceExposer;

import java.util.Objects;

import static org.eclipse.jkube.enricher.generic.DefaultServiceEnricher.getPortToExpose;
import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil.mergeMetadata;
import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil.mergeSimpleFields;
import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil.removeItemFromKubernetesBuilder;

/**
 * Enricher which generates a Route for each exposed Service
 */
public class RouteEnricher extends BaseEnricher implements ServiceExposer {

    private static final String GENERATE_ROUTE_PROPERTY = "jkube.openshift.generateRoute";

    @AllArgsConstructor
    private enum Config implements Configs.Config {
        GENERATE_ROUTE("generateRoute", "true"),
        TLS_TERMINATION("tlsTermination", null),
        TLS_INSECURE_EDGE_TERMINATION_POLICY("tlsInsecureEdgeTerminationPolicy", null);

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

    private boolean isRouteWithTLS() {
        return StringUtils.isNotBlank(getConfig(Config.TLS_TERMINATION, null));
    }

    @Override
    public void create(PlatformMode platformMode, final KubernetesListBuilder listBuilder) {
        final ResourceConfig resourceConfig = getConfiguration().getResource();
        if (resourceConfig != null && resourceConfig.getRouteDomain() != null) {
            routeDomainPostfix = resourceConfig.getRouteDomain();
        }

        if(platformMode == PlatformMode.openshift && isGenerateRoute()) {
            listBuilder.accept(new TypedVisitor<ServiceBuilder>() {

                @Override
                public void visit(ServiceBuilder serviceBuilder) {
                    addRoute(listBuilder, serviceBuilder);
                }
            });
        }
    }

    private void addRoute(KubernetesListBuilder listBuilder, ServiceBuilder serviceBuilder) {
        final String serviceName = serviceBuilder.editOrNewMetadata().getName();
        if (StringUtils.isNotBlank(serviceName) && canExposeService(serviceBuilder)) {
            if (!getCreateExternalUrls() && !isExposedWithLabel(serviceBuilder) && !hasWebPorts(serviceBuilder)) {
                getLog().debug(
                  "Skipping Route creation for service %s as it has no web ports and jkube.createExternalUrls is false",
                  serviceName);
                return;
            }
            updateRouteDomainPostFixBasedOnServiceName(serviceName);
            Route opinionatedRoute = createOpinionatedRouteFromService(serviceBuilder, routeDomainPostfix,
                    getConfig(Config.TLS_TERMINATION, "edge"),
                    getConfig(Config.TLS_INSECURE_EDGE_TERMINATION_POLICY, "Allow"),
                    isRouteWithTLS());
            if (opinionatedRoute != null) {
                final Route mergeableRouteFragment = getMergeableFragment(listBuilder, serviceName);
                if (mergeableRouteFragment != null) { // Merge fragment with Opinionated Route
                    removeItemFromKubernetesBuilder(listBuilder, mergeableRouteFragment);
                    Route mergedRoute = mergeRoute(mergeableRouteFragment, opinionatedRoute);
                    listBuilder.addToItems(mergedRoute);
                } else { // No fragment provided. Use Opinionated Route.
                    listBuilder.addToItems(opinionatedRoute);
                }
            }
        }
    }

    private static RoutePort createRoutePort(ServiceBuilder serviceBuilder) {
        RoutePort routePort = null;
        final Integer servicePort = getPortToExpose(serviceBuilder);
        if (servicePort != null) {
            routePort = new RoutePort();
            routePort.setTargetPort(new IntOrString(servicePort));
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

    private void updateRouteDomainPostFixBasedOnServiceName(String serviceName) {
        if (StringUtils.isNotBlank(routeDomainPostfix)) {
            routeDomainPostfix = prepareHostForRoute(routeDomainPostfix, serviceName);
        } else {
            routeDomainPostfix = "";
        }
    }

    static Route mergeRoute(Route routeFromFragment, Route opinionatedRoute) {
        /*
         * Update ApiVersion to route.openshift.io/v1. Plugin always generates an opinionated
         * Route with apiVersion 'route.openshift.io/v1'. However, when resource fragments are used
         * we get a route with apiVersion 'v1'. For more info, see:
         *        https://github.com/eclipse/jkube/issues/383
         */
        if (routeFromFragment.getApiVersion().equals("v1")) {
            routeFromFragment.setApiVersion(opinionatedRoute.getApiVersion());
        }

        mergeMetadata(routeFromFragment, opinionatedRoute);
        routeFromFragment.getMetadata().setName(opinionatedRoute.getMetadata().getName());

        // Merge spec
        if (routeFromFragment.getSpec() != null) {
            routeFromFragment.setSpec(mergeRouteSpec(routeFromFragment.getSpec(), opinionatedRoute.getSpec()));
        } else {
            routeFromFragment.setSpec(opinionatedRoute.getSpec());
        }
        return routeFromFragment;
    }

    static RouteSpec mergeRouteSpec(RouteSpec fragmentSpec, RouteSpec opinionatedSpec) {
        mergeSimpleFields(fragmentSpec, opinionatedSpec);
        if (fragmentSpec.getAlternateBackends() == null && opinionatedSpec.getAlternateBackends() != null) {
            fragmentSpec.setAlternateBackends(opinionatedSpec.getAlternateBackends());
        }
        if (fragmentSpec.getPort() == null && opinionatedSpec.getPort() != null) {
            fragmentSpec.setPort(opinionatedSpec.getPort());
        }
        if (fragmentSpec.getTls() == null && opinionatedSpec.getTls() != null) {
            fragmentSpec.setTls(opinionatedSpec.getTls());
        }
        if (fragmentSpec.getTo() == null && opinionatedSpec.getTo() != null) {
            fragmentSpec.setTo(opinionatedSpec.getTo());
        }

        return fragmentSpec;
    }

    /**
     * Returns a fragment of a Route to be merged with the opinionated defaults
     * (user provides partial Route).
     * Or null if no Route is found or non is to be merged.
     *
     * We want to merge Routes which either have no name, or have the same name as the opinionated default.
     */
    static Route getMergeableFragment(final KubernetesListBuilder listBuilder, final String name) {
        for (HasMetadata item : listBuilder.buildItems()) {
            if (item instanceof Route) {
                if (item.getMetadata() == null ||
                  item.getMetadata().getName() == null ||
                  Objects.equals(item.getMetadata().getName(), name)
                ) {
                    return (Route) item;
                }
            }
        }
        return null;
    }

    private static void handleTlsTermination(RouteBuilder routeBuilder, String tlsTermination, String edgeTerminationPolicy, boolean isRouteWithTLS) {
        if(isRouteWithTLS){
            routeBuilder.editSpec()
                    .editOrNewTls()
                    .withInsecureEdgeTerminationPolicy(edgeTerminationPolicy)
                    .withTermination(tlsTermination)
                    .endTls()
                    .endSpec();
        }
    }

    static Route createOpinionatedRouteFromService(ServiceBuilder serviceBuilder, String routeDomainPostfix, String tlsTermination, String edgeTerminationPolicy, boolean isRouteWithTls) {
        ObjectMeta serviceMetadata = serviceBuilder.buildMetadata();
        if (serviceMetadata != null) {
            String name = serviceMetadata.getName();
            RoutePort routePort = createRoutePort(serviceBuilder);
            if (routePort != null) {
                RouteBuilder routeBuilder = new RouteBuilder().
                        withMetadata(serviceMetadata).
                        withNewSpec().
                        withPort(routePort).
                        withNewTo().withKind("Service").withName(name).endTo().
                        withHost(routeDomainPostfix.isEmpty() ? null : routeDomainPostfix).
                        endSpec();

                handleTlsTermination(routeBuilder, tlsTermination, edgeTerminationPolicy, isRouteWithTls);

                routeBuilder.withNewMetadataLike(routeBuilder.buildMetadata());
                return routeBuilder.build();
            }
        }
        return null;
    }
}
