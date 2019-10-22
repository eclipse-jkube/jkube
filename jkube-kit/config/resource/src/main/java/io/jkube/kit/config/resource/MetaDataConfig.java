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
package io.jkube.kit.config.resource;

import java.util.Properties;

/**
 * Configuration for labels or annotations
 *
 * @author roland
 * @since 22/03/16
 */
public class MetaDataConfig {
    /**
     * Labels or annotations which should be applied to every object
     */
    private Properties all;

    /**
     * Labels or annotation for a Pod within a controller or deployment
     */
    private Properties pod;

    /**
     * Labels or annotations for replica sets (or replication controller)
     */
    private Properties replicaSet;

    /**
     * Labels or annotation for services
     */
    private Properties service;

    /**
     * Labels or annotations for deployment or deployment configs
     */
    private Properties ingress;

    /**
     * Labels or annotations for deployment or deployment configs
     */
    private Properties deployment;

    public Properties getPod() {
        return pod;
    }

    public Properties getReplicaSet() {
        return replicaSet;
    }

    public Properties getService() {
        return service;
    }

    public Properties getAll() {
        return all;
    }

    public Properties getDeployment() {
        return deployment;
    }

    public Properties getIngress() {
        return ingress;
    }
}
