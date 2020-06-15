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

@SuppressWarnings("JavaDoc")
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@EqualsAndHashCode
public class AssemblyConfiguration implements Serializable {

    /**
     * Assembly name, which is maven by default. This name is used for the archives and directories created during the
     * build.
     *
     * <p> If an external Dockerfile is used than this name is also the relative directory which contains the assembly
     * files.
     */
    private String name;
    /**
     * Directory under which the files and artifacts contained in the assembly will be copied within the container.
     *
     * <p> The default value for this is <code>/&lt;assembly name&gt;</code>, so <code>/maven</code> if name is not set
     * to a different value.
     *
     * <p> This option has no meaning when an external Dockerfile is used.
     */
    private String targetDir;
    /**
     * Path to an assembly descriptor file.
     */
    private String descriptor;
    /**
     * Alias to a predefined assembly descriptor.
     */
    private String descriptorRef;
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
    /**
     * Permission of the files to add
     */
    private PermissionMode permissions;
    /**
     * Mode how the assembled files should be collected:
     */
    private AssemblyMode mode;
    /**
     * User and/or group under which the files should be added. The user must already exist in the base image.
     *
     * <p> It has the general format user[:group[:run-user]]. The user and group can be given either as numeric user
     * and group-id or as names. The group id is optional.
     *
     * <p> If a third part is given, then the build changes to user root before changing the ownerships, changes the
     * ownerships and then change to user run-user which is then used for the final command to execute.
     */
    private String user;
    /**
     * Sets the TarArchiver behaviour on file paths with more than 100 characters length.
     *
     * <p> Valid values are: "warn"(default), "fail", "truncate", "gnu", "posix", "posix_warn" or "omit".
     */
    private String tarLongFileMode;
    /**
     * Assembly defined inline in the pom.xml
     */
    private Assembly inline;

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
         * Respect the assembly provided permissions
         */
        keep,

        /**
         * Ignore permission when using an assembly mode of "dir"
         */
        ignore
    }
}
