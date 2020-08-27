# JKube externally provided Dockerfile example
This quick start showcases how to use Eclipse JKube with external Docker files.

External Docker file are configured using `contextDir` and `dockerFile` options.

If only `dockerFile` option is specified, the file is searched in a relative path
under `src/main/docker`. If set in combination with `contextDir` the Docker file
will be searched relatively to that context directory.

If only `contextDir` option is specified, a file with name `Dockerfile` is searched 
in the specified context directory.

You can provide external Docker files with the following approaches:

## `configuration.images.image.build.contextDir`
Retrieves the `Dockerfile` from the provided `<contextDir>` configuration.

Showcased in the configuration for the `context-dir` Maven profile. Will use Dockerfile
located in path `src/main/docker-context-dir/Dockerfile`.

```shell script
$ mvn clean package k8s:build -P'context-dir'
...
$ docker image inspect jkube/docker-file-provided | jq ".[].ContainerConfig.Labels" | grep location 
    "location": "src/main/docker-context-dir",
```

## `configuration.images.image.build.dockerFile`
Retrieves the `Dockerfile` from the provided `<dockerFile>` configuration.
`<dockerFile>` expects a relative location of a file that is located under `src/main/docker`.
Showcased in the configuration for the `docker-file` Maven profile. Will use Dockerfile
located in path `src/main/docker/subdir/Dockerfile`.

```shell script
$ mvn clean package k8s:build -P'docker-file'
...
$ docker image inspect jkube/docker-file-provided | jq ".[].ContainerConfig.Labels" | grep location 
  "location": "src/main/docker/subdir",
```

## `contextDir` and `dockerFile`
Retrieves the `Dockerfile` from the provided `<dockerFile>` relative path configuration 
using `<contextDir>` as the base path.
Showcased in the configuration for the `context-and-file` Maven profile. Will use Dockerfile
located in path `src/main/docker-context-dir/other/file/DockerfileWithChangedFileName`.

```shell script
$ mvn clean package k8s:build -P'context-and-file'
...
$ docker image inspect jkube/docker-file-provided | jq ".[].ContainerConfig.Labels" | grep location 
  "location": "src/main/docker-context-dir/other/file",
```

## `contextDir` and customized assembly name
Retrieves the `Dockerfile` from the provided `<contextDir>` configuration.

Adds the packaged artifact file to a directory with the name provided in the `<assembly>` configuration.

```shell script
$ mvn clean package k8s:build -P'context-and-assembly'
...
$ docker image inspect jkube/context-and-assembly | jq ".[].ContainerConfig.Labels" | grep location 
  "location": "/",
```