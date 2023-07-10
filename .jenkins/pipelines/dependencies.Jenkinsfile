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
    stage('Check dependency licenses') {
      steps {
        sh 'echo "Eclipse Dash Tool"'
        // Eclipse Dash tool retrieves dependencies from submodules which might need artifacts from previous modules
        sh './mvnw -V -B -e -DskipTests install'
        sh 'ECLIPSE_DASH_VERSION=1.0.2 ./scripts/eclipse-dash-tool.sh'
      }
    }
  }
  post {
    success {
      archiveArtifacts artifacts: 'target/dependencies-resolved.csv', fingerprint: true
    }
  }
}
