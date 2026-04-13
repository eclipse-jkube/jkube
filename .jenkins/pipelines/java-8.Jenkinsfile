#!groovy

pipeline {
  agent any

  tools {
    maven 'apache-maven-latest'
    // https://wiki.eclipse.org/Jenkins#JDK
    jdk 'temurin-jdk8-latest'
  }

  environment {
    MAVEN_OPTS = '-Dmaven.repo.local=.m2/repository'
  }

  options {
    disableConcurrentBuilds(abortPrevious: true)
    timestamps()
  }

  stages {
    stage('Verify Java Version') {
      steps {
        sh 'echo "Checking Java version..."'
        sh '''
          if [[ "$(javac -version 2>&1)" == *"1.8.0"* ]]; then
            echo "Java 8 Present."
          else
            echo "Java 8 Not Present."
            exit 1
          fi
        '''
      }
    }

    stage('Clean & Build') {
      steps {
        sh 'echo "Building project with Maven..."'
        sh './mvnw -V -B clean install -DskipTests'
      }
    }

    stage('Run Tests') {
      steps {
        sh 'echo "Running unit tests..."'
        sh './mvnw -B test'
      }
    }

    stage('Code Quality') {
      steps {
        sh 'echo "Running code quality checks..."'
        sh './mvnw -B verify'
      }
    }
  }

  post {
    always {
      junit testResults: '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml', allowEmptyResults: true
      archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true, allowEmptyArchive: true
    }

    success {
      echo 'Pipeline completed successfully.'
    }

    failure {
      echo 'Pipeline failed. Please check the logs.'
    }
  }
}
