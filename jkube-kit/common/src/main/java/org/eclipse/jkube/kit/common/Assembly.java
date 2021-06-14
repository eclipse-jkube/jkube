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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Singular;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.Serializable;
import java.util.List;

@SuppressWarnings("JavaDoc")
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class Assembly implements Serializable {

    private static final long serialVersionUID = 4048781747133251206L;

    private static final String LAYER_ID_DEFAULT = "default";

    /**
     * Unique ID for the Assembly.
     *
     * @param id New ID for the Assembly.
     * @return The id of the assembly.
     */
    private String id;
    /**
     * The optional directory for this Assembly layer, to be nested within the AssemblyConfiguration targetDir.
     *
     * <p> Files are copied to this directory (if configured) beneath the
     * global {@link AssemblyConfiguration#getTargetDir()}.
     *
     * <p> This feature is especially interesting for multi-layered Images, since each layer will be
     * copied to its own directory allowing the partial images to be cached across iterative builds.
     */
    private String directory;
    /**
     * List of files for the Assembly.
     *
     * @param files New list of files for the Assembly.
     * @return The Assembly files.
     */
    @Singular
    private List<AssemblyFile> files;
    /**
     * List of filesets for the Assembly.
     *
     * @param fileSets New list of filesets for the Assembly.
     * @return The Assembly filesets.
     */
    @Singular
    private List<AssemblyFileSet> fileSets;
    /**
     * Base directory from which to resolve the Assembly files and filesets.
     *
     * @param baseDirectory New base directory for the Assembly.
     * @return The Assembly base directory.
     */
    private File baseDirectory;

}
