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

# create name space usernamespace
kubectl create namespace usernamespace
# for installing zipkin use below command
kubectl -n usernamespace create -f zipkin.yaml
kubectl create -f zipkin.yaml
kubectl -n usernamespace create -f appconfig.yaml
kubectl create -f appconfig.yaml
# for zipkin uninstallation
kubectl -n usernamespace delete -f zipkin.yaml
kubectl delete -f zipkin.yaml
kubectl -n usernamespace delete -f appconfig.yaml
kubectl delete -f appconfig.yaml
# delete namespace
kubectl delete namespace usernamespace
