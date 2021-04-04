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
package org.eclipse.jkube.kit.enricher.api;
/**
 * Enum describing the object types which are created
 *
 * @author roland
 * @since 07/04/16
 */
public enum Kind {
    SERVICE,
    REPLICA_SET,
    REPLICATION_CONTROLLER,
    DEPLOYMENT,
    DEPLOYMENT_CONFIG,
    DAEMON_SET,
    STATEFUL_SET,
    IMAGESTREAM,
    JOB,
    POD_SPEC,
    BUILD_CONFIG,
    BUILD,
    SERVICE_ACCOUNT,
    INGRESS;


    /**
     * Returns true if the kind is a controller
     *
     * @return boolean value whether it's controller or not
     */
    public boolean isController() {
        return isPodController() ||
               isDeployment() ||
               this == Kind.DAEMON_SET || this == Kind.STATEFUL_SET || this == Kind.JOB;
    }

    public boolean isPodController() {
        return this == Kind.REPLICA_SET || this == REPLICATION_CONTROLLER;
    }

    public boolean isDeployment() {
        return this == Kind.DEPLOYMENT || this == DEPLOYMENT_CONFIG;
    }

    public boolean hasNoVersionInSelector() {
        return this == Kind.SERVICE || this == Kind.DEPLOYMENT || this == Kind.DEPLOYMENT_CONFIG ||
               this == Kind.DAEMON_SET || this == Kind.JOB || this == Kind.STATEFUL_SET;
    }
}