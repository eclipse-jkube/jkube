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

import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePort;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.BaseEnricher;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceDiscoveryEnricher extends BaseEnricher {
    static final String ENRICHER_NAME = "jkube-service-discovery";

    //Default Prefix
    static final String PREFIX = "discovery.3scale.net";
    //Service Annotations
    static final String DISCOVERY_VERSION = "discovery-version";
    static final String SCHEME            = "scheme";
    static final String PATH              = "path";
    static final String PORT              = "port";
    static final String DESCRIPTION_PATH  = "description-path";

    private File springConfigDir;
    private String path             = null;
    private String port             = "80";
    private String scheme           = "http";
    private String descriptionPath  = null;
    private String discoverable     = null;
    private String discoveryVersion = "v1";

    private enum Config implements Configs.Config {
        descriptionPath,
        discoverable,
        discoveryVersion,
        path,
        port,
        scheme,
        springDir;
    }

    public ServiceDiscoveryEnricher(JKubeEnricherContext buildContext) {
        super(buildContext, ENRICHER_NAME);

        File baseDir = getContext().getProjectDirectory();
        springConfigDir = new File(getConfig(Config.springDir, baseDir + "/src/main/resources/spring"));
        discoverable = getConfig(Config.discoverable, null);
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
                port = firstServicePort.getPort().toString();
                log.info("Using first mentioned service port '%s' " , port);
            } else {
                log.warn("No service port was found");
            }
        }

        tryCamelDSLProject();

        if (discoverable != null) {
            String labelName = PREFIX;
            String labelValue = getConfig(Config.discoverable, discoverable);
            serviceBuilder.editOrNewMetadata().addToLabels(labelName, labelValue).and().buildMetadata();

            log.info("Add %s label: \"%s\" : \"%s\"", PREFIX, labelName, labelValue);

            Map<String, String> annotations = new HashMap<>();
            annotations.put(PREFIX + "/" + DISCOVERY_VERSION, getConfig(Config.discoveryVersion, discoveryVersion));
            annotations.put(PREFIX + "/" + SCHEME, getConfig(Config.scheme, scheme));

            String resolvedPath = getConfig(Config.path, path);
            if (resolvedPath != null) {
                if (! resolvedPath.startsWith("/")) {
                    resolvedPath = "/" + resolvedPath;
                }
                annotations.put(PREFIX + "/" + PATH, resolvedPath);
            }
            annotations.put(PREFIX + "/" + PORT, getConfig(Config.port, port));

            String resolvedDescriptionPath = getConfig(Config.descriptionPath, descriptionPath);
            if (resolvedDescriptionPath != null) {
                if (! resolvedDescriptionPath.toLowerCase().startsWith("http") && ! resolvedDescriptionPath.startsWith("/")) {
                    resolvedDescriptionPath = "/" + resolvedDescriptionPath;
                }
                annotations.put(PREFIX + "/" + DESCRIPTION_PATH , resolvedDescriptionPath);
            }
            for (String annotationName : annotations.keySet()) {
                log.info("Add %s annotation: \"%s\" : \"%s\"", PREFIX, annotationName, annotations.get(annotationName));
            }
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