/**
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
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

