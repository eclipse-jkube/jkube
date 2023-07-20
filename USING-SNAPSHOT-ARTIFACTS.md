+# How to use Eclipse JKube snapshot artifacts?

- +Artifacts are hosted at [JKube's Sonatype Snapshot repository](https://oss.sonatype.org/content/repositories/snapshots/org/eclipse/jkube/)
- +Our [Jenkins Snapshot release pipeline](https://ci.eclipse.org/jkube/job/ReleasePipelines/job/ReleaseSnapshots/) updates SNAPSHOT artifacts every night.
-
- +## Using SNAPSHOTs in Maven Project
  +In order to use these artifacts, update your `pom.xml` with these:
- +```xml +<pluginRepositories>
- <pluginRepository>
- <id>oss.sonatype.org</id>
- <url>https://oss.sonatype.org/content/repositories/snapshots</url>
- <snapshots>
-      <enabled>true</enabled>
-      <updatePolicy>always</updatePolicy>
- </snapshots>
- </pluginRepository>
  +</pluginRepositories>
  +```
- +You'd also need to update version of the plugin you're using to use a SNAPSHOT version instead of a stable version. Here is an example:
- +```xml +<properties>
- <jkube.version>x.yz-SNAPSHOT</jkube.version> +</properties>
- +<build>
- <plugins>
-        <plugin>
-            <groupId>org.eclipse.jkube</groupId>
-            <artifactId>kubernetes-maven-plugin</artifactId>
-            <version>${jkube.version}</version>
-        </plugin>
- </plugins>
  +</build>
  +```
- +## Using SNAPSHOTS in Gradle project
- +In order to use these artifacts in Gradle project, update your `settings.gradle` pluginManagement section like this:
- +```groovy
  +pluginManagement {
- repositories {
-        maven { url "https://oss.sonatype.org/content/repositories/snapshots" }
- }
  +}
  +```
- +You'd also need to update version of the plugin you're using to use a SNAPSHOT version instead of a stable version. Here is an example:
- +```groovy
  +plugins {
- id "org.eclipse.jkube.openshift" version "x.yz-SNAPSHOT"
- id "org.eclipse.jkube.kubernetes" version "x.yz-SNAPSHOT"
  +}
  +```