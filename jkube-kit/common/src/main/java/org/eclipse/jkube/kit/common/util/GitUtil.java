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
package org.eclipse.jkube.kit.common.util;

import java.io.File;
import java.io.IOException;
import java.util.stream.StreamSupport;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

/**
 * @author roland
 * @since 28/07/16
 */
public class GitUtil {

    private GitUtil() { }

    public static Repository getGitRepository(File currentDir) throws IOException {

        if (currentDir == null) {
            // TODO: Why is this check needed ?
            currentDir = new File(System.getProperty("basedir", "."));
        }
        File gitFolder = findGitFolder(currentDir);
        if (gitFolder == null) {
            // No git repository found
            return null;
        }
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        return builder
                .readEnvironment()
                .setGitDir(gitFolder)
                .build();
    }

    public static File findGitFolder(File basedir) {
        File gitDir = new File(basedir, ".git");
        if (gitDir.exists() && gitDir.isDirectory()) {
            return gitDir;
        }
        File parent = basedir.getParentFile();
        if (parent != null) {
            return findGitFolder(parent);
        }
        return null;
    }

    public static String getGitCommitId(Repository repository) throws GitAPIException {
        if (repository != null) {
            return StreamSupport.stream(new Git(repository).log().call().spliterator(), false)
                .map(RevCommit::getName)
                .findFirst().orElse(null);
        }
        return null;
    }
}
