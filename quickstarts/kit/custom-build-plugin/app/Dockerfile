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

FROM quay.io/jkube/jkube-java:0.0.15

ADD maven/target/${projectFinalName}.jar /opt/hello-world.jar
ADD maven/target/classes/jkube-extra/foo-java/foo-java.sh /opt

# See foo-build-plugin module for more information about foo-java.sh
CMD JAVA_MAIN_CLASS=org.eclipse.jkube.quickstart.foojava.HelloWorld JAVA_APP_DIR=/opt sh /opt/foo-java.sh

