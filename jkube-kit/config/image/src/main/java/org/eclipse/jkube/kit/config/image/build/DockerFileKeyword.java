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
package org.eclipse.jkube.kit.config.image.build;

/**
 * Fields for  a dockerfile
 * @author Paris Apostolopoulos ;&lt;javapapo@mac.com;&gt;
 * @author Christian Fischer ;&lt;sw-dev@computerlyrik.de;&gt;
 * @since 13.06.05
 */
public enum DockerFileKeyword
{
    MAINTAINER,
    EXPOSE,
    FROM,
    RUN,
    WORKDIR,
    ENTRYPOINT,
    CMD,
    USER,
    ENV,
    ARG,
    LABEL,
    COPY,
    VOLUME,
    HEALTHCHECK,
    NONE;

    /**
     * Append this keyword + optionally some args to a {@link StringBuilder} plus a trailing newline.
     *
     * @param sb stringbuilder to add to
     * @param args args added (space separated)
     */
    public void addTo(StringBuilder sb, String... args) {
        addTo(sb, true, args);
    }

    /**
     * Append this keyword + optionally some args to a {@link StringBuilder} and a optional trailing newline.
     *
     * @param sb stringbuilder to add to
     * @param newline flag indicating whether a new line should be added
     * @param args args added (space separated)
     */
    public void addTo(StringBuilder sb, boolean newline, String... args) {
        sb.append(name());
        for (String arg : args) {
            sb.append(" ").append(arg);
        }
        if (newline) {
            sb.append("\n");
        }
    }
}
