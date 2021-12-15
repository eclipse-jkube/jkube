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
package org.eclipse.jkube.vertx.generator;

public class Constants {

    static final String VERTX_MAVEN_PLUGIN_GROUP = "io.reactiverse";
    static final String VERTX_MAVEN_PLUGIN_ARTIFACT = "vertx-maven-plugin";
    static final String VERTX_GRADLE_PLUGIN_GROUP = "io.vertx";
    static final String VERTX_GRADLE_PLUGIN_ARTIFACT = "io.vertx.vertx-plugin";
    static final String SHADE_PLUGIN_GROUP = "org.apache.maven.plugins";
    static final String SHADE_PLUGIN_ARTIFACT = "maven-shade-plugin";
    static final String VERTX_GROUPID = "io.vertx";

    static final String VERTX_DROPWIZARD = "vertx-dropwizard-metrics";
    static final String VERTX_INFINIPAN = "vertx-infinispan";

    static final String CLUSTER_MANAGER_SPI = "META-INF/services/io.vertx.core.spi.cluster.ClusterManager";

    private Constants() { }
}
