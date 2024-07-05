def call(Map config = [:]) {
    def defaultConfig = readYaml(text: libraryResource('org/moralerr/config/defaultConfig.yml'))
    config = defaultConfig + config
    println config

    pipeline {
        agent {
            kubernetes {
                inheritFrom 'nodejs'
            }
        }
        environment {
            DOCKER_CREDENTIALS_ID = "${config.dockerCredentialsId}"
            DOCKER_REGISTRY_URL = "${config.dockerRegistryUrl}"
        }
        stages {
            stage('Copy Dockerfile') {
                steps {
                    script {
                        if (!fileExists('Dockerfile')) {
                            writeFile file: 'Dockerfile', text: libraryResource('Dockerfile')
                        }
                    }
                }
            }
            stage('Build') {
                steps {
                    sh 'npm install'
                    sh 'npm run build'
                }
            }
            stage('Docker Build') {
                steps {
                    container('dind') {
                        script {
                            def imageTag = "${config.dockerRegistryUrl}:${config.dockerImageName}-${config.branch}-${env.BUILD_NUMBER}"
                            def buildEnv = config.buildEnv ?: 'production'
                            sh "docker build --build-arg BUILD_ENV=${buildEnv} -t ${imageTag} ."
                        }
                    }
                }
            }
            stage('Docker Push') {
                steps {
                    container('dind') {
                        script {
                            def imageTag = "${config.dockerRegistryUrl}:${config.dockerImageName}-${config.branch}-${env.BUILD_NUMBER}"
                            withCredentials([usernamePassword(credentialsId: env.DOCKER_CREDENTIALS_ID, passwordVariable: 'DOCKER_PASSWORD', usernameVariable: 'DOCKER_USERNAME')]) {
                                sh 'echo $DOCKER_PASSWORD | docker login --username $DOCKER_USERNAME --password-stdin'
                                sh "docker push ${imageTag}"
                            }
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
                    container('dind') {
                        script {
                            def releaseTag = "${config.dockerRegistryUrl}:${config.dockerImageName}-${env.GIT_TAG}"
                            sh "docker tag ${imageTag} ${releaseTag}"
                            sh "docker push ${releaseTag}"
                        }
                    }
                }
            }
        }
    }
}
