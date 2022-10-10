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
package org.eclipse.jkube.kit.config.service.openshift;

import io.fabric8.mockwebserver.utils.ResponseProvider;
import okhttp3.Headers;
import okhttp3.mockwebserver.RecordedRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.fail;

/**
 * A utility class to record http request events.
 */
public class WebServerEventCollector {

    private final Queue<String> events;
    private final List<String> bodies;

    public WebServerEventCollector() {
        events = new ConcurrentLinkedQueue<>();
        bodies = new ArrayList<>();
    }

    public List<String> getBodies() {
        return bodies;
    }

    public void assertEventsRecorded(String... expectedEvents) {
        for (String exp : expectedEvents) {
            if (!events.contains(exp)) {
                fail("Event '" + exp + "' was not found. Expected: " + Arrays.asList(expectedEvents) + ", found: " + this.events);
            }
        }
    }

    public void assertEventsNotRecorded(String... expectedEvents) {
        for (String exp : expectedEvents) {
            if (events.contains(exp)) {
                fail("Event '" + exp + "' was found. Expected not to find: " + Arrays.asList(expectedEvents) + ", found: " + this.events);
            }
        }
    }

    public void assertEventsRecordedInOrder(String... expectedEvents) {
        LinkedList<String> evts = new LinkedList<>(this.events);
        for (String exp : expectedEvents) {
            boolean found = false;
            while (!found && evts.size() > 0) {
                String ev = evts.pop();
                found = exp.equals(ev);
            }

            if (!found) {
                fail("Event '" + exp + "' was not found in order. Expected: " + Arrays.asList(expectedEvents) + ", found: " + this.events);
            }
        }
    }

    public WebServerEventRecorder record(String event) {
        return new WebServerEventRecorder(event);
    }

    public class WebServerEventRecorder {

        private final String event;

        public WebServerEventRecorder(String event) {
            this.event = event;
        }

        public ResponseProvider<Object> andReturn(final int statusCode, final Object body) {
            return new ResponseProvider<Object>() {

                private Headers headers = new Headers.Builder().build();

                @Override
                public int getStatusCode(RecordedRequest recordedRequest) {
                    return statusCode;
                }

                @Override
                public Headers getHeaders() {
                    return headers;
                }

                @Override
                public void setHeaders(Headers headers) {
                    this.headers = headers;
                }

                @Override
                public Object getBody(RecordedRequest recordedRequest) {
                    WebServerEventCollector.this.bodies.add(recordedRequest.getBody().readUtf8());
                    WebServerEventCollector.this.events.add(event);
                    return body;
                }
            };
        }

    }

}
