package io.jkube.maven.enricher.api.model;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.jkube.kit.common.util.KubernetesHelper;


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

