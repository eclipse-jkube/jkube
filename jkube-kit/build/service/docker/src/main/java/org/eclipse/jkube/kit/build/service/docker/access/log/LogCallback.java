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
package org.eclipse.jkube.kit.build.service.docker.access.log;

import org.eclipse.jkube.kit.build.service.docker.helper.Timestamp;

import java.io.IOException;

/**
 * Interface called for each log line received from the docker host when asynchronous
 * log fetching is used.
 *
 * @author roland
 * @since 21/11/14
 */
public interface LogCallback {

    /**
     * Receive a log entry
     * @param type 1 for log on standard output, 2 for standard error
     * @param timestamp timestampp on the server side when this entry happened
     * @param txt log output
     * @throws DoneException if thrown will stop the logging.
     */
    void log(int type, Timestamp timestamp, String txt) throws DoneException;

    /**
     * Method called in case on an error when reading the logs
     * @param error error description
     */
    void error(String error);

    /**
     * To be called by a client to start the callback and allocate the underlying output stream.
     * In case of a shared stream it might be that the stream is reused
     *
     * @throws IOException in case of any I/O error
     */
    void open() throws IOException;

    /**
     * Invoked by a user when callback is no longer used.
     * Closing the underlying stream may be delayed by the implementation
     * when this stream is shared between multiple clients.
     */
    void close();

    /**
     * Exception indicating that logging is done and should be finished
     */
    class DoneException extends Exception {}
}

