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
package io.jkube.kit.common.util;
/**
 * @author roland
 * @since 03/06/16
 */
public enum ResourceClassifier {

    OPENSHIFT("openshift"),
    KUBERNETES("kubernetes"),
    KUBERNETES_TEMPLATE("k8s-template");

    private final String classifier;

    ResourceClassifier(String classifier) {
        this.classifier = classifier;
    }

    public String getValue() {
        return classifier;
    }
}
