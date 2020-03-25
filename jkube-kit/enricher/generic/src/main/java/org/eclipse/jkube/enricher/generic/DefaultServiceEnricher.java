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

import io.fabric8.ianaservicehelper.Helper;
import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServiceFluent;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import org.eclipse.jkube.kit.build.service.docker.ImageConfiguration;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;
import org.eclipse.jkube.kit.common.util.SpringBootUtil;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.resource.ServiceConfig;
import org.eclipse.jkube.maven.enricher.api.BaseEnricher;
import org.eclipse.jkube.maven.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.maven.enricher.handler.HandlerHub;
import org.eclipse.jkube.maven.enricher.handler.ServiceHandler;
import java.util.Properties;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An enricher for creating default services when not present.
 *
 * @author roland
 * @since 25/05/16
 */
public class DefaultServiceEnricher extends BaseEnricher {

    private static final Pattern PORT_PROTOCOL_PATTERN =
        Pattern.compile("^(\\d+)(?:/(tcp|udp))?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PORT_MAPPING_PATTERN =
        Pattern.compile("^\\s*(?<port>\\d+)(\\s*:\\s*(?<targetPort>\\d+))?(\\s*/\\s*(?<protocol>(tcp|udp)))?\\s*$",
                        Pattern.CASE_INSENSITIVE);
    private static final String PORT_IMAGE_LABEL_PREFIX = "jkube.generator.service.port";
    private static final String PORTS_IMAGE_LABEL_PREFIX = "jkube.generator.service.ports";

    /**
     *  Port mapping to refer for normalization of port numbering.
     *  Key -> TargetPort
     *  Value -> Port
     */
    private static final Map<Integer, Integer> PORT_NORMALIZATION_MAPPING = new HashMap<Integer, Integer>() {{
                                                                                    put(8080, 80);
                                                                                    put(8081, 80);
                                                                                    put(8181, 80);
                                                                                    put(8180, 80);
                                                                                    put(8443, 443);
                                                                                    put(443, 443);
                                                                                }};

    // Available configuration keys
    private enum Config implements Configs.Key {
        // Default name to use instead of a calculated one
        name,

        // Whether allow headless services.
        headless {{ d = "false"; }},

        // Whether expose the service as ingress. Needs an 'exposeController'
        // running
        expose {{ d = "false"; }},

        // Type of the service (LoadBalancer, NodePort, ClusterIP, ...)
        type,

        // Service port to use (only for the first exposed pod port)
        port,

        // Whether to expose multiple ports or only the first one
        multiPort {{ d = "false"; }},

        // protocol to use
        protocol {{ d = "tcp"; }},

        //  Whether to normalize services port numbers according to PORT_NORMALISATION_MAPPING
        normalizePort {{ d = "false"; }};

        public String def() { return d; } protected String d;
    }

    public DefaultServiceEnricher(JKubeEnricherContext buildContext) {
        super(buildContext, "jkube-service");
    }

    @Override
    public void create(PlatformMode platformMode, KubernetesListBuilder builder) {

        ResourceConfig xmlConfig = getConfiguration().getResource().orElse(null);

        if (xmlConfig != null && xmlConfig.getServices() != null) {
            // Add Services configured via XML
            addServices(builder, xmlConfig.getServices());

        } else {
            final Service defaultService = getDefaultService();

            if (hasServices(builder)) {
                mergeInDefaultServiceParameters(builder, defaultService);
            } else {
                addDefaultService(builder, defaultService);
            }

            if (Configs.asBoolean(getConfig(Config.normalizePort))) {
                normalizeServicePorts(builder);
            }
        }


    }

    // =======================================================================================================

    private void normalizeServicePorts(KubernetesListBuilder builder) {
        builder.accept(new TypedVisitor<ServicePortBuilder>() {
            @Override
            public void visit(ServicePortBuilder portBuilder) {
                PORT_NORMALIZATION_MAPPING.keySet().forEach(key -> {
                    if (key.intValue() == portBuilder.buildTargetPort().getIntVal()) {
                        portBuilder.withPort(PORT_NORMALIZATION_MAPPING.get(key));
                    }
                });
            }
        });
    }

    private void addServices(KubernetesListBuilder builder, List<ServiceConfig> services) {
        HandlerHub handlerHub = new HandlerHub(getContext().getGav(), getContext().getConfiguration().getProperties());
        ServiceHandler serviceHandler = handlerHub.getServiceHandler();
        builder.addToServiceItems(toArray(serviceHandler.getServices(services)));
    }

    // convert list to array, never returns null.
    private Service[] toArray(List<Service> services) {
        if (services == null) {
            return new Service[0];
        }
        if (services instanceof ArrayList) {
            return ((ArrayList<Service>) services).toArray(new Service[services.size()]);
        } else {
            Service[] ret = new Service[services.size()];
            for (int i = 0; i < services.size(); i++) {
                ret[i] = services.get(i);
            }
            return ret;
        }
    }

    private String getAppName() {
        try {
            if (getContext().getProjectClassLoaders().isClassInCompileClasspath(true)) {
                Properties
                    properties = SpringBootUtil.getSpringBootApplicationProperties(getContext().getProjectClassLoaders().getCompileClassLoader());
                return properties.getProperty("spring.application.name");
            }
        } catch (Exception ex) {
            log.error("Error while reading the spring-boot configuration", ex);
        }
        return null;
    }

    private String getServiceName() {
        String appName = getAppName();
        if (appName != null) {
            return appName;
        } else {
            return getConfig(Config.name, JKubeProjectUtil.createDefaultResourceName(getContext().getGav().getSanitizedArtifactId()));
        }
    }

    private Service getDefaultService() {

        // No image config, no service
        if (!hasImageConfiguration()) {
            return null;
        }

        String serviceName = getServiceName();

        // Create service only for all images which are supposed to live in a single pod
        List<ServicePort> ports = getImages().map(this::extractPorts).orElse(Collections.emptyList());

        ServiceBuilder builder = new ServiceBuilder()
            .withNewMetadata()
              .withName(serviceName)
              .withLabels(extractLabels())
            .endMetadata();
        ServiceFluent.SpecNested<ServiceBuilder> specBuilder = builder.withNewSpec();
        if (!ports.isEmpty()) {
            specBuilder.withPorts(ports);
        } else if (Configs.asBoolean(getConfig(Config.headless))) {
            specBuilder.withClusterIP("None");
        } else {
            // No ports, no headless --> no service
            return null;
        }
        if (hasConfig(Config.type)) {
            specBuilder.withType(getConfig(Config.type));
        }
        specBuilder.endSpec();

        return builder.build();
    }

    private boolean hasServices(KubernetesListBuilder builder) {
        final AtomicBoolean hasService = new AtomicBoolean(false);
        builder.accept(new TypedVisitor<ServiceBuilder>() {
            @Override
            public void visit(ServiceBuilder element) {
                hasService.set(true);
            }
        });
        return hasService.get();
    }

    private void mergeInDefaultServiceParameters(KubernetesListBuilder builder, final Service defaultService) {
        builder.accept(new TypedVisitor<ServiceBuilder>() {
            @Override
            public void visit(ServiceBuilder service) {
                // Only update single service matching the default service's name
                String defaultServiceName = getDefaultServiceName(defaultService);

                ObjectMeta serviceMetadata = ensureServiceMetadata(service, defaultService);
                String serviceName = ensureServiceName(serviceMetadata, service, defaultServiceName);

                if (defaultService != null && defaultServiceName != null && defaultServiceName.equals(serviceName)) {
                    addMissingServiceParts(service, defaultService);
                }
            }
        });
    }

    private void addDefaultService(KubernetesListBuilder builder, Service defaultService) {
        if (defaultService == null) {
            return;
        }
        ServiceSpec spec = defaultService.getSpec();
        List<ServicePort> ports = spec.getPorts();
        if (!ports.isEmpty()) {
            log.info("Adding a default service '%s' with ports [%s]",
                     defaultService.getMetadata().getName(), formatPortsAsList(ports));
        } else {
            log.info("Adding headless default service '%s'",
                     defaultService.getMetadata().getName());
        }
        builder.addToServiceItems(defaultService);
    }

    // ....................................................................................

    private Map<String, String> extractLabels() {
        Map<String, String> labels = new HashMap<>();
        if (Configs.asBoolean(getConfig(Config.expose))) {
            labels.put("expose", "true");
        }
        return labels;
    }

    // ========================================================================================================
    // Port handling


    private List<ServicePort> extractPorts(List<ImageConfiguration> images) {
        List<ServicePort> ret = new ArrayList<>();
        boolean isMultiPort = Boolean.parseBoolean(getConfig(Config.multiPort));

        List<ServicePort> configuredPorts = extractPortsFromConfig();

        for (ImageConfiguration image : images) {
            Map<String, String> labels = extractLabelsFromConfig(image);
            List<String> podPorts = getPortsFromBuildConfiguration(image);
            List<String> portsFromImageLabels = getLabelWithService(labels);
            if (podPorts.isEmpty()) {
                continue;
            }

            // Extract first port and remove first element
            if(portsFromImageLabels == null || portsFromImageLabels.isEmpty()) {
                addPortIfNotNull(ret, extractPortsFromImageSpec(image.getName(), podPorts.remove(0), shiftOrNull(configuredPorts), null));
            } else {
                for (String imageLabelPort : portsFromImageLabels) {
                    addPortIfNotNull(ret, extractPortsFromImageSpec(image.getName(), podPorts.remove(0), shiftOrNull(configuredPorts), imageLabelPort));
                }
            }

            // Remaining port specs if multi-port is selected
            if (isMultiPort) {
                for (String port : podPorts) {
                    addPortIfNotNull(ret, extractPortsFromImageSpec(image.getName(), port, shiftOrNull(configuredPorts), null));
                }
            }
        }

        // If there are still ports configured add them directly
        if (isMultiPort) {
            ret.addAll(mirrorMissingTargetPorts(configuredPorts));
        } else if (ret.isEmpty() && !configuredPorts.isEmpty()) {
            ret.addAll(mirrorMissingTargetPorts(Collections.singletonList(configuredPorts.get(0))));
        }

        return ret;
    }

    private List<ServicePort> mirrorMissingTargetPorts(List<ServicePort> ports) {
        List<ServicePort> ret = new ArrayList<>();
        for (ServicePort port : ports) {
            ret.add(updateMissingTargetPort(port, port.getPort()));
        }
        return ret;
    }

    // Examine images for build configuration and extract all ports
    private List<String> getPortsFromBuildConfiguration(ImageConfiguration image) {
        // No build, no default service (doesn't make much sense to have no build config, though)
        BuildConfiguration buildConfig = image.getBuildConfiguration();
        if (buildConfig == null) {
            return Collections.emptyList();
        }

        return buildConfig.getPorts();
    }

    // Config can override ports
    private List<ServicePort> extractPortsFromConfig() {
        List<ServicePort> ret = new LinkedList<>();
        String ports = getConfig(Config.port);
        if (ports != null) {
            for (String port : ports.split(",")) {
                ret.add(parsePortMapping(port));
            }
        }
        return ret;
    }

    private Map<String, String> extractLabelsFromConfig(ImageConfiguration imageConfiguration) {
        Map<String, String> labels = new HashMap<>();
        if(imageConfiguration.getBuildConfiguration() != null && imageConfiguration.getBuildConfiguration().getLabels() != null) {
            labels.putAll(imageConfiguration.getBuildConfiguration().getLabels());
        }
        return labels;
    }

    private List<String> getLabelWithService(Map<String, String> labels) {
        List<String> portsList = new ArrayList<>();
        for(Map.Entry<String, String> entry : labels.entrySet()) {
            if(entry.getKey().equals(PORT_IMAGE_LABEL_PREFIX)) {
                portsList.add(entry.getValue());
            } if(entry.getKey().equals(PORTS_IMAGE_LABEL_PREFIX)) {
                portsList.addAll(Arrays.asList(entry.getValue().split(",")));
            }
        }
        return portsList;
    }

    // parse config specified ports
    private ServicePort parsePortMapping(String port) {
        Matcher matcher = PORT_MAPPING_PATTERN.matcher(port);
        if (!matcher.matches()) {
            log.error("Invalid 'port' configuration '%s'. Must match <port>(:<targetPort>)?,<port2>?,...", port);
            throw new IllegalArgumentException("Invalid port mapping specification " + port);
        }

        int servicePort = Integer.parseInt(matcher.group("port"));
        String optionalTargetPort = matcher.group("targetPort");
        String protocol = getProtocol(matcher.group("protocol"));

        ServicePortBuilder builder = new ServicePortBuilder()
            .withPort(servicePort)
            .withProtocol(protocol)
            .withName(getDefaultPortName(servicePort, protocol));

        // leave empty if not set. will be filled up with the port from the image config
        if (optionalTargetPort != null) {
            builder.withNewTargetPort(Integer.parseInt(optionalTargetPort));
        }
        return builder.build();
    }

    // null ports can happen for ignored mappings
    private void addPortIfNotNull(List<ServicePort> ret, ServicePort port) {
        if (port != null) {
            ret.add(port);
        }
    }

    private ServicePort extractPortsFromImageSpec(String imageName, String portSpec, ServicePort portOverride, String targetPortFromImageLabel) {

        Matcher portMatcher = PORT_PROTOCOL_PATTERN.matcher(portSpec);
        if (!portMatcher.matches()) {
            log.warn("Invalid port specification '%s' for image %s. Must match \\d+(/(tcp|udp))?. Ignoring for now for service generation",
                     portSpec, imageName);
            return null;
        }

        Integer targetPort = Integer.parseInt(targetPortFromImageLabel != null ? targetPortFromImageLabel : portMatcher.group(1));
        String protocol = getProtocol(portMatcher.group(2));
        Integer port = checkForLegacyMapping(targetPort);

        // With a port override you can override the detected ports
        if (portOverride != null) {
            return updateMissingTargetPort(portOverride, targetPort);
        }

        return new ServicePortBuilder()
            .withPort(port)
            .withNewTargetPort(targetPort)
            .withProtocol(protocol)
            .withName(getDefaultPortName(port, protocol))
            .build();
    }

    private ServicePort updateMissingTargetPort(ServicePort port, Integer targetPort) {
        if (port.getTargetPort() == null) {
            return new ServicePortBuilder(port).withNewTargetPort(targetPort).build();
        }
        return port;
    }

    private int checkForLegacyMapping(int port) {
        return port;
    }


    private String getProtocol(String imageProtocol) {
        String protocol = imageProtocol != null ? imageProtocol : getConfig(Config.protocol);
        if ("tcp".equalsIgnoreCase(protocol) || "udp".equalsIgnoreCase(protocol)) {
            return protocol.toUpperCase();
        } else {
            throw new IllegalArgumentException(
                String.format("Invalid service protocol %s specified for enricher '%s'. Must be 'tcp' or 'udp'",
                              protocol, getName()));
        }
    }

    private String formatPortsAsList(List<ServicePort> ports)  {
        List<String> p = new ArrayList<>();
        for (ServicePort port : ports) {
            String targetPort = getPortValue(port.getTargetPort());
            String servicePort= port.getPort() != null ? Integer.toString(port.getPort()) : targetPort;
            p.add(targetPort.equals(servicePort) ? targetPort : servicePort + ":" + targetPort);
        }
        return String.join(",", p);
    }

    private String getPortValue(IntOrString port) {
        String val = port.getStrVal();
        if (val == null) {
            val = Integer.toString(port.getIntVal());
        }
        return val;
    }

    private String getDefaultPortName(int port, String serviceProtocol) {
        if ("TCP".equals(serviceProtocol)) {
            switch (port) {
                case 80:
                case 8080:
                case 9090:
                    return "http";
                case 443:
                case 8443:
                    return "https";
                case 8778:
                    return "jolokia";
                case 9779:
                    return "prometheus";
            }
        }

        try {
            Set<String> serviceNames = Helper.serviceNames(port, serviceProtocol.toLowerCase());
            if (serviceNames != null && !serviceNames.isEmpty()) {
                return serviceNames.iterator().next();
            } else {
                return null;
            }
        } catch (IOException e) {
            log.warn("Cannot lookup port %d/%s in IANA database: %s", port, serviceProtocol.toLowerCase(), e.getMessage());
            return null;
        }
    }

    // remove first element of list or null if list is empty
    private ServicePort shiftOrNull(List<ServicePort> ports) {
        if (!ports.isEmpty()) {
            return ports.remove(0);
        }
        return null;
    }

    // ==============================================================================================================
    // Enhance existing services
    // -------------------------

    private String getDefaultServiceName(Service defaultService) {
        String defaultServiceName = KubernetesHelper.getName(defaultService);
        if (defaultServiceName != null && !defaultServiceName.isEmpty()) {
            defaultServiceName = getContext().getGav().getSanitizedArtifactId();
        }
        return defaultServiceName;
    }


    // Merge services of same name with the default service
    private void addMissingServiceParts(ServiceBuilder service, Service defaultService) {

        // If service has no spec -> take over the complete spec from default service
        if (!service.hasSpec()) {
            service.withNewSpecLike(defaultService.getSpec()).endSpec();
            return;
        }

        // If service has no ports -> take over ports from default service
        List<ServicePort> ports = service.buildSpec().getPorts();
        if (ports == null || ports.isEmpty()) {
            service.editSpec().withPorts(defaultService.getSpec().getPorts()).endSpec();
            return;
        }

        // Complete missing parts:
        service.editSpec()
               .withPorts(addMissingDefaultPorts(ports, defaultService))
               .endSpec();
    }

    private String ensureServiceName(ObjectMeta serviceMetadata, ServiceBuilder service, String defaultServiceName) {
        String serviceName = KubernetesHelper.getName(serviceMetadata);
        if (serviceName != null && !serviceName.isEmpty()) {
            service.buildMetadata().setName(defaultServiceName);
            serviceName = KubernetesHelper.getName(service.buildMetadata());
        }
        return serviceName;
    }

    private ObjectMeta ensureServiceMetadata(ServiceBuilder service, Service defaultService) {
        if (!service.hasMetadata() && defaultService != null) {
            service.withNewMetadataLike(defaultService.getMetadata()).endMetadata();
        }
        return service.buildMetadata();
    }


    private List<ServicePort> addMissingDefaultPorts(List<ServicePort> ports, Service defaultService) {

        // Ensure protocol and port names are set on the given ports
        ensurePortProtocolAndName(ports);

        // lets add at least one default port
        return tryToFindAtLeastOnePort(ports, defaultService);
    }

    private void ensurePortProtocolAndName(List<ServicePort> ports) {
        for (ServicePort port : ports) {
            String protocol = ensureProtocol(port);
            ensurePortName(port, protocol);
        }
    }

    private List<ServicePort> tryToFindAtLeastOnePort(List<ServicePort> ports, Service defaultService) {
        List<ServicePort> defaultPorts = defaultService.getSpec().getPorts();
        if (!ports.isEmpty() || defaultPorts == null || defaultPorts.isEmpty()) {
            return ports;
        }
        return Collections.singletonList(defaultPorts.get(0));
    }

    private void ensurePortName(ServicePort port, String protocol) {
        if (port.getName() != null && !port.getName().isEmpty()) {
            port.setName(getDefaultPortName(port.getPort(), getProtocol(protocol)));
        }
    }

    private String ensureProtocol(ServicePort port) {
        String protocol = port.getProtocol();
        if (protocol != null && !protocol.isEmpty()) {
            port.setProtocol("TCP");
            return "TCP";
        }
        return protocol;
    }
}
