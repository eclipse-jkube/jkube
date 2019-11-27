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
package org.eclipse.jkube.kit.build.service.docker.access.chunked;

import com.google.gson.JsonObject;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccessException;
import org.eclipse.jkube.kit.common.KitLogger;

public class BuildJsonResponseHandler implements EntityStreamReaderUtil.JsonEntityResponseHandler {

    private final KitLogger log;

    public BuildJsonResponseHandler(KitLogger log) {
        this.log = log;
    }

    @Override
    public void process(JsonObject json) throws DockerAccessException {
        if (json.has("error")) {
            String msg = json.get("error").getAsString();
            String detailMsg = "";
            if (json.has("errorDetail")) {
                JsonObject details = json.getAsJsonObject("errorDetail");
                detailMsg = details.get("message").getAsString();
            }
            throw new DockerAccessException("%s %s", json.get("error"),
                    (msg.equals(detailMsg) || "".equals(detailMsg) ? "" : "(" + detailMsg + ")"));
        } else if (json.has("stream")) {
            String message = json.get("stream").getAsString();
            log.verbose("%s", message.trim());
        } else if (json.has("status")) {
            String status = json.get("status").getAsString().trim();
            String id = json.has("id") ? json.get("id").getAsString() : null;
            if (status.matches("^.*(Download|Pulling).*")) {
                log.info("  %s%s",id != null ? id + " " : "",status);
            }
        }
    }

    // Lifecycle methods not needed ...
    @Override
    public void start() {}

    @Override
    public void stop() {}
}
