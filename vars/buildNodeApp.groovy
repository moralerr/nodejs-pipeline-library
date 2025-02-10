def call(Map config = [:]) {
    library('pipeline-commons@preview')
    def defaultConfig = readYaml(text: libraryResource('org/moralerr/config/defaultConfig.yml'))
    config = defaultConfig + config
    println config
    utils.logMapReadable(config)
    utils.replaceTemplateStringsInYamlContent(config)
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
                        echo 'Checking if Dockerfile exists...'
                        if (!fileExists('Dockerfile')) {
                            echo 'Dockerfile not found. Copying from library resources...'
                            writeFile file: 'Dockerfile', text: libraryResource('Dockerfile')
                            echo 'Dockerfile successfully copied.'
                        } else {
                            echo 'Dockerfile already exists. No action taken.'
                        }
                    }
                }
            }
            // stage('Build') {
            //     steps {
            //         sh 'npm config set fetch-retry-mintimeout 20000'
            //         sh 'npm config set fetch-retry-maxtimeout 120000'
            //         sh 'npm install'
            //         sh 'npm run build'
            //     }
            // }
            // stage('Docker Build') {
            //     steps {
            //         container('dind') {
            //             script {
            //                 def imageTag = "${config.dockerRegistryUrl}:${config.dockerImageName}-${config.branch}-${env.BUILD_NUMBER}"
            //                 def buildEnv = config.buildEnv ?: 'production'
            //                 def sourceDir = config.sourceDir ?: '/app/dist'
            //                 sh "docker build --build-arg BUILD_ENV=${buildEnv} --build-arg SOURCE_DIR=${sourceDir} -t ${imageTag} ."
            //             }
            //         }
            //     }
            // }
            // stage('Docker Push') {
            //     steps {
            //         container('dind') {
            //             script {
            //                 def imageTag = "${config.dockerRegistryUrl}:${config.dockerImageName}-${config.branch}-${env.BUILD_NUMBER}"
            //                 withCredentials([usernamePassword(credentialsId: env.DOCKER_CREDENTIALS_ID, passwordVariable: 'DOCKER_PASSWORD', usernameVariable: 'DOCKER_USERNAME')]) {
            //                     sh 'echo $DOCKER_PASSWORD | docker login --username $DOCKER_USERNAME --password-stdin'
            //                     sh "docker push ${imageTag}"
            //                 }
            //             }
            //         }
            //     }
            // }
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
            stage('Auto Deploy') {
                steps {
                    script{
                        sh 'rm -rf helm-charts'
                        withCredentials([usernamePassword(credentialsId: 'GITHUB_ADMIN_TOKEN_AS_PASS',
                                                             passwordVariable: 'GIT_PASSWORD',
                                                             usernameVariable: 'GIT_USERNAME')]) {
                            sh "git clone https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/moralerr/helm-charts.git helm-charts"
                                                             }
                        dir('helm-charts') {
                            def valuesFile = "applications/${config.dockerImageName}/values.yaml"
                            // Read file content
                            def valuesContent = readFile(file: valuesFile)
                            def newTag = "${config.dockerImageName}-${config.branch}-${env.BUILD_NUMBER}"
                            // Update only the 'tag' line within the 'image' section
                            def updatedContent = valuesContent.replaceAll(/(?m)^(\\s*tag:\\s*).*/, "\$1${newTag}")
                            writeFile(file: valuesFile, text: updatedContent)

                            sh '''
                                git config user.email "moralerrusc@gmail.com"
                                git config user.name "Ricky"
                            '''

                            sh 'git checkout -b test'

                            sh "git add ${valuesFile}"
                            sh "git commit -m 'Update image tag to ${newTag}'"
                            sh 'git push origin test'
                        }
                    }
                }
            }
        }
    }
}
