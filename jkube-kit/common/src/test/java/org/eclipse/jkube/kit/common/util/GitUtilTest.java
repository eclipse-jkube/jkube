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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.URIish;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class GitUtilTest {
  @TempDir
  private File temporaryFolder;

  @Test
  void getGitRepository_whenNoGitFolderFound_thenReturnNull() throws IOException {
      assertThat(GitUtil.getGitRepository(temporaryFolder)).isNull();
  }

  @Test
  void getGitRepository_whenValidGitRepositoryPresent_thenReturnGitRepository() throws IOException, URISyntaxException, GitAPIException {
    try (Git ignore = createDummyGitRepository(temporaryFolder)) {
      // Given
      // When
      Repository repository = GitUtil.getGitRepository(temporaryFolder);

      // Then
      assertThat(repository)
          .isNotNull()
          .satisfies(r -> assertThat(r.getBranch()).isEqualTo("test-branch"))
          .satisfies(r -> assertThat(r.getDirectory()).isEqualTo(temporaryFolder.toPath().resolve(".git").toFile()));
    }
  }

  @Test
  void getGitCommitId_whenValidRepository_thenReturnValidCommit() throws IOException, GitAPIException, URISyntaxException {
    try (Git ignore = createDummyGitRepository(temporaryFolder)) {
      // Given
      Repository repository = GitUtil.getGitRepository(temporaryFolder);

      // When + Then
      assertThat(GitUtil.getGitCommitId(repository)).isNotBlank().hasSize(40);
    }
  }

  @Test
  void getCommitId_whenNullRepository_thenReturnNull() throws GitAPIException {
    assertThat(GitUtil.getGitCommitId(null)).isNull();
  }

  @Test
  void findGitFolder_whenGitFolderAbsent_thenReturnNull() {
    assertThat(GitUtil.findGitFolder(temporaryFolder)).isNull();
  }

  @Test
  void findGitFolder_whenGitFolderInSameDirectory_thenReturnFolder() throws GitAPIException, URISyntaxException {
    try (Git ignore = createDummyGitRepository(temporaryFolder)) {
      assertThat(GitUtil.findGitFolder(temporaryFolder)).isEqualTo(new File(temporaryFolder, ".git"));
    }
  }

  @Test
  void findGitFolder_whenGitFolderInParentDirectory_thenReturnFolder() throws GitAPIException, URISyntaxException, IOException {
    File childFolder = temporaryFolder.toPath().resolve("subfolder").toFile();
    Files.createDirectory(childFolder.toPath());
    try (Git ignore = createDummyGitRepository(temporaryFolder)) {
      assertThat(GitUtil.findGitFolder(childFolder)).isEqualTo(new File(temporaryFolder, ".git"));
    }
  }

  // These test cases are taken from https://stackoverflow.com/questions/31801271/what-are-the-supported-git-url-formats
  @ParameterizedTest(name = "{0} should be sanitized to {1}")
  @CsvSource({
      ",",
      "https://user:password@example.com/repo.git?queryParam,https://example.com/repo.git?queryParam",
      "git://user:password@example.com/repo.git?queryParam, git://example.com/repo.git?queryParam",
      "user@example.com:repo.git, example.com:repo.git",
      "user:password@example.com:repo.git, example.com:repo.git",
    "git://git.kernel.org/pub/scm/linux/kernel/git/gregkh/staging.git, git://git.kernel.org/pub/scm/linux/kernel/git/gregkh/staging.git",
      "https://@some.host/foo/bar, https://@some.host/foo/bar",
      "https://secrettoken:x-oauth-basic@host.xz/path/to/repo.git, https://host.xz/path/to/repo.git",
      "https://gitlab-ci-token:secrettoken@gitlab.onprem.com/project.git, https://gitlab.onprem.com/project.git",
      "git@github.com:myorg/myproject.git, github.com:myorg/myproject.git",
      "https://github.com/myorg/myproject.git, https://github.com/myorg/myproject.git",
      "https://gitlab.com/foo/bar.git, https://gitlab.com/foo/bar.git",
      "git@gitlab.com:foo/bar.git, gitlab.com:foo/bar.git",
      "git+ssh://git@gitlab.com/foo/bar.git, git+ssh://gitlab.com/foo/bar.git",
      "git+ssh://git@gitlab.com/foo/bar, git+ssh://gitlab.com/foo/bar",
      "git://host.xz/~user/path/to/repo.git, git://host.xz/~user/path/to/repo.git",
      "rsync://host.xz/path/to/repo.git,rsync://host.xz/path/to/repo.git",
      "ssh://user@host.xz:8080/path/to/repo.git, ssh://host.xz:8080/path/to/repo.git",
      "ssh://user@host.xz/path/to/repo.git, ssh://host.xz/path/to/repo.git",
      "ssh://host.xz:8080/path/to/repo.git, ssh://host.xz:8080/path/to/repo.git",
      "ssh://host.xz/path/to/repo.git, ssh://host.xz/path/to/repo.git",
      "ssh://user@host.xz/~user/path/to/repo.git, ssh://host.xz/~user/path/to/repo.git",
      "ssh://host.xz/~user/path/to/repo.git, ssh://host.xz/~user/path/to/repo.git",
      "ssh://user@host.xz/~/path/to/repo.git, ssh://host.xz/~/path/to/repo.git",
      "ssh://host.xz/~/path/to/repo.git, ssh://host.xz/~/path/to/repo.git",
      "file:///path/to/repo.git, file:///path/to/repo.git",
      "file://~/path/to/repo.git, file://~/path/to/repo.git",
      "host.xz:path/to/repo.git, host.xz:path/to/repo.git",
      "user@host.xz:path/to/repo.git, host.xz:path/to/repo.git",
      "user@host.xz:~user/path/to/repo.git, host.xz:~user/path/to/repo.git",
      "host.xz:~user/path/to/repo.git, host.xz:~user/path/to/repo.git",
      "https://[::1]:456, https://[::1]:456"
  })
  void sanitizeRemoteUrl_whenInvoked_shouldSanitize(String original, String expected) {
    assertThat(GitUtil.sanitizeRemoteUrl(original)).isEqualTo(expected);
  }

  private Git createDummyGitRepository(File gitFolder) throws GitAPIException, URISyntaxException {
    Git git = Git.init().setDirectory(gitFolder).setInitialBranch("test-branch").call();
    git.add().addFilepattern(".").call();
    git.remoteAdd().setName("origin").setUri(new URIish("https://example.com/origin.git")).call();
    git.commit().setMessage("Initial commit").call();
    return git;
  }
}
