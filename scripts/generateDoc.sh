#!/bin/bash
#
# Copyright (c) 2019 Red Hat, Inc.
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at:
#
#     https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Contributors:
#   Red Hat, Inc. - initial API and implementation
#

trap 'exit' ERR

mkdir docs-generated
# Kubernetes Gradle Plugin
mvn -f gradle-plugin/doc -Phtml package
cp gradle-plugin/doc/target/generated-docs/index.html docs-generated/kubernetes-gradle-plugin.html
# Kubernetes Maven Plugin
mvn -f kubernetes-maven-plugin/doc -Phtml package
cp kubernetes-maven-plugin/doc/target/generated-docs/index.html docs-generated/kubernetes-maven-plugin.html
# OpenShift Gradle Plugin
mvn -f gradle-plugin/doc -Phtml package -Dplugin=openshift-gradle-plugin -Dtask-prefix=oc -Dcluster=OpenShift -DpluginExtension=openshift
cp gradle-plugin/doc/target/generated-docs/index.html docs-generated/openshift-gradle-plugin.html
# OpenShift Maven Plugin
mvn -f kubernetes-maven-plugin/doc -Phtml package -Dplugin=openshift-maven-plugin -Dgoal-prefix=oc -Dcluster=OpenShift
cp kubernetes-maven-plugin/doc/target/generated-docs/index.html docs-generated/openshift-maven-plugin.html

cat << EOF >> docs-generated/index.html
<html>
  <head>
    <meta name="robots" content="noindex" />
  </head>
  <body>
    <ul>
        <li><a href="./kubernetes-maven-plugin.html">Kubernetes Maven Plugin</a></li>
        <li><a href="./openshift-maven-plugin.html">OpenShift Maven Plugin</a></li>
        <li><a href="./kubernetes-gradle-plugin.html">Kubernetes Gradle Plugin</a></li>
        <li><a href="./openshift-gradle-plugin.html">OpenShift Gradle Plugin</a></li>
    </ul>
  </body>
</html>
EOF

cat << EOF >> docs-generated/robots.txt
User-agent: *
Disallow: /

EOF
