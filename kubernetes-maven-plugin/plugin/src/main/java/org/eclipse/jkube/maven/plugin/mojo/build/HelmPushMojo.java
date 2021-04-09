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
package org.eclipse.jkube.maven.plugin.mojo.build;

import java.io.IOException;

import org.eclipse.jkube.kit.resource.helm.HelmRepository;
import org.eclipse.jkube.kit.resource.helm.HelmRepository.HelmRepoType;
import org.eclipse.jkube.kit.resource.helm.HelmService;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.settings.Server;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;

@Mojo(name = "helm-push", defaultPhase = LifecyclePhase.INSTALL, requiresDependencyResolution = ResolutionScope.COMPILE)
public class HelmPushMojo extends HelmMojo {

  protected static final String PROPERTY_UPLOAD_REPO_SNAPSHOT_NAME =  "jkube.helm.snapshotRepository.name";
  protected static final String PROPERTY_UPLOAD_REPO_SNAPSHOT_URL =  "jkube.helm.snapshotRepository.url";
  protected static final String PROPERTY_UPLOAD_REPO_SNAPSHOT_USERNAME =  "jkube.helm.snapshotRepository.username";
  protected static final String PROPERTY_UPLOAD_REPO_SNAPSHOT_PASSWORD =  "jkube.helm.snapshotRepository.password";
  protected static final String PROPERTY_UPLOAD_REPO_SNAPSHOT_TYPE =  "jkube.helm.snapshotRepository.type";
  protected static final String PROPERTY_UPLOAD_REPO_STABLE_NAME =  "jkube.helm.stableRepository.name";
  protected static final String PROPERTY_UPLOAD_REPO_STABLE_URL =  "jkube.helm.stableRepository.url";
  protected static final String PROPERTY_UPLOAD_REPO_STABLE_USERNAME =  "jkube.helm.stableRepository.username";
  protected static final String PROPERTY_UPLOAD_REPO_STABLE_PASSWORD =  "jkube.helm.stableRepository.password";
  protected static final String PROPERTY_UPLOAD_REPO_STABLE_TYPE =  "jkube.helm.stableRepository.type";
  protected static final String PROPERTY_SECURITY =  "jkube.helm.security";
  protected static final String DEFAULT_SECURITY = "~/.m2/settings-security.xml";

  @Component(role = org.sonatype.plexus.components.sec.dispatcher.SecDispatcher.class, hint = "default")
  private SecDispatcher securityDispatcher;

  @Override
  protected boolean canExecute() {
    return super.canExecute() && !skip;
  }

  @Override
  public void executeInternal() throws MojoExecutionException {
    if (skip) {
      return;
    }
    try {
      super.executeInternal();
      HelmRepository helmRepository = getHelmRepository();
      if (helmRepository != null) {
        this.setAuthentication(helmRepository);
        HelmService.uploadHelmChart(getKitLogger(), helm, helmRepository);
      } else {
        String error = "No repository configured for upload";
        getKitLogger().error(error);
        throw new MojoExecutionException(error);
      }
    } catch (Exception exp) {
      getKitLogger().error("Error performing helm push", exp);
      throw new MojoExecutionException(exp.getMessage());
    }
  }

  @Override
  protected void initDefaults() throws IOException, MojoExecutionException {
    super.initDefaults();

    if (!StringUtils.isBlank(getProperty(PROPERTY_UPLOAD_REPO_STABLE_NAME))) {
      try {
        HelmRepository stableRepository = new HelmRepository();
        stableRepository.setName(getProperty(PROPERTY_UPLOAD_REPO_STABLE_NAME));
        stableRepository.setUrl(getProperty(PROPERTY_UPLOAD_REPO_STABLE_URL));
        stableRepository.setUsername(getProperty(PROPERTY_UPLOAD_REPO_STABLE_USERNAME));
        stableRepository.setPassword(getProperty(PROPERTY_UPLOAD_REPO_STABLE_PASSWORD));
        stableRepository
            .setType(HelmRepoType.valueOf(getProperty(PROPERTY_UPLOAD_REPO_STABLE_TYPE)));
        getHelm().setStableRepository(stableRepository);
      } catch (Exception ex) {
        getKitLogger().debug("Error configuring Helm stable repository", ex);
        throw new MojoExecutionException("Error configuring Helm stable repository", ex);
      }
    }

    if (!StringUtils.isBlank(getProperty(PROPERTY_UPLOAD_REPO_SNAPSHOT_NAME))) {
      try {
        HelmRepository snapshotRepository = new HelmRepository();
        snapshotRepository.setName(getProperty(PROPERTY_UPLOAD_REPO_SNAPSHOT_NAME));
        snapshotRepository.setUrl(getProperty(PROPERTY_UPLOAD_REPO_SNAPSHOT_URL));
        snapshotRepository.setUsername(getProperty(PROPERTY_UPLOAD_REPO_SNAPSHOT_USERNAME));
        snapshotRepository.setPassword(getProperty(PROPERTY_UPLOAD_REPO_SNAPSHOT_PASSWORD));
        snapshotRepository
            .setType(HelmRepoType.valueOf(getProperty(PROPERTY_UPLOAD_REPO_SNAPSHOT_TYPE)));
        getHelm().setSnapshotRepository(snapshotRepository);
      } catch (Exception ex) {
        getKitLogger().debug("Error configuring Helm snapshot repository", ex);
        throw new MojoExecutionException("Error configuring Helm snapshot repository", ex);
      }
    }

    initFromPropertyOrDefault(PROPERTY_SECURITY, helm::getSecurity, helm::setSecurity, DEFAULT_SECURITY);

  }

  HelmRepository getHelmRepository() {
    if (getHelm().getVersion() != null && getHelm().getVersion().endsWith("-SNAPSHOT")) {
      return getHelm().getSnapshotRepository();
    }
    return getHelm().getStableRepository();
  }

  /**
   * Get credentials for given helm repo. If username is not provided the repo
   * name will be used to search for credentials in <code>settings.xml</code>.
   *
   * @param repository Helm repo with id and optional credentials.
   * @throws IllegalArgumentException Unable to get authentication because of misconfiguration.
   */
  void setAuthentication(HelmRepository repository)
      throws SecDispatcherException {
    String id = repository.getName();
    final String REPO = "Repo ";

    if (repository.getUsername() != null) {
      if (repository.getPassword() == null) {
        throw new IllegalArgumentException(REPO + id + " has a username but no password defined.");
      }
      getKitLogger().debug(REPO + id + " has credentials defined, skip searching in server list.");
    } else {

      Server server = getSettings().getServer(id);
      if (server == null) {
        getKitLogger().info(
            "No credentials found for " + id + " in configuration or settings.xml server list.");
      } else {

        getKitLogger().debug("Use credentials from server list for " + id + ".");
        if (server.getUsername() == null || server.getPassword() == null) {
          throw new IllegalArgumentException("Repo "
              + id
              + " was found in server list but has no username/password.");
        }
        repository.setUsername(server.getUsername());
        repository.setPassword(getSecDispatcher().decrypt(server.getPassword()));
      }
    }
  }

  protected SecDispatcher getSecDispatcher() {
    if (securityDispatcher instanceof DefaultSecDispatcher) {
      ((DefaultSecDispatcher) securityDispatcher).setConfigurationFile(getHelm().getSecurity());
    }
    return securityDispatcher;
  }
}
