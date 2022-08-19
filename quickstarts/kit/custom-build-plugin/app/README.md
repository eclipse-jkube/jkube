# Custom Build Plugin App

A simple java application making use of custom foo plugin. Foo Build Plugin is declared as Kubernetes Maven Plugin dependency:

```xml
      <plugin>
        <groupId>org.eclipse.jkube</groupId>
        <artifactId>kubernetes-maven-plugin</artifactId>
        <version>${jkube.version}</version>
        <dependencies>
          <dependency>
            <groupId>org.eclipse.jkube.quickstarts.kit</groupId>
            <artifactId>jkube-sample-foo-build-plugin</artifactId>
            <version>${project.version}</version>
          </dependency>
        </dependencies>
      </plugin>
```

When user is doing `mvn k8s:build` for building the image Kubernetes Maven Plugin extracts `foo-java.sh` script and copies it to `target/classes/jkube-extra/` directory. In this case, we're copying script into container image and executing it on startup (see Dockerfile):

```Dockerfile
ADD maven/target/classes/jkube-extra/foo-java/foo-java.sh /opt

CMD JAVA_MAIN_CLASS=org.eclipse.jkube.quickstart.foojava.HelloWorld JAVA_APP_DIR=/opt sh /opt/foo-java.sh
```

When you build container image, script should be successfully copied and executed on startup:

```shell
app : $ mvn k8s:build

...
[INFO] k8s: Extra files from org.eclipse.jkube.quickstart.kit.FooScriptLoader extracted
...

app : $ tree target/classes/
target/classes/
── jkube-extra
 └── foo-java
     └── foo-java.sh


1 directory, 1 file
app : $ docker run 8a54c
Hello world !

```