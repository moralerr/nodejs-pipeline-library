def call(Map config = [:]) {
    def defaultConfig = readYaml(text: libraryResource('org/moralerr/config/defaultConfig.yml'))
    config = defaultConfig + config

    pipeline {
        agent any
        environment {
            DOCKER_CREDENTIALS_ID = config.dockerCredentialsId
            DOCKER_REGISTRY_URL = config.dockerRegistryUrl
        }
        stages {
            stage('Checkout') {
                steps {
                    git url: config.repoUrl, branch: config.branch
                }
            }
            stage('Build') {
                steps {
                    sh 'npm install'
                    sh 'npm run build'
                }
            }
            stage('Test') {
                steps {
                    sh 'npm test'
                }
            }
            stage('Docker Build') {
                steps {
                    script {
                        def imageTag = "${config.dockerRegistryUrl}/${config.dockerImageName}:${config.branch}-${env.BUILD_NUMBER}"
                        sh "docker build -t ${imageTag} ."
                    }
                }
            }
            stage('Docker Push') {
                steps {
                    script {
                        def imageTag = "${config.dockerRegistryUrl}/${config.dockerImageName}:${config.branch}-${env.BUILD_NUMBER}"
                        withCredentials([usernamePassword(credentialsId: env.DOCKER_CREDENTIALS_ID, passwordVariable: 'DOCKER_PASSWORD', usernameVariable: 'DOCKER_USERNAME')]) {
                            sh "echo $DOCKER_PASSWORD | docker login -u $DOCKER_USERNAME --password-stdin ${config.dockerRegistryUrl}"
                            sh "docker push ${imageTag}"
                        }
                    }
                }
            }
            stage('Release') {
                when {
                    expression {
                        return env.GIT_TAG
                    }
                }
                steps {
                    script {
                        def releaseTag = "${config.dockerRegistryUrl}/${config.dockerImageName}:${env.GIT_TAG}"
                        sh "docker tag ${imageTag} ${releaseTag}"
                        sh "docker push ${releaseTag}"
                    }
                }
            }
        }
    }
}
