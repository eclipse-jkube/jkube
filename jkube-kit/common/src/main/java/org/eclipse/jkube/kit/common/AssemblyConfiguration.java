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
package org.eclipse.jkube.kit.common;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

import lombok.AccessLevel;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Singular;

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
     * <p> If an external Dockerfile is used then this name is also the relative directory which contains the assembly
     * files.
     */
    private String name;
    /**
     * Directory under which the files and artifacts contained in the assembly will be copied within the container.
     *
     * <p> The default value for this is <code>/&lt;assembly name&gt;</code>, so <code>/maven</code> if name is not set
     * to a different value.
     */
    private String targetDir;
    /**
     * Whether the target directory should be exported as a volume.
     */
    private Boolean exportTargetDir;
    /**
     * By default, the project's final artifact will be included in the assembly, set this flag to true in case the
     * artifact should be excluded from the assembly.
     *
     * @param excludeFinalOutputArtifact set if artifact must be excluded from the assembly.
     * @return true if artifact must be excluded from the assembly false otherwise.
     */
    private boolean excludeFinalOutputArtifact;
    /**
     * Permission of the files to add.
     */
    private PermissionMode permissions;
    /**
     * Mode how the assembled files should be collected.
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
     * @deprecated Use {@link #layers} instead
     */
    @Deprecated
    private Assembly inline;
    /**
     * Each of the layers ({@link Assembly} for the Container Image.
     */
    @Singular("layer")
    private List<Assembly> layers;
    /**
     * Internal field.
     *
     * <p>true if this image has been flattened ({@link #getFlattenedClone(JKubeConfiguration)}), false otherwise
     */
    @Getter(value = AccessLevel.PRIVATE)
    @Setter(value = AccessLevel.PRIVATE)
    private boolean flattened;

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


    /**
     * @deprecated Use {@link #getLayers()} instead
     */
    @Deprecated
    public Assembly getInline() {
        return Optional.ofNullable(getLayers())
            .filter(l -> !l.isEmpty())
            .map(l -> l.get(l.size() - 1))
            .orElse(null);
    }

    /**
     * @deprecated Use {@link #layers} instead
     */
    @Deprecated
    public void setInline(Assembly inline) {
        this.inline = inline;
    }

    @Nonnull
    public List<Assembly> getLayers() {
        final List<Assembly> ret = new ArrayList<>();
        if (layers != null) {
            ret.addAll(layers);
        }
        if (inline != null) {
            ret.add(inline);
        }
        return ret;
    }

    @Nonnull
    public List<Assembly> getProcessedLayers(@Nonnull JKubeConfiguration configuration) {
        final List<Assembly> originalLayers = getLayers();
        if (flattened) {
            return originalLayers;
        }
        final List<Assembly> ret = new ArrayList<>();
        if (originalLayers.size() == 1 && StringUtils.isBlank(originalLayers.iterator().next().getId())) {
            ret.add(originalLayers.iterator().next().toBuilder().id("jkube-generated-layer-original").build());
        } else {
            ret.addAll(originalLayers);
        }
        final File finalArtifactFile = JKubeProjectUtil.getFinalOutputArtifact(configuration.getProject());
        if (!isExcludeFinalOutputArtifact() && finalArtifactFile != null) {
            ret.add(Assembly.builder()
                .id("jkube-generated-layer-final-artifact")
                .file(AssemblyFile.builder()
                    .source(finalArtifactFile)
                    .destName(finalArtifactFile.getName())
                    .outputDirectory(new File(".")).build())
                .build());
        }
        return ret;
    }

    @Nonnull
    private Assembly getLayersFlattened(@Nonnull JKubeConfiguration configuration) {
        final Assembly.AssemblyBuilder assemblyBuilder = Assembly.builder();
        getProcessedLayers(configuration).forEach(layer -> {
            if (layer.getFileSets() != null) {
                layer.getFileSets().forEach(assemblyBuilder::fileSet);
            }
            if (layer.getFiles() != null) {
                layer.getFiles().forEach(assemblyBuilder::file);
            }
        });
        return assemblyBuilder.build();
    }

    @Nonnull
    public AssemblyConfiguration getFlattenedClone(@Nonnull JKubeConfiguration configuration) {
        if (isFlattened()) {
            throw new IllegalStateException("This image has already been flattened, you can only flatten the image once");
        }
        return toBuilder()
            .flattened(true).inline(null).clearLayers().layer(getLayersFlattened(configuration))
            .build();
    }

    public static class AssemblyConfigurationBuilder {

        private AssemblyConfigurationBuilder flattened(boolean flattened) {
            this.flattened = flattened;
            return this;
        }

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
