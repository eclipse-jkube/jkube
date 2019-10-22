package io.jkube.kit.common;



/**
 * @author roland
 * @since 23/07/16
 */
public class PrefixedLogger implements KitLogger {
    private final String prefix;
    private final KitLogger log;

    public PrefixedLogger(String prefix, KitLogger log) {
        this.prefix = prefix;
        this.log = log;
    }

    @Override
    public void debug(String message, Object... objects) {
        log.debug(p(message), objects);
    }

    @Override
    public void info(String message, Object... objects) {
        log.info(p(message),objects);
    }

    @Override
    public void verbose(String message, Object... objects) {
        log.verbose(p(message), objects);
    }

    @Override
    public void warn(String message, Object... objects) {
        log.warn(p(message), objects);
    }

    @Override
    public void error(String message, Object... objects) {
        log.error(p(message), objects);
    }

    @Override
    public boolean isDebugEnabled() {
        return log.isDebugEnabled();
    }

    @Override
    public boolean isVerboseEnabled() {
        return log.isVerboseEnabled();
    }

    @Override
    public void progressStart() {
        log.progressStart();
    }

    @Override
    public void progressUpdate(String s, String s1, String s2) {
        log.progressUpdate(s,s1,s2);
    }

    @Override
    public void progressFinished() {
        log.progressFinished();
    }

    private String p(String message) {
        return prefix + ": " + message;
    }
}

