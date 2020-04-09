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
package org.eclipse.jkube.springboot.generator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.google.common.base.Strings;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.generator.api.GeneratorMode;
import org.eclipse.jkube.generator.javaexec.FatJarDetector;
import org.eclipse.jkube.generator.javaexec.JavaExecGenerator;
import org.eclipse.jkube.kit.build.service.docker.ImageConfiguration;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.Plugin;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;
import org.eclipse.jkube.kit.common.util.SpringBootConfigurationHelper;
import org.eclipse.jkube.kit.common.util.SpringBootUtil;
import org.apache.commons.io.FileUtils;

import static org.eclipse.jkube.kit.common.util.SpringBootConfigurationHelper.DEV_TOOLS_REMOTE_SECRET;
import static org.eclipse.jkube.springboot.generator.SpringBootGenerator.Config.color;

/**
 * @author roland
 * @since 15/05/16
 */
public class SpringBootGenerator extends JavaExecGenerator {

    private static final String DEFAULT_SERVER_PORT = "8080";

    public enum Config implements Configs.Key {
        color {{ d = ""; }};

        public String def() { return d; } protected String d;
    }

    public SpringBootGenerator(GeneratorContext context) {
        super(context, "spring-boot");
    }

    @Override
    public boolean isApplicable(List<ImageConfiguration> configs) {
        return shouldAddImageConfiguration(configs)
               && JKubeProjectUtil.hasPluginOfAnyArtifactId(getProject(), SpringBootConfigurationHelper.SPRING_BOOT_MAVEN_PLUGIN_ARTIFACT_ID);
    }

    @Override
    public List<ImageConfiguration> customize(List<ImageConfiguration> configs, boolean isPrePackagePhase) {
        if (getContext().getGeneratorMode() == GeneratorMode.WATCH) {
            ensureSpringDevToolSecretToken();
            if (!isPrePackagePhase ) {
                addDevToolsFilesToFatJar(configs);
            }
        }
        return super.customize(configs, isPrePackagePhase);
    }

    @Override
    protected Map<String, String> getEnv(boolean prePackagePhase) {
        Map<String, String> res = super.getEnv(prePackagePhase);
        if (getContext().getGeneratorMode() == GeneratorMode.WATCH) {
            // adding dev tools token to env variables to prevent override during recompile
            final String secret = SpringBootUtil.getSpringBootApplicationProperties(
                    SpringBootUtil.getSpringBootActiveProfile(getProject()),
                    JKubeProjectUtil.getClassLoader(getProject()))
                .getProperty(SpringBootConfigurationHelper.DEV_TOOLS_REMOTE_SECRET);
            if (secret != null) {
                res.put(SpringBootConfigurationHelper.DEV_TOOLS_REMOTE_SECRET_ENV, secret);
            }
        }
        return res;
    }

    @Override
    protected List<String> getExtraJavaOptions() {
        List<String> opts = super.getExtraJavaOptions();
        final String configuredColor = getConfig(color);
        if (configuredColor != null && !configuredColor.isEmpty()) {
            opts.add("-Dspring.output.ansi.enabled=" + configuredColor);
        }
        return opts;
    }

    @Override
    protected boolean isFatJar() {
        if (!hasMainClass() && isSpringBootRepackage()) {
            return true;
        }
        return super.isFatJar();
    }

    @Override
    protected List<String> extractPorts() {
        List<String> answer = new ArrayList<>();
        Properties properties = SpringBootUtil.getSpringBootApplicationProperties(
            SpringBootUtil.getSpringBootActiveProfile(getProject()),
            JKubeProjectUtil.getClassLoader(getProject()));
        SpringBootConfigurationHelper propertyHelper = new SpringBootConfigurationHelper(SpringBootUtil.getSpringBootVersion(getProject()));
        String port = properties.getProperty(propertyHelper.getServerPortPropertyKey(), DEFAULT_SERVER_PORT);
        addPortIfValid(answer, getConfig(JavaExecGenerator.Config.webPort, port));
        addPortIfValid(answer, getConfig(JavaExecGenerator.Config.jolokiaPort));
        addPortIfValid(answer, getConfig(JavaExecGenerator.Config.prometheusPort));
        return answer;
    }

    // =============================================================================

    private void ensureSpringDevToolSecretToken() {
        Properties properties = SpringBootUtil.getSpringBootApplicationProperties(
            SpringBootUtil.getSpringBootActiveProfile(getProject()),
            JKubeProjectUtil.getClassLoader(getProject()));
        String remoteSecret = properties.getProperty(DEV_TOOLS_REMOTE_SECRET);
        if (Strings.isNullOrEmpty(remoteSecret)) {
            addSecretTokenToApplicationProperties();
            throw new IllegalStateException("No spring.devtools.remote.secret found in application.properties. Plugin has added it, please re-run goals");
        }
    }

    private void addDevToolsFilesToFatJar(List<ImageConfiguration> configs) {
        if (isFatJar()) {
            File target = getFatJarFile();
            try {
                File devToolsFile = getSpringBootDevToolsJar();
                File applicationPropertiesFile = new File(getProject().getBaseDirectory(), "target/classes/application.properties");
                copyFilesToFatJar(Collections.singletonList(devToolsFile), Collections.singletonList(applicationPropertiesFile), target);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to add devtools files to fat jar " + target + ". " + e, e);
            }
        }
    }

