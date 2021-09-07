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
package org.eclipse.jkube.kit.enricher.specific;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.BaseEnricher;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePort;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class ServiceDiscoveryEnricher extends BaseEnricher {
    private static final String ENRICHER_NAME = "jkube-service-discovery";

    //Default Prefix
    private static final String PREFIX = "discovery.3scale.net";
    //Service Annotations
    private static final String ANNOTATION_DISCOVERY_VERSION = "discovery-version";
    private static final String ANNOTATION_SCHEME            = Config.SCHEME.key;
    private static final String ANNOTATION_PATH              = Config.PATH.key;
    private static final String ANNOTATION_PORT              = Config.PORT.key;
    private static final String ANNOTATION_DESCRIPTION_PATH  = "description-path";

    @AllArgsConstructor
    private enum Config implements Configs.Config {
        DESCRIPTION_PATH("descriptionPath", null),
        DISCOVERABLE("discoverable", null),
        DISCOVERY_VERSION("discoveryVersion", "v1"),
        PATH("path", null),
        PORT("port", "80"),
        SCHEME("scheme", "http"),
        SPRING_DIR("springDir", null);

        @Getter
        protected String key;
        @Getter
        protected String defaultValue;
    }

    private final File springConfigDir;
    private String path;
    private String port;
    private String scheme;
    private String descriptionPath;
    private String discoverable;


    public ServiceDiscoveryEnricher(JKubeEnricherContext buildContext) {
        super(buildContext, ENRICHER_NAME);

        File baseDir = getContext().getProjectDirectory();
        springConfigDir = new File(getConfig(Config.SPRING_DIR, baseDir + "/src/main/resources/spring"));
        discoverable = getConfig(Config.DISCOVERABLE, null);
    }

    @Override
    public void create(PlatformMode platformMode, final KubernetesListBuilder listBuilder) {
        listBuilder.accept(new TypedVisitor<ServiceBuilder>() {

            @Override
            public void visit(ServiceBuilder serviceBuilder) {
                addAnnotations(serviceBuilder);
            }

        });
    }

    protected void addAnnotations(ServiceBuilder serviceBuilder) {
        if (serviceBuilder.buildSpec() != null) {
            List<ServicePort> ports = serviceBuilder.buildSpec().getPorts();
            if (! ports.isEmpty()) {
                ServicePort firstServicePort = ports.iterator().next();
                Objects.requireNonNull(firstServicePort.getPort(),
                        String.format("Service %s .spec.ports[0].port: required value", serviceBuilder.buildMetadata().getName()));
                port = firstServicePort.getPort().toString();
                log.info("Using first mentioned service port '%s' " , port);
            } else {
                log.warn("No service port was found");
            }
        }

        tryCamelDSLProject();

        if (discoverable != null) {
            String labelName = PREFIX;
            String labelValue = getConfig(Config.DISCOVERABLE, discoverable);
            serviceBuilder.editOrNewMetadata().addToLabels(labelName, labelValue).and().buildMetadata();

            log.info("Add %s label: \"%s\" : \"%s\"", PREFIX, labelName, labelValue);

            Map<String, String> annotations = new HashMap<>();
            annotations.put(PREFIX + "/" + ANNOTATION_DISCOVERY_VERSION, getConfig(Config.DISCOVERY_VERSION));
            annotations.put(PREFIX + "/" + ANNOTATION_SCHEME, getConfig(Config.SCHEME, scheme));

            String resolvedPath = getConfig(Config.PATH, path);
            if (resolvedPath != null) {
                if (! resolvedPath.startsWith("/")) {
                    resolvedPath = "/" + resolvedPath;
                }
                annotations.put(PREFIX + "/" + ANNOTATION_PATH, resolvedPath);
            }
            annotations.put(PREFIX + "/" + ANNOTATION_PORT, getConfig(Config.PORT, port));

            String resolvedDescriptionPath = getConfig(Config.DESCRIPTION_PATH, descriptionPath);
            if (resolvedDescriptionPath != null) {
                if (! resolvedDescriptionPath.toLowerCase().startsWith("http") && ! resolvedDescriptionPath.startsWith("/")) {
                    resolvedDescriptionPath = "/" + resolvedDescriptionPath;
                }
                annotations.put(PREFIX + "/" + ANNOTATION_DESCRIPTION_PATH, resolvedDescriptionPath);
            }
            annotations.forEach((key, value) -> log.info("Add %s annotation: \"%s\" : \"%s\"", PREFIX, key, value));
            serviceBuilder.editMetadata().addToAnnotations(annotations).and().buildMetadata();
        }
    }

    public void tryCamelDSLProject(){
        File camelContextXmlFile = new File(springConfigDir.getAbsoluteFile() + "/camel-context.xml");
        if (camelContextXmlFile.exists()) {
            try {
                DocumentBuilderFactory df = DocumentBuilderFactory.newInstance();
                df.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
                df.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
                Document doc = df.newDocumentBuilder().parse(camelContextXmlFile);
                XPath xPath = XPathFactory.newInstance().newXPath();
                Node nl = (Node) xPath.evaluate("/beans/camelContext/restConfiguration", doc, XPathConstants.NODE);
                if (nl != null) {
                    discoverable = "true";
                    if (nl.getAttributes().getNamedItem("scheme") != null) {
                        scheme = nl.getAttributes().getNamedItem("scheme").getNodeValue();
                        log.verbose("Obtained scheme '%s' from camel-context.xml ", scheme);
                    }
                    if (nl.getAttributes().getNamedItem("contextPath") != null) {
                        path = nl.getAttributes().getNamedItem("contextPath").getNodeValue();
                        log.verbose("Obtained path '%s' from camel-context.xml ", path);
                    }
                    if (nl.getAttributes().getNamedItem("apiContextPath") != null) {
                        descriptionPath = nl.getAttributes().getNamedItem("apiContextPath").getNodeValue();
                        log.verbose("Obtained descriptionPath '%s' from camel-context.xml ", descriptionPath);
                    }
                }
            } catch (Exception ex) {
                log.warn("Failed to load camel context file: %s", ex);
            }
        }
    }

}
