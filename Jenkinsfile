pipeline {
  agent {
    docker {
      image 'maven:3-jdk-8'
      args '-e MAVEN_CONFIG=/var/maven/.m2 -v /data/jenkins/m2-common:/var/maven/.m2 -v /data/jenkins/gpg:/var/maven/.gnupg'
    }
  }
  environment {
	GIT_COMMIT_SHORT = sh(
		script: "printf \$(git rev-parse --short ${GIT_COMMIT})",
		returnStdout: true
	)
  }
  stages {
    stage('Build') {
      steps {
        sh 'MAVEN_OPTS=-Duser.home=/var/maven mvn "-Djenkins=true" "-Drevision=0.1.${BUILD_NUMBER}" "-Dchangelist=" "-Dsha1=-${GIT_COMMIT_SHORT}" clean deploy'
      }
    }
  }
}