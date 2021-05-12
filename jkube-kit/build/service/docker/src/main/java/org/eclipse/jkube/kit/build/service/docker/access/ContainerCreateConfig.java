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
package org.eclipse.jkube.kit.build.service.docker.access;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.jkube.kit.common.JsonFactory;
import org.eclipse.jkube.kit.common.util.EnvUtil;
import org.eclipse.jkube.kit.config.image.build.Arguments;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.text.StringSubstitutor;


public class ContainerCreateConfig {

    private final JsonObject createConfig = new JsonObject();
    private final String imageName;

    public ContainerCreateConfig(String imageName) {
        this.imageName = imageName;
        createConfig.addProperty("Image", imageName);
    }

    public ContainerCreateConfig binds(List<String> volumes) {
        if (volumes != null && !volumes.isEmpty()) {
            JsonObject extractedVolumes = new JsonObject();

            for (String volume : volumes) {
                extractedVolumes.add(extractContainerPath(volume),
                        new JsonObject());
            }
            createConfig.add("Volumes", extractedVolumes);
        }
        return this;
    }

    public ContainerCreateConfig command(Arguments command) {
        if (command != null) {
            createConfig.add("Cmd", JsonFactory.newJsonArray(command.asStrings()));
        }
        return this;
    }

    public ContainerCreateConfig domainname(String domainname) {
        return add("Domainname", domainname);
    }

    public ContainerCreateConfig entrypoint(Arguments entrypoint) {
        if (entrypoint != null) {
            createConfig.add("Entrypoint", JsonFactory.newJsonArray(entrypoint.asStrings()));
        }
        return this;
    }

    public ContainerCreateConfig environment(String envPropsFile, Map<String, String> env, Map mavenProps) {

        Properties envProps = new Properties();
        if (env != null && env.size() > 0) {
            for (Map.Entry<String, String> entry : env.entrySet()) {
                String value = entry.getValue();
                if (value == null) {
                    value = "";
                } else if(value.matches("^\\+\\$\\{.*}$")) {
                    /*
                     * This case is to handle the Maven interpolation issue which used
                     * to occur when using ${..} only without any suffix.
                     */
                    value = value.substring(1, value.length());
                }
                envProps.put(entry.getKey(), StringSubstitutor.replace(value, mavenProps));
            }
        }
        if (envPropsFile != null) {
            // Props from external file take precedence
            addPropertiesFromFile(envPropsFile, envProps);
        }

        if (envProps.size() > 0) {
            addEnvironment(envProps);
        }
        return this;
    }

    public ContainerCreateConfig labels(Map<String,String> labels) {
        if (labels != null && labels.size() > 0) {
            createConfig.add("Labels", JsonFactory.newJsonObject(labels));
        }
        return this;
    }

    public ContainerCreateConfig exposedPorts(Set<String> portSpecs) {
        if (portSpecs != null && !portSpecs.isEmpty()) {
            JsonObject exposedPorts = new JsonObject();
            for (String portSpec : portSpecs) {
                exposedPorts.add(portSpec, new JsonObject());
            }
            createConfig.add("ExposedPorts", exposedPorts);
        }
        return this;
    }

    public String getImageName() {
        return imageName;
    }

    public ContainerCreateConfig hostname(String hostname) {
        return add("Hostname", hostname);
    }

    public ContainerCreateConfig user(String user) {
        return add("User", user);
    }

    public ContainerCreateConfig workingDir(String workingDir) {
        return add("WorkingDir", workingDir);
    }

    public ContainerCreateConfig hostConfig(ContainerHostConfig startConfig) {
        return add("HostConfig", startConfig.toJsonObject());
    }

    public ContainerCreateConfig networkingConfig(ContainerNetworkingConfig networkingConfig) {
        return add("NetworkingConfig", networkingConfig.toJsonObject());
    }

    /**
     * Get JSON which is used for <em>creating</em> a container
     *
     * @return string representation for JSON representing creating a container
     */
    public String toJson() {
        return createConfig.toString();
    }

    // =======================================================================

    private ContainerCreateConfig add(String name, String value) {
        if (value != null) {
            createConfig.addProperty(name, value);
        }
        return this;
    }

    private ContainerCreateConfig add(String name, JsonObject value) {
        if (value != null) {
            createConfig.add(name, value);
        }
        return this;
    }

    private String extractContainerPath(String volume) {
        String path  = EnvUtil.fixupPath(volume);
        if (path.contains(":")) {
            String[] parts = path.split(":");
            if (parts.length > 1) {
                return parts[1];
            }
        }
        return path;
    }

    private void addEnvironment(Properties envProps) {
        JsonArray containerEnv = new JsonArray();
        Enumeration<Object> keys = envProps.keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            String value = envProps.getProperty(key);
            if (value == null) {
                value = "";
            }
            containerEnv.add(key + "=" + value);
        }
        createConfig.add("Env", containerEnv);
    }

    private void addPropertiesFromFile(String envPropsFile, Properties envProps) {
        // External properties override internally specified properties
        try (FileReader reader = new FileReader(envPropsFile)) {
            envProps.load(reader);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException(String.format("Cannot find environment property file '%s'", envPropsFile));
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Error while loading environment properties: %s", e.getMessage()), e);
        }
    }
}

