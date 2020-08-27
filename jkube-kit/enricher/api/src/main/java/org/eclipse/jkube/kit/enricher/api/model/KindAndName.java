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
package org.eclipse.jkube.kit.enricher.api.model;

import io.fabric8.kubernetes.api.model.HasMetadata;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;


/**
 * Represents a key for a resource so we can look up resources by key and name
 */
public class KindAndName {
    private final String kind;
    private final String name;

    public KindAndName(String kind, String name) {
        this.kind = kind;
        this.name = name;
    }

    public KindAndName(HasMetadata item) {
        this(KubernetesHelper.getKind(item), KubernetesHelper.getName(item));
    }

    public String getKind() {
        return kind;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "KindAndName{" +
                "kind='" + kind + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        KindAndName that = (KindAndName) o;

        if (!kind.equals(that.kind))
            return false;

        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        int result = kind.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }
}

