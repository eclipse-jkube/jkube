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
package org.eclipse.jkube.maven.sample.spring.boot.dekorate;

import io.dekorate.docker.annotation.DockerBuild;
import io.dekorate.kubernetes.annotation.KubernetesApplication;
import io.dekorate.kubernetes.annotation.Label;
import io.dekorate.kubernetes.annotation.ServiceType;
import io.dekorate.openshift.annotation.OpenshiftApplication;
import io.dekorate.s2i.annotation.S2iBuild;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OpenshiftApplication(
        labels = @Label(key = "decorated-by", value = "dekorate"),
        group = "jkube",
        serviceType = ServiceType.NodePort
)
@KubernetesApplication(
        labels = @Label(key = "decorated-by", value = "dekorate"),
        group = "jkube",
        serviceType = ServiceType.NodePort
)
@DockerBuild(
        group = "jkube",
        version = "latest",
        name = "jkube-maven-sample-spring-boot-dekorate"
)
@S2iBuild(
        group = "jkube",
        version = "latest",
        name = "jkube-maven-sample-spring-boot-dekorate"
)
@SpringBootApplication
public class SpringDekorateApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringDekorateApplication.class, args);
    }

}
