# Building Eclipse JKube (Internal documentation)

## Javadoc
_Applicable profiles: javadoc, release_


Javadoc can be generated for the global project running the following command from the root directory:
```shell script
mvn clean install -Pjavadoc
```

This will generate Javadoc for each module in `target/apidocs` and a jar package
`target/${project.artifactId}-${project.version}.jar`.

The affected profiles will ["delombok"](http://anthonywhitford.com/lombok.maven/lombok-maven-plugin/)
the source code before generating the documentation in order to spread the 
"condensed" javadoc declared in the Lombok affected fields.

## Release process
Release process has to be performed in a project fork.

A pull request with 2 commits must be generated, one with the release version and
another with the next snapshot version.

### Steps
0. Update/rebase master branch to upstream
0. Set release version
   0. Set release version for all modules
   ```shell script
   mvn versions:set -DnewVersion=x.y.z -DgenerateBackupPoms=false
   ```
   0. Set release version for quickstarts
   ```shell script
   ./scripts/quickstarts.sh version
   ```
0. Update documentation
   0. Add new entry to compatibility matrices
      0. [./kubernetes-maven-plugin/doc/src/main/asciidoc/inc/_compatibility.adoc](./kubernetes-maven-plugin/doc/src/main/asciidoc/inc/_compatibility.adoc)
      0. [./kubernetes-maven-plugin/README.md](./kubernetes-maven-plugin/README.md)
      0. [./openshift-maven-plugin/doc/src/main/asciidoc/inc/_compatibility.adoc](./openshift-maven-plugin/doc/src/main/asciidoc/inc/_compatibility.adoc)
   0. Set date and version in [CHANGELOG.md](./CHANGELOG.md) SNAPSHOT entry