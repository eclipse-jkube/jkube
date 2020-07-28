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

import java.io.File;
import java.io.Serializable;

@SuppressWarnings("JavaDoc")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class AssemblyFile implements Serializable {

    private static final long serialVersionUID = -5232977405418412052L;

    /**
     * Absolute or relative path from the project's directory of the file to be included in the assembly.
     *
     * @param source New source for the assembly file.
     * @return The assembly source file.
     */
    private File source;
    /**
     * Output directory relative to the root of the root directory of the assembly.
     *
     * @param outputDirectory New output directory for the assembly file.
     * @return The assembly output directory.
     */
    private File outputDirectory;
    /**
     * Destination filename in the outputDirectory.
     *
     * @param destName New destination filename.
     * @return The assembly destination filename.
     */
    private String destName;
    /**
     * Similar to a UNIX permission, sets the file mode of the file included. THIS IS AN OCTAL VALUE.
     *
     * <p> Format: (User)(Group)(Other) where each component is a sum of Read = 4, Write = 2, and Execute = 1.
     *
     * <p> For example, the value 0644 translates to User read-write, Group and Other read-only.
     *
     * @param fileMode New file mode value for the assembly fileSet.
     * @return The assembly fileSet file mode value.
     */
    private String fileMode;


    // Plexus deserialization specific setters
    /**
     * Output directory relative to the root of the root directory of the assembly.
     *
     * @param outputDirectory New output directory for the assembly file.
     */
    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = new File(outputDirectory);
    }
}
