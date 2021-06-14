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
package org.eclipse.jkube.enricher.generic;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.TypedLocalObjectReferenceBuilder;
import io.fabric8.kubernetes.api.model.extensions.HTTPIngressPath;
import io.fabric8.kubernetes.api.model.extensions.HTTPIngressPathBuilder;
import io.fabric8.kubernetes.api.model.extensions.HTTPIngressRuleValue;
import io.fabric8.kubernetes.api.model.extensions.HTTPIngressRuleValueBuilder;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.extensions.IngressBackend;
import io.fabric8.kubernetes.api.model.extensions.IngressBackendBuilder;
import io.fabric8.kubernetes.api.model.extensions.IngressBuilder;
import io.fabric8.kubernetes.api.model.extensions.IngressRule;
import io.fabric8.kubernetes.api.model.extensions.IngressRuleBuilder;
import io.fabric8.kubernetes.api.model.extensions.IngressSpec;
import io.fabric8.kubernetes.api.model.extensions.IngressSpecBuilder;
import io.fabric8.kubernetes.api.model.extensions.IngressTLS;
import io.fabric8.kubernetes.api.model.extensions.IngressTLSBuilder;

import java.util.ArrayList;
import java.util.List;

public class ExtensionsV1beta1IngressConverter {
    private ExtensionsV1beta1IngressConverter() { }

    public static Ingress convert(io.fabric8.kubernetes.api.model.networking.v1.Ingress networkV1Ingress) {
        if (networkV1Ingress == null) {
            return null;
        }
        IngressBuilder extensionsIngressBuilder = new IngressBuilder();
        if (networkV1Ingress.getMetadata() != null) {
            extensionsIngressBuilder.withMetadata(networkV1Ingress.getMetadata());
        }
        if (networkV1Ingress.getSpec() != null) {
            extensionsIngressBuilder.withSpec(convertIngressSpec(networkV1Ingress.getSpec()));
        }

        return extensionsIngressBuilder.build();
    }

    private static IngressSpec convertIngressSpec(io.fabric8.kubernetes.api.model.networking.v1.IngressSpec networkV1IngressSpec) {
        IngressSpecBuilder extensionIngressSpecBuilder = new IngressSpecBuilder();
        if (networkV1IngressSpec.getIngressClassName() != null) {
            extensionIngressSpecBuilder.withIngressClassName(networkV1IngressSpec.getIngressClassName());
        }
        if (networkV1IngressSpec.getDefaultBackend() != null) {
            extensionIngressSpecBuilder.withBackend(convertIngressBackend(networkV1IngressSpec.getDefaultBackend()));
        }
        if (networkV1IngressSpec.getTls() != null) {
            extensionIngressSpecBuilder.withTls(convertIngressTls(networkV1IngressSpec.getTls()));
        }
        if (networkV1IngressSpec.getRules() != null) {
            extensionIngressSpecBuilder.withRules(convertIngressRules(networkV1IngressSpec.getRules()));
        }
        return extensionIngressSpecBuilder.build();
    }

    private static IngressBackend convertIngressBackend(io.fabric8.kubernetes.api.model.networking.v1.IngressBackend networkV1IngressBackend) {
        IngressBackendBuilder extensionIngressBackendBuilder = new IngressBackendBuilder();
        if (networkV1IngressBackend.getService() != null) {
            extensionIngressBackendBuilder.withServiceName(networkV1IngressBackend.getService().getName());
            if (networkV1IngressBackend.getService().getPort() != null) {
                extensionIngressBackendBuilder.withServicePort(new IntOrString(networkV1IngressBackend.getService().getPort().getNumber()));
            }
        }
        if (networkV1IngressBackend.getResource() != null) {
            extensionIngressBackendBuilder.withResource(new TypedLocalObjectReferenceBuilder()
                    .withName(networkV1IngressBackend.getResource().getName())
                    .withApiGroup(networkV1IngressBackend.getResource().getApiGroup())
                    .withKind(networkV1IngressBackend.getResource().getKind())
                    .build());
        }
        return extensionIngressBackendBuilder.build();
    }

    private static List<IngressTLS> convertIngressTls(List<io.fabric8.kubernetes.api.model.networking.v1.IngressTLS> networkV1IngressTls) {
        List<IngressTLS> ingressTLS = new ArrayList<>();
        networkV1IngressTls.forEach(t -> ingressTLS.add(convertIngressTls(t)));

        return ingressTLS;
    }

    private static IngressTLS convertIngressTls(io.fabric8.kubernetes.api.model.networking.v1.IngressTLS networkV1IngressTls) {
        IngressTLSBuilder ingressTLSBuilder = new IngressTLSBuilder();
        if (networkV1IngressTls.getHosts() != null) {
            ingressTLSBuilder.withHosts(networkV1IngressTls.getHosts());
        }
        if (networkV1IngressTls.getSecretName() != null) {
            ingressTLSBuilder.withSecretName(networkV1IngressTls.getSecretName());
        }
        return ingressTLSBuilder.build();
    }

    private static List<IngressRule> convertIngressRules(List<io.fabric8.kubernetes.api.model.networking.v1.IngressRule> networkingV1IngressRules) {
        List<IngressRule> ingressRules = new ArrayList<>();
        networkingV1IngressRules.forEach(ir -> ingressRules.add(convertIngressRule(ir)));

        return ingressRules;
    }

    private static IngressRule convertIngressRule(io.fabric8.kubernetes.api.model.networking.v1.IngressRule networkingV1IngressRule) {
        IngressRuleBuilder ingressRuleBuilder = new IngressRuleBuilder();
        if (networkingV1IngressRule.getHost() != null) {
            ingressRuleBuilder.withHost(networkingV1IngressRule.getHost());
        }
        if (networkingV1IngressRule.getHttp() != null) {
            ingressRuleBuilder.withHttp(convertHTTPIngressRuleValue(networkingV1IngressRule.getHttp()));
        }
        return ingressRuleBuilder.build();
    }

    private static HTTPIngressRuleValue convertHTTPIngressRuleValue(io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressRuleValue networkV1HTTPIngressRuleValue) {
        HTTPIngressRuleValueBuilder httpIngressRuleValueBuilder = new HTTPIngressRuleValueBuilder();
        if (networkV1HTTPIngressRuleValue.getPaths() != null) {
            httpIngressRuleValueBuilder.withPaths(convertHTTPIngressPaths(networkV1HTTPIngressRuleValue.getPaths()));
        }
        return httpIngressRuleValueBuilder.build();
    }

    private static List<HTTPIngressPath> convertHTTPIngressPaths(List<io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressPath> networkV1HttpIngressPaths) {
        List<HTTPIngressPath> httpIngressPaths = new ArrayList<>();
        networkV1HttpIngressPaths.forEach(h -> httpIngressPaths.add(convertHTTPIngressPath(h)));

        return httpIngressPaths;
    }

    private static HTTPIngressPath convertHTTPIngressPath(io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressPath networkV1HttpIngressPath) {
        HTTPIngressPathBuilder httpIngressPathBuilder = new HTTPIngressPathBuilder();
        if (networkV1HttpIngressPath.getPath() != null) {
            httpIngressPathBuilder.withPath(networkV1HttpIngressPath.getPath());
        }
        if (networkV1HttpIngressPath.getPathType() != null) {
            httpIngressPathBuilder.withPathType(networkV1HttpIngressPath.getPathType());
        }
        if (networkV1HttpIngressPath.getBackend() != null) {
            httpIngressPathBuilder.withBackend(convertIngressBackend(networkV1HttpIngressPath.getBackend()));
        }
        return httpIngressPathBuilder.build();
    }
}
