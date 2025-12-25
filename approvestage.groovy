pipeline {
    agent any

    tools {
        jdk 'jdk17'
        maven 'maven3'
    }

    environment {
        DOCKER_HUB_CREDS = credentials('docker-creds')
        DOCKER_IMAGE = "ghandgevikas/leave-management"
        DOCKER_TAG = "${BUILD_NUMBER}"
        APP_DIR = "leave-app-kastro-master"
    }

    stages {
        stage('Clean Workspace') {
            steps {
                cleanWs()
            }
        }

        stage('Git Clone') {
            steps {
                git branch: 'dev', 
                url: 'https://github.com/Vikasghandge/HR-Leave_App.git',
                poll: false
            }
        }

        stage('Build Project') {
            steps {
                dir("${APP_DIR}") {
                    sh 'mvn clean package -DskipTests'
                }
            }
        }

        stage('Prepare Docker Assets') {
            steps {
                dir("${APP_DIR}") {
                    sh '''
                    wget https://raw.githubusercontent.com/vishnubob/wait-for-it/master/wait-for-it.sh -O wait-for-it.sh
                    sed -i '1s|^.*$|#!/usr/bin/env bash|' wait-for-it.sh
                    sed -i 's/\r$//' wait-for-it.sh
                    chmod +x wait-for-it.sh
                    '''
                }
            }
        }

        stage('Create Docker Image') {
            steps {
                dir("${APP_DIR}") {
                    sh """
                    docker build --no-cache -t ${DOCKER_IMAGE}:${DOCKER_TAG} .
                    docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${DOCKER_IMAGE}:latest
                    """
                }
            }
        }

        stage('Approval: Push to Docker Hub') {
            steps {
                script {
                    // Send notification email
                    emailext (
                        subject: "Approval Required: Docker Image Push for Build #${BUILD_NUMBER}",
                        body: """
                        The Docker image is ready to be pushed to Docker Hub.
                        Build Number: ${BUILD_NUMBER}
                        Image: ${DOCKER_IMAGE}:${DOCKER_TAG}
                        
                        Please review and approve or reject this deployment.
                        
                        Jenkins Build URL: ${BUILD_URL}
                        """,
                        to: 'ghandgevikas804@gmail.com',
                        recipientProviders: [[$class: 'DevelopersRecipientProvider']]
                    )
                    
                    // Wait for manual approval
                    timeout(time: 24, unit: 'HOURS') {
                        input(
                            message: 'Approve Docker Image Push?', 
                            ok: 'Approve',
                            submitterParameter: 'approver'
                        )
                    }
                }
            }
        }

        stage('Push Docker Image') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'docker-creds', 
                    usernameVariable: 'DOCKER_USER', 
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh """
                    echo \$DOCKER_PASS | docker login -u \$DOCKER_USER --password-stdin
                    docker push ${DOCKER_IMAGE}:${DOCKER_TAG}
                    docker push ${DOCKER_IMAGE}:latest
                    """
                }
            }
        }

        stage('Clean Old Deployment') {
            steps {
                dir("${APP_DIR}") {
                    sh '''
                    docker compose down --rmi all --volumes --remove-orphans || true
                    docker system prune -af || true
                    '''
                }
            }
        }

        stage('Deploy with Docker Compose') {
            steps {
                dir("${APP_DIR}") {
                    sh """
                    docker compose build --no-cache
                    DOCKER_TAG=${DOCKER_TAG} docker compose up -d --force-recreate
                    sleep 15
                    """
                }
            }
        }

        stage('Verify Deployment') {
            steps {
                dir("${APP_DIR}") {
                    sh '''
                    docker ps -a
                    docker logs leave-management-app --tail 50
                    curl -I http://localhost:8090/actuator/health || true
                    '''
                }
            }
        }
    }

    post {
        always {
            cleanWs()
        }
        failure {
            emailext body: 'Build ${BUILD_NUMBER} failed. Please check Jenkins for details.', 
                    subject: 'Jenkins Build Failed: ${JOB_NAME} #${BUILD_NUMBER}',
                    to: 'ghandgevikas804@gmail.com'
        }
        success {
            script {
                if (currentBuild.currentResult == 'SUCCESS') {
                    emailext (
                        subject: "Build Successful: ${JOB_NAME} #${BUILD_NUMBER}",
                        body: """
                        Build Number: ${BUILD_NUMBER}
                        Status: SUCCESS
                        
                        Docker Image: ${DOCKER_IMAGE}:${DOCKER_TAG}
                        Approver: ${env.approver ?: 'Automated'}
                        
                        Jenkins Build URL: ${BUILD_URL}
                        """,
                        to: 'ghandgevikas@gmail.com'
                    )
                }
            }
        }
    }
}
