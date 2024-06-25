/*
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
package org.eclipse.jkube.kit.build.api.config.property;


/**
 * Enum holding possible configuration keys
 *
 * @author roland
 */
public enum ConfigKey {

    ALIAS,
    ARGS(ValueCombinePolicy.MERGE),
    ASSEMBLY_NAME("assembly.name"),
    ASSEMBLY_TARGET_DIR("assembly.targetDir"),
    ASSEMBLY_EXPORT_TARGET_DIR("assembly.exportTargetDir"),
    ASSEMBLY_PERMISSIONS("assembly.permissions"),
    ASSEMBLY_USER("assembly.user"),
    ASSEMBLY_MODE("assembly.mode"),
    ASSEMBLY_TARLONGFILEMODE("assembly.tarLongFileMode"),
    ASSEMBLY_EXCLUDE_FINAL_OUTPUT_ARTIFACT("assembly.excludeFinalOutputArtifact"),
    BUILDPACKS_BUILDER_IMAGE("buildpacksBuilderImage"),
    BUILD_OPTIONS,
    CLEANUP,
    NOCACHE,
    CACHEFROM,
    CREATE_IMAGE_OPTIONS("createImageOptions"),
    OPTIMISE,
    CMD,
    CONTEXT_DIR,
    DOCKER_ARCHIVE,
    DOCKER_FILE,
    ENTRYPOINT,
    ENV,
    ENV_BUILD("envBuild", ValueCombinePolicy.MERGE),
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
    IMAGE_PULL_POLICY,
    LABELS(ValueCombinePolicy.MERGE),
    MAINTAINER,
    NAME,
    PLATFORMS(ValueCombinePolicy.MERGE),
    PORTS(ValueCombinePolicy.MERGE),
    REGISTRY,
    SHELL,
    RUN,
    SKIP,
    TAGS(ValueCombinePolicy.MERGE),
    USER,
    VOLUMES,
    WATCH_INTERVAL("watch.interval"),
    WATCH_MODE("watch.mode"),
    WATCH_POSTGOAL("watch.postGoal"),
    WATCH_POSTEXEC("watch.postExec"),
    WORKDIR;

    ConfigKey() {
        this(ValueCombinePolicy.REPLACE);
    }

    ConfigKey(String key) {
        this(key, ValueCombinePolicy.REPLACE);
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
