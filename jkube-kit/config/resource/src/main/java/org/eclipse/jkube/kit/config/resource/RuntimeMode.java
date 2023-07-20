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
package org.eclipse.jkube.kit.config.resource;


import java.util.Objects;
import java.util.Properties;


/**
 * Mode on how to build/deploy resources onto Kubernetes/OpenShift cluster.
 * It can be provided by user as a parameter, otherwise it would be
 * automatically detected by plugin via querying cluster.
 *
 * @author Rohan
 * @since 14/02/2019
 */
public enum RuntimeMode {

    /**
     * Build Docker images and use plain Deployments for deployment
     * onto cluster. It can be used both on vanilla Kubernetes and
     * OpenShift.
     */
    KUBERNETES("Kubernetes"),

    /**
     * Use special OpenShift features like BuildConfigs, DeploymentConfigs
     * ImageStreams and S2I builds while deploying onto cluster. It can be
     * used only when on OpenShift.
     */
    OPENSHIFT("OpenShift");

    public static final String JKUBE_EFFECTIVE_PLATFORM_MODE = "jkube.internal.effective.platform.mode";

    private String label;

    RuntimeMode(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Returns true if the given maven properties indicate running in OpenShift platform mode
     */
    public static boolean isOpenShiftMode(Properties properties) {
        return properties != null && Objects.equals(OPENSHIFT.toString(), properties.getProperty(JKUBE_EFFECTIVE_PLATFORM_MODE, ""));
    }
}

