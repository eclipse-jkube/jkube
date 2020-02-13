# Migration Guide for projects using Fabric8 Maven Plugin to Eclipse Jkube

For any project which is using [Fabric8 Maven Plugin](https://github.com/fabric8io/fabric8-maven-plugin) right now. Migrating to Eclipse Jkube should not be that hard. Fabric8 Maven Plugin used to handle both Kubernetes and Openshift clusters but Eclipse Jkube has separate plugins for these two different environments.

## For Project Using Kubernetes
For any project deploying their applications onto Kubernetes, we need to replace [Fabric8 Maven Plugin](https://github.com/fabric8io/fabric8-maven-plugin) with Eclipse Jkube like this. Let's say in case of zero-configuration, we just need to replace `groupId` and `artifactId` like this:

### FMP zero configuration mode:
```
    <plugin>
        <groupId>io.fabric8</groupId>
        <artifactId>fabric8-maven-plugin</artifactId>
        <version>4.4.0</version>
    </plugin>
```

### Eclipse Jkube zero configuration mode:
```
    <plugin>
        <groupId>org.eclipse.jkube</groupId>
        <artifactId>k8s-maven-plugin</artifactId>
        <version>0.1.0</version>
    </plugin>
```

In cases where XML configuration is used for enrichers and generators. All the enrichers with prefixes `fmp` or `f8` are replaced with `jkube`. Let's have a look at this example:

### FMP XML configuration for enrichers, generators and resources:
```
    <plugin>
        <groupId>io.fabric8</groupId>
        <artifactId>fabric8-maven-plugin</artifactId>
        <version>4.4.0</version>
        <configuration>
            <resources>
                <labels>
                    <all>
                        <testProject>spring-boot-sample</testProject>
                    </all>
                </labels>
            </resources>

            <generator>
                <includes>
                    <include>spring-boot</include>
                </includes>
                <config>
                    <spring-boot>
                        <color>always</color>
                    </spring-boot>
                </config>
            </generator>
            <enricher>
                <excludes>
                    <exclude>f8-expose</exclude>
                </excludes>
                <config>
                    <fmp-service>
                        <type>NodePort</type>
                    </fmp-service>
                </config>
            </enricher>
        </configuration>

        <executions>
            <execution>
                <goals>
                    <goal>resource</goal>
                    <goal>build</goal>
                    <goal>helm</goal>
                </goals>
            </execution>
        </executions>
    </plugin>
```
### Eclipse Jkube XML configuration for enrichers, generators and resources:
```
    <plugin>
        <groupId>org.eclipse.jkube</groupId>
        <artifactId>k8s-maven-plugin</artifactId>
        <version>0.1.0</version>

        <configuration>
            <resources>
                <labels>
                    <all>
                        <testProject>spring-boot-sample</testProject>
                    </all>
                </labels>
            </resources>
            <generator>
                <includes>
                    <include>spring-boot</include>
                </includes>
                <config>
                    <spring-boot>
                        <color>always</color>
                    </spring-boot>
                </config>
            </generator>
            <enricher>
                <excludes>
                    <exclude>jkube-expose</exclude>
                </excludes>
                <config>
                    <jkube-service>
                        <type>NodePort</type>
                    </jkube-service>
                </config>
            </enricher>
        </configuration>

        <executions>
            <execution>
                <goals>
                    <goal>resource</goal>
                    <goal>build</goal>
                    <goal>helm</goal>
                </goals>
            </execution>
        </executions>
    </plugin>
```

In case you want to customize Kubernetes manifests added by FMP by means other than XML configuration, you usually add yourresources to `src/main/fabric8` directory and FMP used to pick these during enrichment process and merge it alongwith default generated resources. In case of Eclipse Jkube also it's the same, only the `src/main/fabric8` directory is replaced with`src/main/jkube` directory:

### Project making use of old FMP fragment configuration:

```
~/work/repos/fabric8-maven-plugin/samples/external-resources : $ ls src/main/fabric8/
deployment.yml  sa.yml  service.yml

```
### Project making use of Eclipse Jkube fragment configuration:
```
~/work/repos/jkube/quickstarts/maven/external-resources : $ ls src/main/jkube/
deployment.yml  sa.yml  service.yml
```


## For Project Using OpenShift
For any project deploying their applications onto OpenShift, we need to replace [Fabric8 Maven Plugin](https://github.com/fabric8io/fabric8-maven-plugin) with Eclipse Jkube like this. Let's say in case of zero-configuration, we just need to replace `groupId` and `artifactId` like this:

### FMP zero configuration mode:
```
    <plugin>
        <groupId>io.fabric8</groupId>
        <artifactId>fabric8-maven-plugin</artifactId>
        <version>4.4.0</version>
    </plugin>
```

### Eclipse Jkube zero configuration mode:
```
    <plugin>
        <groupId>org.eclipse.jkube</groupId>
        <artifactId>oc-maven-plugin</artifactId>
        <version>0.1.0</version>
    </plugin>
```

XML configuration and resource fragment configuration are same as Kubernetes Maven Plugin.

## Image Configuration for Docker builds

For projects relying on FMP's `ImageConfiguration` model for building docker images. There isn't any change in Eclipse Jkube in XML configuration. For example, let's consider this simple project's plugin configuration:

### FMP Image Configuration:
```
      <plugin>
        <groupId>io.fabric8</groupId>
        <artifactId>fabric8-maven-plugin</artifactId>
        <version>4.4.0</version>
        <configuration>
          <images>
            <image>
              <name>rohankanojia/helloworld-java:${project.version}</name>
              <alias>hello-world</alias>
              <build>
                <from>openjdk:latest</from>
                <assembly>
                  <descriptorRef>artifact</descriptorRef>
                </assembly>
                <cmd>java -jar maven/${project.name}-${project.version}.jar</cmd>
              </build>
              <run>
                <wait>
                  <log>Hello World!</log>
                </wait>
              </run>
            </image>
          </images>
        </configuration>
      </plugin>
```

### Eclipse Jkube Image Configuration:
```
      <plugin>
        <groupId>org.eclipse.jkube</groupId>
        <artifactId>k8s-maven-plugin</artifactId>
        <version>0.1.0</version>
        <configuration>
          <images>
            <image>
              <name>rohankanojia/helloworld-java:${project.version}</name>
              <alias>hello-world</alias>
              <build>
                <from>openjdk:latest</from>
                <assembly>
                  <descriptorRef>artifact</descriptorRef>
                </assembly>
                <cmd>java -jar maven/${project.name}-${project.version}.jar</cmd>
              </build>
              <run>
                <wait>
                  <log>Hello World!</log>
                </wait>
              </run>
            </image>
          </images>
        </configuration>
      </plugin>
```
