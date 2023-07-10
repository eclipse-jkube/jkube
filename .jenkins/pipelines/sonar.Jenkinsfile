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
    stage('Sonar (PR)') {
      when { changeRequest() }
      steps {
        sh 'echo "Building Project and analyzing with Sonar"'
        // Needs install instad of verify since ITs rely on artifacts from previous modules
        sh './mvnw -V -B -e -Pjacoco,sonar install ' +
           '-Dsonar.pullrequest.key=${CHANGE_ID} ' +
           '-Dsonar.pullrequest.branch=${GIT_BRANCH} ' +
           '-Dsonar.pullrequest.base=master'
      }
    }
    stage('Sonar (main)') {
      when { not { changeRequest() } }
      steps {
        sh 'echo "Building Project and analyzing with Sonar"'
        // Needs install instad of verify since ITs rely on artifacts from previous modules
        sh './mvnw -V -B -e -Pjacoco,sonar install'
      }
    }
  }
}
