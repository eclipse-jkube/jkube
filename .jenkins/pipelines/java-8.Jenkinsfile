#!groovy

pipeline {
  agent any

  tools {
    maven 'apache-maven-latest'
    jdk 'temurin-jdk8-latest'
  }

  options {
    disableConcurrentBuilds(abortPrevious: true)
    timestamps()
  }

  environment {
    MAVEN_OPTS = "-Dmaven.repo.local=$WORKSPACE/.m2"
  }

  stages {

    stage('Verify Java Version') {
      steps {
        sh '''
          echo "Checking Java Version..."
          javac -version
        '''
      }
    }

    stage('Build & Test') {
      steps {
        sh '''
          echo "Building Project..."
          ./mvnw clean test install -B -V
        '''
      }
      post {
        always {
          junit 'target/surefire-reports/*.xml'
        }
      }
    }

    stage('Archive Artifacts') {
      steps {
        archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
      }
    }
  }

  post {
    success {
      echo "Build Successful ✅"
    }
    failure {
      echo "Build Fai
