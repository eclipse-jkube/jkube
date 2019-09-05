package io.jshift.kit.config.service;

/**
 * @author nicola
 * @since 17/02/2017
 */
public class JshiftServiceException extends Exception {

    public JshiftServiceException() {
    }

    public JshiftServiceException(String message) {
        super(message);
    }

    public JshiftServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public JshiftServiceException(Throwable cause) {
        super(cause);
    }

    public JshiftServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
