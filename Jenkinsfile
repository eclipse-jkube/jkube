#!groovy

pipeline {
  agent any
  tools {
    maven 'apache-maven-latest'
    jdk 'adoptopenjdk-hotspot-jdk8-latest'
  }
  stages {
    stage('Build') {
        steps {
            withCredentials([file(credentialsId: 'secret-subkeys.asc', variable: 'KEYRING')]) {
                sh 'gpg --batch --import "${KEYRING}"'
                sh 'for fpr in $(gpg --list-keys --with-colons  | awk -F: \'/fpr:/ {print $10}\' | sort -u); do echo -e "5\ny\n" |  gpg --batch --command-fd 0 --expert --edit-key ${fpr} trust; done'
            }
            withCredentials([string(credentialsId: '00d22e13-04f3-432a-b95b-90893e93e70b', variable: 'GH_TOKEN')]) {
            }
            sh '''
                # Setup Git Config
                git config --global user.email eclipsejkubebot@eclipse.org
                git config --global user.name "Eclipse JKube Bot"
                git clone https://eclipse-jkube-bot:$GH_TOKEN@github.com/eclipse/jkube.git && cd jkube
                
                # Find Project release version
                HEAD=$(git log -1 --format=format:%H)
                PROJECT_VERSION=$(mvn -q -Dexec.executable=echo -Dexec.args='${project.version}' --non-recursive exec:exec)
                NEXT_RELEASE_VERSION=`echo $PROJECT_VERSION | sed 's/-SNAPSHOT//g'`
                if [ ${#NEXT_RELEASE_VERSION} -eq 3 ]; then
                    NEXT_RELEASE_VERSION=`echo "$NEXT_RELEASE_VERSION.0"`
                fi

                echo "Releasing project with version $NEXT_RELEASE_VERSION"

                # Prepare project for release, modify pom to new release version
                mvn versions:set -DnewVersion=$NEXT_RELEASE_VERSION
                find . -iname *.versionsBackup -exec rm {} +
                git add . && git commit -m "[RELEASE] Modified Project pom version to $NEXT_RELEASE_VERSION"
                git tag $NEXT_RELEASE_VERSION
                git push origin $NEXT_RELEASE_VERSION
                git push origin master

                mvn clean -B
                mvn -V -B -e -U install org.sonatype.plugins:nexus-staging-maven-plugin:1.6.7:deploy -P release -DnexusUrl=https://oss.sonatype.org -DserverId=ossrh
                
                # Modify poms back to SNAPSHOT VERSIONS
                MAJOR_VERSION=`echo $NEXT_RELEASE_VERSION | cut -d. -f1`
                MINOR_VERSION=`echo $NEXT_RELEASE_VERSION | cut -d. -f2`
                PATCH_VERSION=`echo $NEXT_RELEASE_VERSION | cut -d. -f3`
                PATCH_VERSION=$(($PATCH_VERSION + 1))
                NEXT_SNAPSHOT_VERSION=`echo "$MAJOR_VERSION.$MINOR_VERSION.$PATCH_VERSION-SNAPSHOT"`
                mvn versions:set -DnewVersion=$NEXT_SNAPSHOT_VERSION
                find . -iname *.versionsBackup -exec rm {} +
                git add . && git commit -m "[RELEASE] Prepare project for next development iteration $NEXT_SNAPSHOT_VERSION"
                git push origin master


                # read repo_id from *.properties file and set it
                repo_id=$(cat target/nexus-staging/staging/*.properties | grep id | awk -F'=' '{print $2}')
                mvn -B org.sonatype.plugins:nexus-staging-maven-plugin:1.6.5:rc-release -DserverId=ossrh -DnexusUrl=https://oss.sonatype.org -DstagingRepositoryId=${repo_id} -Ddescription=\"Next release is ready\" -DstagingProgressTimeoutMinutes=60
                '''
        }
    }
  }
}

