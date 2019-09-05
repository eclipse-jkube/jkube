/**
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.jshift.maven.enricher.api;
/**
 * Enum describing the object types which are created
 *
 * @author roland
 * @since 07/04/16
 */
public enum Kind {
    SERVICE,
    REPLICA_SET,
    REPLICATION_CONTROLLER,
    DEPLOYMENT,
    DEPLOYMENT_CONFIG,
    DAEMON_SET,
    STATEFUL_SET,
    IMAGESTREAM,
    JOB,
    POD_SPEC,
    BUILD_CONFIG,
    BUILD,
    INGRESS;


    /**
     * Returns true if the kind is a controller
     *
     * @return boolean value whether it's controller or not
     */
    public boolean isController() {
        return isPodController() ||
               isDeployment() ||
               this == Kind.DAEMON_SET || this == Kind.STATEFUL_SET || this == Kind.JOB;
    }

    public boolean isPodController() {
        return this == Kind.REPLICA_SET || this == REPLICATION_CONTROLLER;
    }

    public boolean isDeployment() {
        return this == Kind.DEPLOYMENT || this == DEPLOYMENT_CONFIG;
    }

    public boolean hasNoVersionInSelector() {
        return this == Kind.SERVICE || this == Kind.DEPLOYMENT || this == Kind.DEPLOYMENT_CONFIG ||
               this == Kind.DAEMON_SET || this == Kind.JOB || this == Kind.STATEFUL_SET;
    }
}