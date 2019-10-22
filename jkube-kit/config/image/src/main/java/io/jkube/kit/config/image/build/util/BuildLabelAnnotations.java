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
package io.jkube.kit.config.image.build.util;

public enum BuildLabelAnnotations {
    BUILD_DATE("build-date"),
    NAME("name"),
    DESCRIPTION("description"),
    USAGE("usage"),
    URL("url"),
    VCS_URL("vcs-url"),
    VCS_REF("vcs-ref"),
    VENDOR("vendor"),
    VERSION("version"),
    SCHEMA_VERSION("schema-version");

    private final String annotation;

    BuildLabelAnnotations(String anno) {
        this.annotation = "org.label-schema." + anno;
    }

    public String value() {
        return annotation;
    }

    @Override
    public String toString() {
        return value();
    }
}
