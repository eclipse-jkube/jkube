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
package io.jkube.maven.enricher.api.visitor;

import java.util.List;

import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.jkube.kit.config.resource.ProcessorConfig;
import io.jkube.maven.enricher.api.Enricher;

public abstract class SelectorVisitor<T> extends TypedVisitor<T> {

    List<Enricher> enrichers;

    SelectorVisitor(List<Enricher> enrichers) {
        this.enrichers = enrichers;
    }

    private static ThreadLocal<ProcessorConfig> configHolder = new ThreadLocal<>();

    public static void setProcessorConfig(ProcessorConfig config) {
        configHolder.set(config);
    }

    public static void clearProcessorConfig() {
        configHolder.set(null);
    }

    protected static ProcessorConfig getConfig() {
        ProcessorConfig ret = configHolder.get();
        if (ret == null) {
            throw new IllegalArgumentException("Internal: No ProcessorConfig set");
        }
        return ret;
    }

    // ========================================================================

}

