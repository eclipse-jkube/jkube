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
package org.eclipse.jkube.kit.build.api.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.eclipse.jkube.kit.common.util.EnvUtil;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Entity holding port mappings which can be set through the configuration.
 *
 * @author roland
 * @since 04.04.14
 */
public class PortMapping {

    // Pattern for splitting of the protocol
    private static final Pattern PROTOCOL_SPLIT_PATTERN = Pattern.compile("(.*?)(?:/(tcp|udp))?$");

    // Mapping between ports and the IP they should bind to
    private final Map<String, String> bindToHostMap = new HashMap<>();

    // ports map (container port -> host port)
    private final Map<String, Integer> containerPortToHostPort = new HashMap<>();

    // Mapping between property name and host port (port filled in after container creation)
    private final Map<String, Integer> hostPortVariableMap = new HashMap<>();

    // project properties
    private final Properties projProperties;

    // variables (container port spec -> host ip variable name)
    private final Map<String, String> specToHostIpVariableMap = new HashMap<>();

    // variables (container port spec -> host port variable name)
    private final Map<String, String> specToHostPortVariableMap = new HashMap<>();

    /**
     * Create the mapping from a configuration. The configuration is list of port mapping specifications which has the
     * format used by docker for port mapping (i.e. host_ip:host_port:container_port)
     * <ul>
     * <li>The "host_ip" part is optional. If not given, the all interfaces are used</li>
     * <li>If "host_port" is non numeric it is taken as a variable name. If this variable is given as value in
     * variables, this number is used as host port. If no numeric value is given, it is considered to be filled with the
     * real, dynamically created port value when #updateProperties(Map) is called</li>
     * </ul>
     *
     * @param portMappings a list of configuration strings where each string hast the format
     *            <code>host_ip:host_port:container_port</code>. If the <code>host-port</code> is non-numeric it is
     *            assumed to be a variable (which later might be filled in with the dynamically created port).
     * @param projProperties project properties
     */
    public PortMapping(List<String> portMappings, Properties projProperties) {
        this.projProperties = projProperties;

        for (String portMapping : portMappings) {
            parsePortMapping(portMapping);
        }
    }

    /**
     * Return the content of the mapping as an array with all specifications as given
     *
     * @return port mappings as JSON array or null if no mappings exist
     */
    public JsonArray toJson() {
        Map<String, Integer> portMap = getContainerPortToHostPortMap();
        if (portMap.isEmpty()) {
            return null;
        }

        JsonArray ret = new JsonArray();
        Map<String, String> bindToMap = getBindToHostMap();

        for (Map.Entry<String, Integer> entry : portMap.entrySet()) {
            JsonObject mapping = new JsonObject();
            String containerPortSpec = entry.getKey();
            Matcher matcher = PROTOCOL_SPLIT_PATTERN.matcher(entry.getKey());
            if (!matcher.matches()) {
                throw new IllegalStateException("Internal error: " + entry.getKey() +
                        " doesn't contain protocol part and doesn't match "
                        + PROTOCOL_SPLIT_PATTERN);
            }

            mapping.addProperty("containerPort", Integer.parseInt(matcher.group(1)));
            if (matcher.group(2) != null) {
                mapping.addProperty("protocol", matcher.group(2));
            }

            Integer hostPort = entry.getValue();
            if (hostPort != null) {
                mapping.addProperty("hostPort", hostPort);
            }

            if (bindToMap.containsKey(containerPortSpec)) {
                mapping.addProperty("hostIP", bindToMap.get(containerPortSpec));
            }

            ret.add(mapping);
        }

        return ret;
    }

    // ==========================================================================================================

    // visible for testing
    Map<String, String> getBindToHostMap() {
        return bindToHostMap;
    }

    // visible for testing
    Map<String, Integer> getContainerPortToHostPortMap() {
        return containerPortToHostPort;
    }


    private IllegalArgumentException createInvalidMappingError(String mapping, Exception exp) {
        return new IllegalArgumentException("\nInvalid port mapping '" + mapping + "'\n" +
                "Required format: '<hostIP>:<hostPort>:<containerPort>(/tcp|udp)'\n" +
                "See the reference manual for more details");
    }

