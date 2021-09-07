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
package org.eclipse.jkube.kit.config.image;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;


/**
 * @author roland
 */
@NoArgsConstructor
@Getter
@EqualsAndHashCode
public class LogConfiguration implements Serializable {

    private static final long serialVersionUID = 6896468158396394236L;

    public static final LogConfiguration DEFAULT = new LogConfiguration(null, null, null, null, null, null, null);

    private boolean enabled;
    private String prefix;
    private String date;
    private String color;
    private String file;
    private LogDriver driver;

    @Builder
    private LogConfiguration(
        Boolean enabled, String prefix, String color, String date, String file, Map<String, String> logDriverOpts, String driverName) {
        this.enabled = Optional.ofNullable(enabled).orElse(false);
        this.prefix = prefix;
        this.date = date;
        this.color = color;
        this.file = file;
        this.driver = Optional.ofNullable(driverName).map(dn -> new LogDriver(dn, logDriverOpts)).orElse(null);
    }

    /**
     * If explicitly enabled, or configured in any way and NOT explicitly disabled, return true.
     *
     * @return whether its activated or not
     */
    public boolean isActivated() {
        return enabled || !isBlank();
    }

    /**
     * Returns true if all options (except enabled) are null, used to decide value of enabled.
     *
     * @return whether its blank or not
     */
    private boolean isBlank() {
        return prefix == null && date == null && color == null && file == null && driver == null;
    }

    public String getFileLocation() {
        return file;
    }

    public static class LogDriver implements Serializable {

        private String name;

        private Map<String, String> opts;

        private LogDriver(String name, Map<String, String> opts) {
            this.name = name;
            this.opts = opts;
        }

        public String getName() {
            return name;
        }

        public Map<String, String> getOpts() {
            return opts;
        }
    }

}

