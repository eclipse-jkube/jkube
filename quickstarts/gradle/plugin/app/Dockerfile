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

ENV JAVA_APP_DIR=/deployments

WORKDIR ${JAVA_APP_DIR}

COPY maven/build/libs/*.jar app.jar
COPY maven/build/classes/java/main/jkube-extra/* files/

EXPOSE 8080

