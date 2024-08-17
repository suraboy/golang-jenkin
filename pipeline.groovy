pipeline {
    agent any

    environment {
        DOCKER_HOME = '/usr/local/bin/docker'
        DOCKER_USERNAME = '0054'
      
        DOCKER_CREDENTIALS_ID = 'dockerpwd'
        DOCKER_IMAGE = 'suraboy/golang-jenkin:latest'

        REPO_URL = 'https://github.com/suraboy/golang-jenkin.git'
        BRANCH_NAME = 'main'

        GIT_CREDENTIALS_ID = 'gitpwd'
        GO_VERSION = '1.22'

        KUBECTL_HOME = '/usr/local/bin/kubectl'
        K8S_CREDENTIALS_ID='kubectlpwd'
        K8S_NAMESPACE = 'minikube-local'

        IMAGE_NAME = 'golang-jenkin'
        IMAGE_TAG = "1.0.${BUILD_NUMBER}"
    }

    stages {
        stage('Clean Workspace') {
            steps {
                deleteDir() // Clean the workspace before starting
            }
        }
        stage('Checkout') {
            steps {
                git branch: "${BRANCH_NAME}",
                    url: "${REPO_URL}",
                    credentialsId: "${GIT_CREDENTIALS_ID}"
            }
        }

        stage('Set Up Go Environment') {
            steps {
                script {
                     sh '''
                        export DOCKER_CLIENT_TIMEOUT=12000
                        export COMPOSE_HTTP_TIMEOUT=12000
                        echo "Docker client timeout: $DOCKER_CLIENT_TIMEOUT"
                        echo "Compose HTTP timeout: $COMPOSE_HTTP_TIMEOUT"
                    '''
                }
            }
        }

        stage('Clean Docker State') {
            steps {
                script {
                    sh '${DOCKER_HOME} system prune -af --volumes' // Clean all Docker resources
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    sh '${DOCKER_HOME} pull golang:${GO_VERSION}'
                    sh '${DOCKER_HOME} network prune --force'
                    sh '${DOCKER_HOME} build -t ${IMAGE_NAME}:${IMAGE_TAG} .'
                }
            }
        }

        stage('Push Image to Hub') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: "${DOCKER_CREDENTIALS_ID}", usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD')]) {
                        sh '${DOCKER_HOME} login -u ${DOCKER_USERNAME} -p ${DOCKER_PASSWORD}'
                        sh '${DOCKER_HOME} tag ${IMAGE_NAME}:${IMAGE_TAG} ${DOCKER_USERNAME}/${IMAGE_NAME}:${IMAGE_TAG}'
                        sh '${DOCKER_HOME} push ${DOCKER_USERNAME}/${IMAGE_NAME}:${IMAGE_TAG}'

                        // Check for dangling images and remove them if any are found
                        def danglingImages = sh(script: "${DOCKER_HOME} images -f 'dangling=true' -q", returnStdout: true).trim()

                                    if (danglingImages) {
                                        // Split the output into a list, then iterate over it to remove each image
                                        danglingImages.split("\n").each { image ->
                                            sh "${DOCKER_HOME} rmi -f ${image}"
                                        }
                                    } else {
                                        echo 'No dangling images to remove.'
                                    }
                    }
                }
            }
        }

        stage('Deploy to k8s') {
            steps {
                withKubeConfig([credentialsId: "${K8S_CREDENTIALS_ID}", serverUrl: 'https://127.0.0.1:56786']) {
                    script {
                        // Replace the image tag in the deployment YAML file
                        sh "sed -i '' 's/\$IMAGE_TAG/$IMAGE_TAG/g' k8s/deployment.yaml"
                        sh 'cat k8s/deployment.yaml'
                    }
                    sh '${KUBECTL_HOME} get pods -n ${K8S_NAMESPACE}'
                    sh '${KUBECTL_HOME} apply -f k8s/deployment.yaml -n ${K8S_NAMESPACE}'
                    sh '${KUBECTL_HOME} apply -f k8s/service.yaml -n ${K8S_NAMESPACE}'
                }
            }
        }

    }

    post {
        always {
            // Archive package version for reference
            writeFile file: 'version.txt', text: "${IMAGE_TAG}"
            archiveArtifacts artifacts: 'version.txt'
            buildName("Build #${BUILD_NUMBER} - Version ${env.IMAGE_TAG}")
        }
    }
}
