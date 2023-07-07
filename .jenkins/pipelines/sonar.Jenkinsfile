#!groovy

pipeline {
  agent any
  tools {
    maven 'apache-maven-latest'
    // https://wiki.eclipse.org/Jenkins#JDK
    jdk 'temurin-jdk11-latest'
  }
  stages {
    stage('Sonar') {
      steps {
        sshagent(['github-bot-ssh']) {
          sh 'echo "Cloning Project"'
          sh 'git clone git@github.com:eclipse/jkube.git'
        }
        dir('jkube') {
          sh 'echo "Building Project and analyzing with Sonar"'
          // Needs install instad of verify since ITs rely on artifacts from previous modules
          sh './mvnw -V -B -e -Pjacoco,sonar install ' +
             '-Dsonar.pullrequest.key=${CHANGE_ID} ' +
             '-Dsonar.pullrequest.branch=${GIT_BRANCH} ' +
             '-Dsonar.pullrequest.base=master'
        }
      }
    }
  }
}
