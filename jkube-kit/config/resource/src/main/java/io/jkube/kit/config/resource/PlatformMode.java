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


/**
 * Mode how to create resource descriptors used by enrichers.
 *
 * @author roland
 * @since 25/05/16
 */
public enum PlatformMode {

    /**
     * Create resources descriptors for vanilla Kubernetes
     */
    kubernetes("Kubernetes"),

    /**
     * Use special OpenShift features like BuildConfigs
     */
    openshift("OpenShift");

    private String label;

    PlatformMode(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

}
