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
package org.eclipse.jkube.kit.remotedev;

import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.config.keys.DefaultAuthorizedKeysAuthenticator;
import org.apache.sshd.server.forward.AcceptAllForwardingFilter;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

public class SshServerExtension implements BeforeTestExecutionCallback, AfterEachCallback {

  static final String AUTHORIZED_USER = "authorized-user";

  private final Supplier<RemoteDevelopmentContext> remoteDevelopmentContextSupplier;

  protected Path sshPath;
  @Getter
  protected SshServer sshServer;

  public SshServerExtension(Supplier<RemoteDevelopmentContext> remoteDevelopmentContextSupplier) {
    this.remoteDevelopmentContextSupplier = remoteDevelopmentContextSupplier;
  }

  @Override
  public void beforeTestExecution(ExtensionContext context) throws Exception {
    final RemoteDevelopmentContext remoteDevelopmentContext = remoteDevelopmentContextSupplier.get();
    sshPath = Files.createTempDirectory("jkube-ssh");
    final Path authorizedKeys = Files.createFile(sshPath.resolve("authorized_keys"));
    Files.write(authorizedKeys, remoteDevelopmentContext.getSshRsaPublicKey().getBytes(StandardCharsets.UTF_8));
    sshServer = SshServer.setUpDefaultServer();
    sshServer.setPort(remoteDevelopmentContext.getSshPort());
    sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
    sshServer.setPublickeyAuthenticator(new DefaultAuthorizedKeysAuthenticator(AUTHORIZED_USER, authorizedKeys,false));
    sshServer.setForwardingFilter(AcceptAllForwardingFilter.INSTANCE);
    sshServer.start();
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    sshServer.stop(true);
    FileUtils.deleteDirectory(sshPath.toFile());
  }
}
