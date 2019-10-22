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
package io.jkube.kit.config.service;

import io.jkube.kit.common.KitLogger;
import io.jkube.kit.common.util.ProcessUtil;

import java.io.File;

/**
 * A service that manages the client tools.
 * Try to avoid using this class, as support for client tools may be removed in the future.
 */
public class ClientToolsService {

    private KitLogger log;

    public ClientToolsService(KitLogger log) {
        this.log = log;
    }

    public File getKubeCtlExecutable(boolean preferOc) {
        if (preferOc) {
            File file = ProcessUtil.findExecutable(log, "oc");
            if (file != null) {
                return file;
            }
        }
        File file = ProcessUtil.findExecutable(log, "kubectl");
        if (file != null) {
            return file;
        }
        throw new IllegalStateException("Could not find " + (preferOc ? "oc or kubectl" : "kubectl") +
                                        ". Please install the necessary binaries and ensure they get added to your $PATH");
    }
}

