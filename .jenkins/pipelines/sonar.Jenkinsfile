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
        // Required by Sonar to compare coverage info, etc. with master
        sh 'git remote add upstream https://github.com/eclipse/jkube.git'
        sh 'git fetch upstream'
        // Needs install instad of verify since ITs rely on artifacts from previous modules
        sh './mvnw -V -B -e -Pjacoco,sonar install ' +
           '-Dsonar.pullrequest.key=${CHANGE_ID} ' +
           '-Dsonar.pullrequest.branch=${GIT_BRANCH} ' +
           '-Dsonar.pullrequest.base=master'
        // CodeCov
        withCredentials([string(credentialsId: 'CODECOV_TOKEN', variable: 'CODECOV_TOKEN')]) {
          sh 'wget -O - https://codecov.io/bash | bash -s -- -t $CODECOV_TOKEN'
        }
      }
    }
    stage('Sonar (main)') {
      when { not { changeRequest() } }
      steps {
        sh 'echo "Building Project and analyzing with Sonar"'
        // Needs install instad of verify since ITs rely on artifacts from previous modules
        sh './mvnw -V -B -e -Pjacoco,sonar install'
        // CodeCov
        sh 'wget -O - https://codecov.io/bash | bash'
      }
    }
  }
}
