pipeline {
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
}
