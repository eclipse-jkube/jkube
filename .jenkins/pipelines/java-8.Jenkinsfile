#!groovy

pipeline {
  agent any
  tools {
    maven 'apache-maven-latest'
    // https://wiki.eclipse.org/Jenkins#JDK
    jdk 'temurin-jdk8-latest'
  }
  options {
    disableConcurrentBuilds(abortPrevious: true)
  }
  stages {
    stage('Build & Test (Java 8)') {
      steps {
        sh 'echo "Building Project with Java 8"'
        sh '''
          if [[ `javac -version 2>&1` == *"1.8.0"* ]]; then
            echo "Java 8 Present."
          else
            echo "Java 8 Not Present."
            exit 1
          fi
        '''
        sh './mvnw -V -B -e  install'
      }
    }
  }
}
