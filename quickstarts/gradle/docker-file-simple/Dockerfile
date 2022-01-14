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

FROM quay.io/jkube/jkube-java:0.0.13
ENV JAVA_APP_DIR=/deployments
EXPOSE 8080 8778 9779
COPY maven/build/libs/docker-file-simple-1.5.1.jar /deployments/
COPY maven/static-dir-in-project-root/my-file.txt /deployments/my-file.txt
