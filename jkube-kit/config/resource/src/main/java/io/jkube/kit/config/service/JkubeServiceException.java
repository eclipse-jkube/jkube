package io.jkube.kit.config.service;

/**
 * @author nicola
 * @since 17/02/2017
 */
public class JkubeServiceException extends Exception {

    public JkubeServiceException() {
    }

    public JkubeServiceException(String message) {
        super(message);
    }

    public JkubeServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public JkubeServiceException(Throwable cause) {
        super(cause);
    }

    public JkubeServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
