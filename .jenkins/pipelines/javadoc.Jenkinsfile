#!groovy

pipeline {
  agent any
  tools {
    maven 'apache-maven-latest'
    // https://wiki.eclipse.org/Jenkins#JDK
    jdk 'temurin-jdk11-latest'
  }
  options {
    disableConcurrentBuilds(abortPrevious: true)
  }
  stages {
    stage('Validate Javadoc') {
      steps {
        sh 'echo "Building Project with Javadoc"'
        sh './mvnw -V -B -e -Pjavadoc -DskipTests install javadoc:jar'
      }
    }
  }
}