    private File getFatJarFile() {
        FatJarDetector.Result fatJarDetectResult = detectFatJar();
        if (fatJarDetectResult == null) {
            throw new IllegalStateException("No fat jar built yet. Please ensure that the 'package' phase has run");
        }
        return fatJarDetectResult.getArchiveFile();
    }

    private void copyFilesToFatJar(List<File> libs, List<File> classes, File target) throws IOException {
        File tmpZip = File.createTempFile(target.getName(), null);
        tmpZip.delete();

        // Using Apache commons rename, because renameTo has issues across file systems
        FileUtils.moveFile(target, tmpZip);

        byte[] buffer = new byte[8192];
        try (ZipInputStream zin = new ZipInputStream(new FileInputStream(tmpZip));
             ZipOutputStream out = new ZipOutputStream(new FileOutputStream(target))) {
            for (ZipEntry ze = zin.getNextEntry(); ze != null; ze = zin.getNextEntry()) {
                if (matchesFatJarEntry(libs, ze.getName(), true) || matchesFatJarEntry(classes, ze.getName(), false)) {
                    continue;
                }
                out.putNextEntry(ze);
                for(int read = zin.read(buffer); read > -1; read = zin.read(buffer)){
                    out.write(buffer, 0, read);
                }
                out.closeEntry();
            }

            for (File lib : libs) {
                try (InputStream in = new FileInputStream(lib)) {
                    out.putNextEntry(createZipEntry(lib, getFatJarFullPath(lib, true)));
                    for (int read = in.read(buffer); read > -1; read = in.read(buffer)) {
                        out.write(buffer, 0, read);
                    }
                    out.closeEntry();
                }
            }

            for (File cls : classes) {
                try (InputStream in = new FileInputStream(cls)) {
                    out.putNextEntry(createZipEntry(cls, getFatJarFullPath(cls, false)));
                    for (int read = in.read(buffer); read > -1; read = in.read(buffer)) {
                        out.write(buffer, 0, read);
                    }
                    out.closeEntry();
                }
            }
        }
        tmpZip.delete();
    }

    private boolean matchesFatJarEntry(List<File> fatJarEntries, String path, boolean lib) {
        for (File e : fatJarEntries) {
            String fullPath = getFatJarFullPath(e, lib);
            if (fullPath.equals(path)) {
                return true;
            }
        }
        return false;
    }

    private String getFatJarFullPath(File file, boolean lib) {
        if (lib) {
            return "BOOT-INF/lib/" + file.getName();
        }
        return "BOOT-INF/classes/" + file.getName();
    }

    private ZipEntry createZipEntry(File file, String fullPath) throws IOException {
        ZipEntry entry = new ZipEntry(fullPath);

        byte[] buffer = new byte[8192];
        int bytesRead = -1;
        try (InputStream is = new FileInputStream(file)) {
            CRC32 crc = new CRC32();
            int size = 0;
            while ((bytesRead = is.read(buffer)) != -1) {
                crc.update(buffer, 0, bytesRead);
                size += bytesRead;
            }
            entry.setSize(size);
            entry.setCompressedSize(size);
            entry.setCrc(crc.getValue());
            entry.setMethod(ZipEntry.STORED);
            return entry;
        }
    }

    private void addSecretTokenToApplicationProperties() {
        String newToken = UUID.randomUUID().toString();
        log.verbose("Generating the spring devtools token in property: " + DEV_TOOLS_REMOTE_SECRET);

        // We always add to application.properties, even when an application.yml exists, since both
        // files are evaluated by Spring Boot.
        appendSecretTokenToFile("target/classes/application.properties", newToken);
        appendSecretTokenToFile("src/main/resources/application.properties", newToken);
    }

    private void appendSecretTokenToFile(String path, String token) throws IllegalStateException {
        File file = new File(getProject().getBaseDirectory(), path);
        file.getParentFile().mkdirs();
        String text = String.format("%s" +
                        "# Remote secret added by jkube-kit-plugin\n" +
                        "%s=%s\n",
                file.exists() ? "\n" : "", DEV_TOOLS_REMOTE_SECRET, token);

        try (FileWriter writer = new FileWriter(file, true)) {
            writer.append(text);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to append to file: " + file + ". " + e, e);
        }
    }

    private boolean isSpringBootRepackage() {
        JavaProject project = getProject();
        Plugin plugin = JKubeProjectUtil.getPlugin(project, SpringBootConfigurationHelper.SPRING_BOOT_MAVEN_PLUGIN_ARTIFACT_ID);
        if (plugin != null) {
            List<String> executions = plugin.getExecutions();
            if (executions != null) {
                if (executions.contains("repackage")) {
                    log.verbose("Using fat jar packaging as the spring boot plugin is using `repackage` goal execution");
                    return true;
                }
            }
        }
        return false;
    }

    private File getSpringBootDevToolsJar() throws IOException {
        String version = SpringBootUtil.getSpringBootDevToolsVersion(getProject()).orElseThrow(() -> new IllegalStateException("Unable to find the spring-boot version"));
        return getContext().getArtifactResolver().resolveArtifact(SpringBootConfigurationHelper.SPRING_BOOT_GROUP_ID, SpringBootConfigurationHelper.SPRING_BOOT_DEVTOOLS_ARTIFACT_ID, version, "jar");
    }

}
