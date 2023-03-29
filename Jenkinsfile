def getRepoURL() {
  sh "git config --get remote.origin.url > .git/remote-url"
  return readFile(".git/remote-url").trim()
}

def getCommitSha() {
  sh "git rev-parse HEAD > .git/current-commit"
  return readFile(".git/current-commit").trim()
}

void setBuildStatus(String message, String state) {
  repoUrl = getRepoURL()
  commitSha = getCommitSha()
  step([
      $class: "GitHubCommitStatusSetter",
      reposSource: [$class: "ManuallyEnteredRepositorySource", url: repoUrl],
      contextSource: [$class: "ManuallyEnteredCommitContextSource", context: "ci/jenkins/build-status"],
      commitShaSource: [$class: "ManuallyEnteredShaSource", sha: commitSha],
      statusResultSource: [ $class: "ConditionalStatusResultSource", results: [[$class: "AnyBuildResult", message: message, state: state]] ]
  ]);
}

pipeline {
    setBuildStatus("Build started", "PENDING")
    agent {
        docker {
            image 'maven:3-eclipse-temurin-8'
            args '-v /root/.m2:/root/.m2'
        }
    }
    stages {
        stage('Build') {
            steps {
                checkout scm
                withCredentials([usernamePassword(credentialsId: 'c0d3m4513r-deployment', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]){
                    sh 'mvn -B --settings settings.xml clean deploy -Dusername=$USERNAME -Dpassword=$PASSWORD'
                }
            }
        }
    }
  post {
    success {
        setBuildStatus("Build succeeded", "SUCCESS");
    }
    failure {
        setBuildStatus("Build failed", "FAILURE");
    }
  }
}
