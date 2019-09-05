package io.jshift.kit.config.resource;


import java.util.Objects;
import java.util.Properties;


/**
 * Mode on how to build/deploy resources onto Kubernetes/Openshift cluster.
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
     * Openshift.
     */
    kubernetes(false, "Kubernetes"),

    /**
     * Use special OpenShift features like BuildConfigs, DeploymentConfigs
     * ImageStreams and S2I builds while deploying onto cluster. It can be
     * used only when on Openshift.
     */
    openshift(false, "OpenShift"),

    /**
     * Detect automatically whether running cluster is OpenShift or Kuberentes.
     * This is done by contacting cluster API server.
     */
    auto(true, "Auto");

    public static final RuntimeMode DEFAULT = RuntimeMode.auto;
    public static final String FABRIC8_EFFECTIVE_PLATFORM_MODE = "jshift.internal.effective.platform.mode";

    private boolean autoFlag;
    private String label;

    RuntimeMode(boolean autoFlag, String label) {
        this.autoFlag = autoFlag;
        this.label = label;
    }

    public boolean isAuto() {
        return autoFlag;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Returns true if the given maven properties indicate running in OpenShift platform mode
     */
    public static boolean isOpenShiftMode(Properties properties) {
        return properties == null ? false : Objects.equals(openshift.toString(), properties.getProperty(FABRIC8_EFFECTIVE_PLATFORM_MODE, ""));
    }
}

