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
package org.eclipse.jkube.kit.common;

import java.io.Serializable;
import java.util.Optional;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@EqualsAndHashCode
public class AssemblyConfiguration implements Serializable {

    /**
     * New replacement for base directory which better reflects its
     * purpose.
     */
    private String targetDir;
    /**
     * Name of the assembly which is used also as name of the archive
     * which is created and has to be used when providing an own Dockerfile.
     */
    private String name;
    private String descriptor;
    private String descriptorRef;
    /**
     * @deprecated use 'exportTargetDir' instead
     */
    @Deprecated
    private Boolean exportBasedir;
    @Deprecated
    private String dockerFileDir;
    @Deprecated
    private Boolean ignorePermissions;
    /**
     * Whether the target directory should be exported.
     */
    private Boolean exportTargetDir;
    /**
     * Java Project final artifact will be excluded from the assembly if this flag is set to true.
     *
     * @param excludeFinalOutputArtifact set if artifact must be excluded from the assembly.
     * @return true if artifact must be excluded from the assembly false otherwise.
     */
    private boolean excludeFinalOutputArtifact;
    private PermissionMode permissions;
    private AssemblyMode mode;
    private String user;
    private String tarLongFileMode;
    /**
     * Assembly defined inline in the pom.xml
     */
    private Assembly inline;

    public Boolean exportTargetDir() {
        if (exportTargetDir != null) {
            return exportTargetDir;
        } else if (exportBasedir != null) {
            return exportBasedir;
        } else {
            return null;
        }
    }

    public AssemblyMode getMode() {
        return mode != null ? mode : AssemblyMode.dir;
    }

    public String getModeRaw() {
        return mode != null ? mode.name() : null;
    }

    public PermissionMode getPermissions() {
        return permissions != null ? permissions : PermissionMode.keep;
    }

    public String getPermissionsRaw() {
        return permissions != null ? permissions.name() : null;
    }


    public static class AssemblyConfigurationBuilder {

        public AssemblyConfigurationBuilder permissionsString(String permissionsString) {
            permissions = Optional.ofNullable(permissionsString)
                .map(String::toLowerCase).map(PermissionMode::valueOf).orElse(null);
            return this;
        }

        public AssemblyConfigurationBuilder modeString(String modeString) {
            mode = Optional.ofNullable(modeString)
                .map(String::toLowerCase).map(AssemblyMode::valueOf).orElse(null);
            return this;
        }
    }

    public enum PermissionMode {

        /**
         * Auto detect permission mode
         */
        auto,

        /**
         * Make everything executable
         */
        exec,

        /**
         * Leave all as it is
         */
        keep,

        /**
         * Ignore permission when using an assembly mode of "dir"
         */
        ignore
    }
}
