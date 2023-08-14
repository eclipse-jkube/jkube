---
name: "Kit :: Helm"
description: |
  Eclipse JKube Kit example showing how to generate a Helm Charts using Eclipse JKube in standalone mode.
---
# JKube Kit - Docker Image Build  from Dynamic multilayered Dockerfile Example

This [quickstart](../../../quickstarts) will generate Helm charts using JKube Kit API.

It first creates a temporary directory containing the source manifests/templates that will be included in the chart.
These manifests include some that are generated programmatically using the Fabric8 Kubernetes client. And some that are
copied from the static directory.

Once the process completes, the new generated chart is available in the `target/helm` directory and as a tarball in the
`target` directory.

## How to run

Run the `mvn package` from your terminal.

Once the process completes, a success informative message is printed.

You can now install the generated chart using the `helm install jkube-example ./target/helm/kubernetes` command.


```shell script
$ mvn clean package
# ...
Creating Helm Chart "Demo Chart" for Kubernetes
Source directory: /home/user/00-MN/projects/forks/jkube/quickstarts/kit/helm/target/helm-sources
OutputDir: /home/user/00-MN/projects/forks/jkube/quickstarts/kit/helm/target/helm
Processing source files
Creating Chart.yaml
Copying additional files
Processing YAML templates
Creating Helm configuration Tarball: '/home/user/00-MN/projects/forks/jkube/quickstarts/kit/helm/target/Demo Chart-1.33.7-helm.tar.gz'
# ...
$ helm install jkube-example ./target/helm/kubernetes
# ...
NAME: jkube-example
NAMESPACE: default
STATUS: deployed
REVISION: 1
TEST SUITE: None
# ...
```
