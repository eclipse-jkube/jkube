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
package io.jkube.kit.build.service.docker.access.chunked;

import com.google.gson.JsonObject;
import io.jkube.kit.build.service.docker.access.DockerAccessException;
import io.jkube.kit.common.KitLogger;
import io.jkube.kit.build.service.docker.access.DockerAccessException;

public class PullOrPushResponseJsonHandler implements EntityStreamReaderUtil.JsonEntityResponseHandler {

    private final KitLogger log;

    public PullOrPushResponseJsonHandler(KitLogger log) {
        this.log = log;
    }

    @Override
    public void process(JsonObject json) throws DockerAccessException {
        if (json.has("progressDetail")) {
            log.progressUpdate(getStringOrEmpty(json, "id"),
                               getStringOrEmpty(json, "status"),
                               getStringOrEmpty(json, "progress"));
        } else if (json.has("error")) {
            throwDockerAccessException(json);
        } else {
            log.progressFinished();
            logInfoMessage(json);
            log.progressStart();
        }
    }

    private void logInfoMessage(JsonObject json) {
        String value;
        if (json.has("stream")) {
            value = json.get("stream").getAsString().replaceFirst("\n$", "");
        } else if (json.has("status")) {
            value = json.get("status").getAsString();
        } else {
            value = json.toString();
        }
        log.info("%s", value);
    }

    private void throwDockerAccessException(JsonObject json) throws DockerAccessException {
        String msg = json.get("error").getAsString().trim();
        String details = json.getAsJsonObject("errorDetail").get("message").getAsString().trim();
        throw new DockerAccessException("%s %s", msg, (msg.equals(details) ? "" : "(" + details + ")"));
    }

    private String getStringOrEmpty(JsonObject json, String what) {
        return json.has(what) ? json.get(what).getAsString() : "";
    }

    @Override
    public void start() {
        log.progressStart();
    }

    @Override
    public void stop() {
        log.progressFinished();
    }
}
