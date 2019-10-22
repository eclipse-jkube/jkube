package io.jkube.kit.build.service.docker.config;

import java.io.Serializable;


/**
 * @author roland
 * @since 08/12/14
 */
public class RestartPolicy implements Serializable {

    public static final RestartPolicy DEFAULT = new RestartPolicy();

    private String name;

    private int retry;

    public RestartPolicy() {};

    public String getName() {
        return name;
    }

    public int getRetry() {
        return retry;
    }

    // ================================================

    public static class Builder {

        private RestartPolicy policy = new RestartPolicy();

        public Builder name(String name) {
            policy.name = name;
            return this;
        }

        public Builder retry(int retry) {
            policy.retry = retry;
            return this;
        }

        public RestartPolicy build() {
            return policy;
        }
    }
}