    private void createMapping(String[] parts, String protocol) {
        if (parts.length == 3) {
            mapBindToAndHostPortSpec(parts[0], parts[1], createPortSpec(parts[2], protocol));
        } else if (parts.length == 2) {
            mapHostPortToSpec(parts[0], createPortSpec(parts[1], protocol));
        } else {
            mapHostPortToSpec(null, createPortSpec(parts[0], protocol));
        }
    }

    private String createPortSpec(String port, String protocol) {
        return Integer.parseInt(port) + "/" + protocol;
    }

    private Integer getAsIntOrNull(String val) {
        try {
            return Integer.parseInt(val);
        } catch (@SuppressWarnings("unused") NumberFormatException exp) {
            return null;
        }
    }

    // Check for a variable containing a port, return it as integer or <code>null</code> is not found or not a number
    // First check system properties, then the variables given
    private Integer getPortFromProjectOrSystemProperty(String var) {
        String sysProp = System.getProperty(var);
        if (sysProp != null) {
            return getAsIntOrNull(sysProp);
        }
        if (projProperties.containsKey(var)) {
            return getAsIntOrNull(projProperties.getProperty(var));
        }
        return null;
    }

    private String extractPortPropertyName(String name) {
        String mavenPropName = EnvUtil.extractMavenPropertyName(name);
        return mavenPropName != null ? mavenPropName : name;
    }

    private void mapBindToAndHostPortSpec(String bindTo, String hPort, String portSpec) {
        mapHostPortToSpec(hPort, portSpec);

        String hostPropName = extractHostPropertyName(bindTo);
        if (hostPropName != null) {
            String host = projProperties.getProperty(hostPropName);
            if (host != null) {
                // the container portSpec can never be null, so use that as the key
                bindToHostMap.put(portSpec, resolveHostname(host));
            }

            specToHostIpVariableMap.put(portSpec, hostPropName);
        } else {
            // the container portSpec can never be null, so use that as the key
            bindToHostMap.put(portSpec, resolveHostname(bindTo));
        }
    }

    private String extractHostPropertyName(String name) {
        if (name.startsWith("+")) {
            return name.substring(1);
        } else {
            return EnvUtil.extractMavenPropertyName(name);
        }
    }

    private void mapHostPortToSpec(String hPort, String portSpec) {
        Integer hostPort;
        if (hPort == null) {
            hostPort = null;
        } else {
            try {
                hostPort = Integer.parseInt(hPort);
            } catch (@SuppressWarnings("unused") NumberFormatException exp) {
                // Port should be dynamically assigned and set to the variable give in hPort
                String portPropertyName = extractPortPropertyName(hPort);

                hostPort = getPortFromProjectOrSystemProperty(portPropertyName);
                if (hostPort != null) {
                    // portPropertyName: Prop name, hostPort: Port from a property value (prefilled)
                    hostPortVariableMap.put(portPropertyName, hostPort);
                } else {
                    // portSpec: Port from container, portPropertyName: Variable name to be filled in later
                    specToHostPortVariableMap.put(portSpec, portPropertyName);
                }
            }
        }
        containerPortToHostPort.put(portSpec, hostPort);
    }

    private void parsePortMapping(String input) {
        try {
            Matcher matcher = PROTOCOL_SPLIT_PATTERN.matcher(input);
            // Matches always
            matcher.matches();
            String mapping = matcher.group(1);
            String protocol = matcher.group(2);
            if (protocol == null) {
                protocol = "tcp";
            }

            createMapping(mapping.split(":", 3), protocol);
        } catch (NullPointerException | NumberFormatException exp) {
            throw createInvalidMappingError(input, exp);
        }
    }

    private String resolveHostname(String bindToHost) {
        try {
            return InetAddress.getByName(bindToHost).getHostAddress();
        } catch (@SuppressWarnings("unused") UnknownHostException e) {
            throw new IllegalArgumentException("Host '" + bindToHost + "' to bind to cannot be resolved");
        }
    }

}

