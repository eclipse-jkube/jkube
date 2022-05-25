# Demo Application consuming FooGenerator

This is a basic application which would be showcasing how user can integrate its own generator with Eclipse JKube Kubernetes Gradle Plugin. 

## Prerequisite
You need to build `foo-generator` module in `foo-generator/` directory and publish it to local maven repository in order to use it from this module.

## Adding Generator as Plugin dependency

Generator dependency needs to be declared in gradle buildscript dependencies:
```groovy
buildscript {
    repositories {
        mavenLocal()
    }
    dependencies {
        classpath('org.eclipse.jkube.quickstarts.gradle:foo-generator:1.8.0-SNAPSHOT')
    }
}
```

## Adding Generator as Compile time dependency

You can also configure Eclipse JKube Kubernetes Gradle Plugin to look into project dependencies for potential generators. This can be done via setting `useProjectClassPath` flag to `true`. Here is an example:

```groovy
dependencies {
    // Generator Dependency
    compileOnly 'org.eclipse.jkube.quickstarts.gradle:foo-generator:1.8.0-SNAPSHOT'
}

kubernetes {
    // Also look into project class path for enrichers/generators
    useProjectClassPath = true
    // ...
}
```

## Using the Generator
If you'll run `gradle k8sBuild`, you'll see generator kicks in during the build:
```
$ gradle k8sBuild
> Task :k8sBuild
k8s: Running generator foo
k8s: Add Environment variable to ImageConfigurations

```

Inspect the image built by plugin, you should be able to see environment variable added to image by the generator:
```sh
$ docker inspect 2989dee2ddd3 
[
    {
        "Id": "sha256:2989dee2ddd3d8d19f126e132ae221d32f4501ada216562fa12d738b6d24f967",
        "RepoTags": [
            "jkube/jkube-gradle-sample-custom-foo-generator-app:0.0.1-SNAPSHOT"
        ],
        "ContainerConfig": {
            "StdinOnce": false,
            "Env": [
                "foo=fooval"
            ],
```
