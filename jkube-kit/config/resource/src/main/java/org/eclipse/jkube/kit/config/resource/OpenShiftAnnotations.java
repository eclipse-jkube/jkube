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

public enum OpenShiftAnnotations {
    VCS_URI("vcs-uri"),
    VCS_REF("vcs-ref"),
    CONNECTS_TO("connects-to"),
    OVERVIEW_APP_ROUTE("overview-app-route");

    private final String annotation;

    OpenShiftAnnotations(String anno) {
        this.annotation = "app.openshift.io/" + anno;
    }

    public String value() {
        return annotation;
    }

    @Override
    public String toString() {
        return value();
    }
}
