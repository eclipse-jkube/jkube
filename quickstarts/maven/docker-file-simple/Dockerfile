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

FROM openjdk:latest
COPY maven/target/docker-file-simple.jar /deployments/docker-file-simple.jar
# Copying a file inside project root directory
COPY maven/static-dir-in-project-root/my-file.txt /deployments/my-file.txt
CMD ["java", "-jar", "/deployments/docker-file-simple.jar"]
