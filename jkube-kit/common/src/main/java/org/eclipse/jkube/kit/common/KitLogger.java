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

/**
 * @author roland
 * @since 30.05.17
 */
public interface KitLogger {

    /**
     * Debug message if debugging is enabled.
     *
     * @param format debug message format
     * @param params parameter for formatting message
     */
    void debug(String format, Object... params);

    /**
     * Informational message
     *
     * @param format info message format
     * @param params parameter for formatting message
     */
    void info(String format, Object... params);

    /**
     * Verbose message
     *
     * @param format verbose message format
     * @param params parameter for formatting message
     */
    default void verbose(String format, Object... params) {
        if (isVerboseEnabled()) {
            info(format, params);
        }
    }

    /**
     * A warning.
     *
     * @param format warning message format
     * @param params parameter for formatting message
     */
    void warn(String format, Object... params);

    /**
     * Severe errors
     *
     * @param format error message format
     * @param params parameter for formatting message
     */
    void error(String format, Object... params);

    /**
     * Whether debugging is enabled.
     *
     * @return boolean value indicating debug is enabled or not.
     */
    boolean isDebugEnabled();

    /**
     * Whether verbose is enabled
     *
     * @return boolean value indicating verbose is enabled or not.
     */
    default boolean isVerboseEnabled() {
        return false;
    }


    // ================================================================================
    // Progress handling, by default disabled

    /**
     * Start a progress bar* @param total the total number to be expected
     */
    default void progressStart() {}

    /**
     * Update the progress
     *
     * @param layerId the image id of the layer fetched
     * @param status a status message
     * @param progressMessage the progressBar
     */
    default void progressUpdate(String layerId, String status, String progressMessage) {}

    /**
     * Finis progress meter. Must be always called if {@link #progressStart()} has been
     * used.
     */
    default void progressFinished() {}

    class StdoutLogger implements KitLogger {
        @Override
        public void debug(String format, Object... params) {
            System.out.println(String.format(format,params));
        }

        @Override
        public void info(String format, Object... params) {
            System.out.println(String.format(format,params));
        }

        @Override
        public void warn(String format, Object... params) {
            System.out.println(String.format(format,params));
        }

        @Override
        public void error(String format, Object... params) {
            System.out.println(String.format(format,params));
        }

        @Override
        public boolean isDebugEnabled() {
            return true;
        }

    }

    enum LogVerboseCategory {
        BUILD("build"), API("api");

        private String category;

        LogVerboseCategory(String category) {
            this.category = category;
        }

        public String getValue() {
            return category;
        }
    }
}
