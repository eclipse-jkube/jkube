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
package org.eclipse.jkube.enricher.generic;

import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.BaseEnricher;
import org.eclipse.jkube.kit.enricher.api.EnricherContext;

public class SecurityHardeningEnricher extends BaseEnricher {

  public SecurityHardeningEnricher(EnricherContext enricherContext) {
    super(enricherContext, "jkube-security-hardening");
  }

  @Override
  public void enrich(PlatformMode platformMode, KubernetesListBuilder builder) {
    // TODO: (maybe) make default security values configurable -a user might want to exclude some-
    builder.accept(
      new PodSpecBuilderSecurityHardeningVisitor(),
      new ContainerSecurityHardeningVisitor(),
      new ContainerSecurityWarningVisitor(getContext())
    );
  }

  private static final class PodSpecBuilderSecurityHardeningVisitor extends TypedVisitor<PodSpecBuilder> {

    @Override
    public void visit(PodSpecBuilder podSpec) {
      podSpec
        // Ensure service account tokens are mounted where necessary
        // https://docs.bridgecrew.io/docs/bc_k8s_35
        .withAutomountServiceAccountToken(false);
        // --------------------
    }
  }

  private static final class ContainerSecurityHardeningVisitor extends TypedVisitor<ContainerBuilder> {

    @Override
    public void visit(ContainerBuilder container) {
      container
        .editOrNewSecurityContext()
        // Ensure container is not privileged
        // https://docs.bridgecrew.io/docs/bc_k8s_15
        .withPrivileged(false)
        // --------------------
        // Ensure containers do not run with AllowPrivilegeEscalation
        // https://docs.bridgecrew.io/docs/bc_k8s_19
        .withAllowPrivilegeEscalation(false)
        // --------------------
        // Use Read-Only filesystem for containers where possible
        // https://docs.bridgecrew.io/docs/bc_k8s_21
        // TODO: Issue for Tomcat and other WebApplications
        // Incompatible with Jolokia and other scripts in our base image
        // .withReadOnlyRootFilesystem(true)
        // --------------------
        // Containers should run as a high UID to avoid host conflict
        // https://docs.bridgecrew.io/docs/bc_k8s_37
        .withRunAsUser(10000L)
        .withRunAsNonRoot(true)
        // --------------------
        // Ensure seccomp is set to Docker/Default or Runtime/Default
        // https://docs.bridgecrew.io/docs/bc_k8s_29
        .editOrNewSeccompProfile()
        .withType("RuntimeDefault")
        .endSeccompProfile()
        // --------------------
        // Minimize the admission of containers with the NET_RAW capability
        // https://docs.bridgecrew.io/docs/bc_k8s_27
        // https://docs.bridgecrew.io/docs/bc_k8s_34
        .editOrNewCapabilities()
        .addToDrop("NET_RAW")
        .addToDrop("ALL")
        .endCapabilities()
        // --------------------
        .endSecurityContext();
    }
  }

  private static final class ContainerSecurityWarningVisitor extends TypedVisitor<ContainerBuilder> {

    private final EnricherContext enricherContext;

    public ContainerSecurityWarningVisitor(EnricherContext enricherContext) {
      this.enricherContext = enricherContext;
    }

    @Override
    public void visit(ContainerBuilder containerBuilder) {
      final Container container = containerBuilder.build();
      // Ensure image tag is set to Fixed - not Latest or Blank
      // https://docs.bridgecrew.io/docs/bc_k8s_13
      if (!enricherContext.getProject().isSnapshot()
        && container.getImage() != null && container.getImage().endsWith(":latest")) {
        enricherContext.getLog().warn(
          "Container %s has an image with tag 'latest', it's recommended to use a fixed tag or a digest instead",
          container.getName());
      }
    }
  }
}
