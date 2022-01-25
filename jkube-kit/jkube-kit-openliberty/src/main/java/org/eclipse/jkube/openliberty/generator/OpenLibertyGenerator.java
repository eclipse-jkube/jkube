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
package org.eclipse.jkube.openliberty.generator;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.generator.javaexec.FatJarDetector;
import org.eclipse.jkube.generator.javaexec.JavaExecGenerator;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.common.AssemblyFileSet;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OpenLibertyGenerator extends JavaExecGenerator {

    protected static final String LIBERTY_SELF_EXTRACTOR = "wlp.lib.extract.SelfExtractRun";
    protected static final String LIBERTY_RUNNABLE_JAR = "LIBERTY_RUNNABLE_JAR";
    protected static final String JAVA_APP_JAR = "JAVA_APP_JAR";

    public OpenLibertyGenerator(GeneratorContext context) {
        super(context, "openliberty");
    }

    // Override so that the generator kicks in when the liberty-maven-plugin is used
    @Override
    public boolean isApplicable(List<ImageConfiguration> configs) {
        return shouldAddGeneratedImageConfiguration(configs)
                && (
                    JKubeProjectUtil.hasPlugin(getProject(), "io.openliberty.tools", "liberty-maven-plugin") ||
                    JKubeProjectUtil.hasPlugin(getProject(), "io.openliberty.tools.gradle.Liberty", "io.openliberty.tools.gradle.Liberty.gradle.plugin") ||
                    JKubeProjectUtil.hasGradlePlugin(getProject(), "io.openliberty.tools.gradle.Liberty")
                );

    }

    // Override extractPorts so that we default to 9080 rather than 8080 for the web port.
    @Override
    protected List<String> extractPorts() {
        List<String> ret = new ArrayList<>();
        addPortIfValid(ret, getConfig(JavaExecGenerator.Config.WEB_PORT, "9080"));
        addPortIfValid(ret, getConfig(JavaExecGenerator.Config.JOLOKIA_PORT));
        addPortIfValid(ret, getConfig(JavaExecGenerator.Config.PROMETHEUS_PORT));
        return ret;
    }

    @Override
    protected Map<String, String> getEnv(boolean prePackagePhase) {
        Map<String, String> ret = super.getEnv(prePackagePhase);
        final FatJarDetector.Result fatJar = detectFatJar();
        if (fatJar != null && LIBERTY_SELF_EXTRACTOR.equals(fatJar.getMainClass())) {
            ret.put(LIBERTY_RUNNABLE_JAR, fatJar.getArchiveFile().getName());
            ret.put(JAVA_APP_JAR, fatJar.getArchiveFile().getName());
        }
        return ret;
    }

    @Override
    public List<AssemblyFileSet> addAdditionalFiles() {
        List<AssemblyFileSet> fileSets = new ArrayList<>();
        fileSets.add(createFileSet("src/main/jkube-includes/bin","bin", "0755"));
        fileSets.add(createFileSet("src/main/jkube-includes",".", "0644"));
        // Add server.xml file
        fileSets.add(createFileSet("src/main/liberty/config","src/wlp/config", "0644"));
        return fileSets;
    }

}
