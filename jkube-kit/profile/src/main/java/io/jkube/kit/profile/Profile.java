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
package io.jkube.kit.profile;

import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.jkube.kit.config.resource.ProcessorConfig;

/**
 * A profile is a named configuration with ernicher and generator configs.
 *
 * @author roland
 * @since 24/07/16
 */
public class Profile implements Comparable<Profile> {

    // Create id for each object
    private static final AtomicInteger idCreator = new AtomicInteger(0);
    private final int id;

    /**
     * Profile name
     */
    @JsonProperty(value = "name")
    private String name;

    /**
     * Extends configuration
     */
    @JsonProperty(value = "extends")
    private String parentProfile;

    /**
     * Enricher configurations
     */
    @JsonProperty(value = "enricher")
    private ProcessorConfig enricherConfig;

    /**
     * Generator configurations
     */
    @JsonProperty(value = "generator")
    private ProcessorConfig generatorConfig;

    /**
     * Watcher configurations
     */
    @JsonProperty(value = "watcher")
    private ProcessorConfig watcherConfig;

    /**
     * An order in case multiple profiles are found
     * with the same name
     */
    @JsonProperty(value = "order")
    private int order;

    // No-arg constructor for YAML deserialization
    public Profile() {
        this.id = idCreator.getAndIncrement();
    }

    // Copy constructor
    public Profile(Profile profile) {
        this();
        this.name = profile.name;
        this.parentProfile = profile.parentProfile;
        this.order = profile.order;
        this.enricherConfig = ProcessorConfig.cloneProcessorConfig(profile.enricherConfig);
        this.generatorConfig = ProcessorConfig.cloneProcessorConfig(profile.generatorConfig);
        this.watcherConfig = ProcessorConfig.cloneProcessorConfig(profile.watcherConfig);
    }

    // Merge constructor
    public Profile(Profile profileA, Profile profileB) {
        this();
        this.name = profileA.name;
        if (!profileB.name.equals(profileA.getName())) {
            throw new IllegalArgumentException(String.format("Cannot merge to profiles with different names (%s vs. %s)", profileA.getName(), profileB.getName()));
        }
        // Respect order: The higher order overrides the smaller order. If equal, use the argument order given.
        if (profileA.order >= profileB.order) {
            this.order = profileA.order;
            this.enricherConfig = ProcessorConfig.mergeProcessorConfigs(profileA.enricherConfig, profileB.enricherConfig);
            this.generatorConfig = ProcessorConfig.mergeProcessorConfigs(profileA.generatorConfig, profileB.generatorConfig);
            this.watcherConfig = ProcessorConfig.mergeProcessorConfigs(profileA.watcherConfig, profileB.watcherConfig);
        } else {
            this.order = profileB.order;
            this.enricherConfig = ProcessorConfig.mergeProcessorConfigs(profileB.enricherConfig, profileA.enricherConfig);
            this.generatorConfig = ProcessorConfig.mergeProcessorConfigs(profileB.generatorConfig, profileA.generatorConfig);
            this.watcherConfig = ProcessorConfig.mergeProcessorConfigs(profileB.watcherConfig, profileA.watcherConfig);
        }
    }

    public String getName() {
        return name;
    }

    public String getParentProfile() { return parentProfile; }

    public ProcessorConfig getEnricherConfig() {
        return enricherConfig;
    }

    public ProcessorConfig getGeneratorConfig() {
        return generatorConfig;
    }

    public ProcessorConfig getWatcherConfig() {
        return watcherConfig;
    }

    public void setEnricherConfig(ProcessorConfig config) { this.enricherConfig = config; }

    public void setGeneratorConfig(ProcessorConfig config) { this.generatorConfig = config; }

    public void setWatcherConfig(ProcessorConfig config) { this.watcherConfig = config; }

    public int getOrder() {
        return order;
    }

    @Override
    // Higher order means "larger"
    public int compareTo(Profile o) {
        int orderDiff = order - o.order;
        if (orderDiff != 0) {
            return orderDiff;
        } else {
            // A later generated profile has a higher priority/order
            return this.id - o.id;
        }
    }
}

