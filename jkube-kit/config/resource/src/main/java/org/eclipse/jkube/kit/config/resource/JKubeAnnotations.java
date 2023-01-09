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
package org.eclipse.jkube.kit.config.resource;

/**
 * @author roland
 * @since 23.05.17
 */
public enum JKubeAnnotations {

    BUILD_ID("build-id"),
    BUILD_URL("build-url"),

    GIT_COMMIT("git-commit"),
    GIT_URL("git-url"),
    GIT_BRANCH("git-branch"),
    GIT_CLONE_URL("git-clone-url"),
    GIT_LOCAL_CLONE_URL("local-git-url"),

    DOCS_URL("docs-url"),

    ISSUE_SYSTEM("issue-system"),
    ISSUE_TRACKER_URL("issue-tracker-url"),

    SCM_TAG("scm-tag"),
    SCM_URL("scm-url"),

    TARGET_PLATFORM("target-platform"),
    ICON_URL("iconUrl");
    private static final String JKUBE_ANNOTATION_PREFIX = "jkube.eclipse.org";
    /**
     * @deprecated in favor of <code>jkube.eclipse.org</code>
     */
    @Deprecated
    private static final String DEPRECATED_JKUBE_ANNOTATION_PREFIX = "jkube.io";
    private final String annotation;

    JKubeAnnotations(String anno) {
        this.annotation = "/" + anno;
    }

    public String value() {
        return value(false);
    }

    public String value(boolean useDeprecatedPrefix) {
        if (useDeprecatedPrefix) {
            return DEPRECATED_JKUBE_ANNOTATION_PREFIX + annotation;
        }
        return JKUBE_ANNOTATION_PREFIX + annotation;
    }

    @Override
    public String toString() {
        return value();
    }

}

