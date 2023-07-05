#!groovy

pipeline {
  agent any
  tools {
    maven 'apache-maven-latest'
    jdk 'temurin-jdk8-latest'
  }
  stages {
    stage('Release Snapshots') {
      steps {
        withCredentials([file(credentialsId: 'secret-subkeys.asc', variable: 'KEYRING')]) {
          sh 'echo "Setting up GPG signing keys"'
          sh 'gpg --batch --import "${KEYRING}"'
          sh 'for fpr in $(gpg --list-keys --with-colons  | awk -F: \'/fpr:/ {print $10}\' | sort -u); do echo -e "5\ny\n" |  gpg --batch --command-fd 0 --expert --edit-key ${fpr} trust; done'
        }
        sshagent(['github-bot-ssh']) {
          sh 'echo "Cloning Project"'
          // sh 'git clone git@github.com:eclipse/jkube.git' // disabled temporarily, credentials not working
          sh 'git clone https://github.com/eclipse/jkube.git'
        }
        dir('jkube') {
          sh 'echo "Deploying Snapshots"'
          sh 'mvn -V -B -e -U -Prelease -Denforcer.skip=true deploy'
        }
      }
    }
  }
}
