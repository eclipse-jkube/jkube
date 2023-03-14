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
package org.eclipse.jkube.kit.build.service.docker.config.handler.property;


/**
 * Enum holding possible configuration keys
 *
 * @author roland
 */
public enum ConfigKey {

    ALIAS,
    ARGS(ValueCombinePolicy.Merge),
    ASSEMBLY_BASEDIR("assembly.baseDir"),
    ASSEMBLY_EXPORT_TARGET_DIR("assembly.exportTargetDir"),
    ASSEMBLY_PERMISSIONS("assembly.permissions"),
    ASSEMBLY_USER("assembly.user"),
    ASSEMBLY_MODE("assembly.mode"),
    ASSEMBLY_TARLONGFILEMODE("assembly.tarLongFileMode"),
    AUTO_REMOVE,
    BIND,
    BUILD_OPTIONS,
    CAP_ADD,
    CAP_DROP,
    CLEANUP,
    CONTAINER_NAME_PATTERN,
    CPUSHARES,
    CPUS,
    CPUSET,
    NOCACHE,
    CACHEFROM,
    OPTIMISE,
    CMD,
    CONTEXT_DIR,
    DEPENDS_ON,
    DOMAINNAME,
    DNS,
    DNS_SEARCH,
    DOCKER_ARCHIVE,
    DOCKER_FILE,
    ENTRYPOINT,
    ENV,
    ENV_PROPERTY_FILE,
    ENV_BUILD("envBuild", ValueCombinePolicy.Merge),
    ENV_RUN("envRun", ValueCombinePolicy.Merge),
    EXPOSED_PROPERTY_KEY,
    EXTRA_HOSTS,
    FILTER,
    FROM,
    FROM_EXT,
    HEALTHCHECK,
    HEALTHCHECK_MODE("healthcheck.mode"),
    HEALTHCHECK_INTERVAL("healthcheck.interval"),
    HEALTHCHECK_TIMEOUT("healthcheck.timeout"),
    HEALTHCHECK_START_PERIOD("healthcheck.startPeriod"),
    HEALTHCHECK_RETRIES("healthcheck.retries"),
    HEALTHCHECK_CMD("healthcheck.cmd"),
    HOSTNAME,
    IMAGE_PULL_POLICY_BUILD("imagePullPolicy.build"),
    IMAGE_PULL_POLICY_RUN("imagePullPolicy.run"),
    LABELS(ValueCombinePolicy.Merge),
    LINKS,
    LOG_ENABLED("log.enabled"),
    LOG_PREFIX("log.prefix"),
    LOG_DATE("log.date"),
    LOG_FILE("log.file"),
    LOG_COLOR("log.color"),
    LOG_DRIVER_NAME("log.driver.name"),
    LOG_DRIVER_OPTS("log.driver.opts"),
    MAINTAINER,
    MEMORY,
    MEMORY_SWAP,
    NAME,
    NET,
    NETWORK_MODE("network.mode"),
    NETWORK_NAME("network.name"),
    NETWORK_ALIAS("network.alias"),
    PORT_PROPERTY_FILE,
    PORTS(ValueCombinePolicy.Merge),
    PRIVILEGED,
    READ_ONLY,
    REGISTRY,
    RESTART_POLICY_NAME("restartPolicy.name"),
    RESTART_POLICY_RETRY("restartPolicy.retry"),
    SHELL,
    RUN,
    SECURITY_OPTS,
    SHMSIZE,
    SKIP_BUILD("skip.build"),
    SKIP_RUN("skip.run"),
    TAGS(ValueCombinePolicy.Merge),
    TMPFS,
    ULIMITS,
    USER,
    VOLUMES,
    VOLUMES_FROM,
    WAIT_LOG("wait.log"),
    WAIT_TIME("wait.time"),
    WAIT_HEALTHY("wait.healthy"),
    WAIT_URL("wait.url"),
    WAIT_HTTP_URL("wait.http.url"),
    WAIT_HTTP_METHOD("wait.http.method"),
    WAIT_HTTP_STATUS("wait.http.status"),
    WAIT_KILL("wait.kill"),
    WAIT_EXEC_POST_START("wait.exec.postStart"),
    WAIT_EXEC_PRE_STOP("wait.exec.preStop"),
    WAIT_EXEC_BREAK_ON_ERROR("wait.exec.breakOnError"),
    WAIT_EXIT("wait.exit"),
    WAIT_SHUTDOWN("wait.shutdown"),
    WAIT_TCP_MODE("wait.tcp.mode"),
    WAIT_TCP_HOST("wait.tcp.host"),
    WAIT_TCP_PORT("wait.tcp.port"),
    WATCH_INTERVAL("watch.interval"),
    WATCH_MODE("watch.mode"),
    WATCH_POSTGOAL("watch.postGoal"),
    WATCH_POSTEXEC("watch.postExec"),
    WORKDIR,
    WORKING_DIR;

    ConfigKey() {
        this(ValueCombinePolicy.Replace);
    }

    ConfigKey(String key) {
        this(key, ValueCombinePolicy.Replace);
    }

    ConfigKey(ValueCombinePolicy valueCombinePolicy) {
        this.key = toVarName(name());
        this.valueCombinePolicy = valueCombinePolicy;
    }

    ConfigKey(String key, ValueCombinePolicy valueCombinePolicy) {
        this.key = key;
        this.valueCombinePolicy = valueCombinePolicy;
    }

    private final String key;
    private final ValueCombinePolicy valueCombinePolicy;

    private static final String DEFAULT_PREFIX = "docker";

    // Convert to camel case
    private String toVarName(String s) {
        String[] parts = s.split("_");
        StringBuilder stringBuilder = new StringBuilder(parts[0].toLowerCase());
        for (int i = 1; i < parts.length; i++) {
            stringBuilder.append(parts[i].substring(0, 1).toUpperCase()).append(parts[i].substring(1).toLowerCase());
        }
        return stringBuilder.toString();
    }

    public String asPropertyKey(String prefix) {
        return prefix + "." + key;
    }

    public String asPropertyKey() {
        return DEFAULT_PREFIX + "." + key;
    }

    public ValueCombinePolicy getValueCombinePolicy() {
        return valueCombinePolicy;
    }
}
