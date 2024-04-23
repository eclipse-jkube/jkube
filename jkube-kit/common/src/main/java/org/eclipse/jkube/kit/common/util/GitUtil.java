/*
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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.URIish;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.stream.StreamSupport;

/**
 * @author roland
 * @since 28/07/16
 */
public class GitUtil {

    private GitUtil() { }

    public static Repository getGitRepository(File currentDir) throws IOException {

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

    /**
     * Sanitize Git Repository's remote URL, trims username and access token from URL.
     *
     * @param remoteUrlStr URL string of a particular git remote
     * @return sanitized URL
     */
    public static String sanitizeRemoteUrl(String remoteUrlStr) {
        if (StringUtils.isBlank(remoteUrlStr)) {
            return remoteUrlStr;
        }
        try {
            URIish uri = new URIish(remoteUrlStr);
            final StringBuilder userInfo = new StringBuilder();
            if (StringUtils.isNotBlank(uri.getUser())) {
                userInfo.append(uri.getUser());
            }
            if (StringUtils.isNotBlank(uri.getPass())) {
                userInfo.append(":").append(uri.getPass());
            }
            if (userInfo.length() > 0) {
                remoteUrlStr = remoteUrlStr.replace(userInfo + "@", "");
            }
            return remoteUrlStr;
        } catch (URISyntaxException e) {
            //NO OP - Not a valid URL
        }
        return remoteUrlStr;
    }
}
