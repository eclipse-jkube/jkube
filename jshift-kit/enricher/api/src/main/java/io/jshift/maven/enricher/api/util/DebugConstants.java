package io.jshift.maven.enricher.api.util;

/**
 * Some constants for Java Debugging on Kubernetes
 */
public class DebugConstants {
    public static final String ENV_VAR_JAVA_DEBUG = "JAVA_ENABLE_DEBUG";
    public static final String ENV_VAR_JAVA_DEBUG_SUSPEND = "JAVA_DEBUG_SUSPEND";
    public static final String ENV_VAR_JAVA_DEBUG_SESSION = "JAVA_DEBUG_SESSION";
    public static final String ENV_VAR_JAVA_DEBUG_PORT = "JAVA_DEBUG_PORT";
    public static final String ENV_VAR_JAVA_DEBUG_PORT_DEFAULT = "5005";
}

