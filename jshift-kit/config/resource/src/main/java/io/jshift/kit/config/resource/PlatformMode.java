package io.jshift.kit.config.resource;


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
