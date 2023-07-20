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
package org.eclipse.jkube.kit.build.service.docker.wait;

/**
 * Interface for various wait checkers
 *
 * @author roland
 * @since 25/03/2017
 */
public interface WaitChecker {
    /**
     * @return true if the the check has succeed, false otherwise
     */
    boolean check();

    /**
     * Cleanup hook which is called after the wait phase.
     */
    void cleanUp();

    /**
     * Get the label to be used in the log
     * @return string value of log label
     */
    String getLogLabel();
}