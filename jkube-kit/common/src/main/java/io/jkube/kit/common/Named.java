package io.jkube.kit.common;
/**
 * Interface for marking object holding a name
 *
 * @author roland
 * @since 24/07/16
 */
public interface Named {
    /**
     * Get name of this object
     * @return String denoting name
     */
    public String getName();
}

