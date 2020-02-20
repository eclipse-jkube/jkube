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
package org.eclipse.jkube.kit.config.resource;

import java.util.List;

/**
 * Configuration for a helm chart
 * @author roland
 * @since 11/08/16
 */
public class HelmConfig {

    private String chart;

    private String outputDir;

    private String sourceDir;

    private List<String> keywords;

    private String engine;

    private List<HelmType> type;

    private String chartExtension;

    public String getChart() {
        return chart;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public String getSourceDir() {
        return sourceDir;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public String getEngine() {
        return engine;
    }

    public List<HelmType> getType() {
        return type;
    }

    public String getChartExtension() {
        return chartExtension;
    }

    public enum HelmType {
        kubernetes("helm", "k8s-template", "Kubernetes"),
        openshift("helmshift", "openshift", "OpenShift");

        private final String classifier;
        private final String sourceDir;
        private final String description;
        private final String outputDir;

        HelmType(String classifier, String sourceDir, String description) {
            this.classifier = classifier;
            this.sourceDir = sourceDir;
            this.description = description;
            this.outputDir = description.toLowerCase();
        }

        public String getClassifier() {
            return classifier;
        }

        public String getSourceDir() {
            return sourceDir;
        }

        public String getOutputDir() {
            return outputDir;
        }

        public String getDescription() {
            return description;
        }
    }
}
