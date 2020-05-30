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
package org.eclipse.jkube.kit.common.service;

import mockit.Mocked;
import org.apache.commons.io.FileUtils;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.XMLUtil;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MigrateServiceTest {
    @Mocked
    KitLogger logger;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testPomPluginMigrationInBuild() throws Exception {
        // Given
        File projectPom = new File(getClass().getResource("/test-project/pom.xml").toURI());
        File pomFile = folder.newFile("pom.xml");
        FileUtils.copyFile(projectPom, pomFile);
        MigrateService migrateService = new MigrateService(folder.getRoot(), logger);

        // When
        migrateService.migrate("org.eclipse.jkube", "kubernetes-maven-plugin", "1.0.0-SNAPSHOT");

        // Then
        Document document = XMLUtil.readXML(pomFile);
        assertTrue(pomFile.exists());
        assertEquals("org.eclipse.jkube", XMLUtil.getNodeValueFromDocument(document, "/project/build/plugins/plugin[2]/groupId"));
        assertEquals("kubernetes-maven-plugin", XMLUtil.getNodeValueFromDocument(document, "/project/build/plugins/plugin[2]/artifactId"));
        assertEquals("1.0.0-SNAPSHOT", XMLUtil.getNodeValueFromDocument(document, "/project/build/plugins/plugin[2]/version"));
    }


    @Test
    public void testPomPluginMigrationInProfile() throws Exception {
        // Given
        File projectPom = new File(getClass().getResource("/test-project-profile/pom.xml").toURI());
        File pomFile = folder.newFile("pom.xml");
        FileUtils.copyFile(projectPom, pomFile);
        MigrateService migrateService = new MigrateService(folder.getRoot(), logger);

        // When
        migrateService.migrate("org.eclipse.jkube", "kubernetes-maven-plugin", "1.0.0-SNAPSHOT");

        // Then
        Document document = XMLUtil.readXML(pomFile);
        assertTrue(pomFile.exists());
        assertEquals("org.eclipse.jkube", XMLUtil.getNodeValueFromDocument(document, "/project/profiles/profile[1]/build/plugins/plugin[1]/groupId"));
        assertEquals("kubernetes-maven-plugin", XMLUtil.getNodeValueFromDocument(document, "/project/profiles/profile[1]/build/plugins/plugin[1]/artifactId"));
        assertEquals("1.0.0-SNAPSHOT", XMLUtil.getNodeValueFromDocument(document, "/project/profiles/profile[1]/build/plugins/plugin[1]/version"));
    }

    @Test
    public void testProjectResourceFragmentDirectoryRename() throws Exception {
        // Given
        File projectPom = new File(getClass().getResource("/test-project-profile/pom.xml").toURI());
        File pomFile = folder.newFile("pom.xml");
        folder.newFolder("src", "main", "fabric8");
        FileUtils.copyFile(projectPom, pomFile);
        MigrateService migrateService = new MigrateService(folder.getRoot(), logger);

        // When
        migrateService.migrate("org.eclipse.jkube", "kubernetes-maven-plugin", "1.0.0-SNAPSHOT");

        // Then
        Document document = XMLUtil.readXML(pomFile);
        assertTrue(pomFile.exists());
        assertTrue(new File(folder.getRoot(), "src/main/jkube").exists());
        assertEquals("org.eclipse.jkube", XMLUtil.getNodeValueFromDocument(document, "/project/profiles/profile[1]/build/plugins/plugin[1]/groupId"));
        assertEquals("kubernetes-maven-plugin", XMLUtil.getNodeValueFromDocument(document, "/project/profiles/profile[1]/build/plugins/plugin[1]/artifactId"));
        assertEquals("1.0.0-SNAPSHOT", XMLUtil.getNodeValueFromDocument(document, "/project/profiles/profile[1]/build/plugins/plugin[1]/version"));
    }
}
