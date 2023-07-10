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
    stage('Generate documentation preview') {
      when { changeRequest() }
      steps {
        sh 'echo "Generating project documentation"'
        sh './scripts/generateDoc.sh'
      }
    }
  }
  post {
    success {
      archiveArtifacts artifacts: 'docs-generated/*.html,docs-generated/*.txt,docs-generated/*.css', fingerprint: true
    }
  }
}
