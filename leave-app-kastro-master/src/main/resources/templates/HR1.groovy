pipeline {
    agent { label 'agent-1' }
    parameters {
        string(name: 'BRANCH', defaultValue: 'dev', description: 'Git branch to build')
    }
    environment {
        GIT_REPO = 'https://github.com/Vikasghandge/HR-Leave_App.git'
        APP_DIR = 'HR-Leave_App'
    }
    stages {
        stage('Checkout') {
            steps {
                sh """
                echo "Pulling branch: ${params.BRANCH}"
                rm -rf \$APP_DIR
                git clone -b ${params.BRANCH} \$GIT_REPO \$APP_DIR
                """
            }
        }
        stage('Build & Run Docker Compose') {
            steps {
                sh """
                cd \$APP_DIR/leave-app-kastro-master
                docker-compose down
                docker-compose up -d --build
                """
            }
        }
        stage('Verify') {
            steps {
                sh """
                docker ps
                """
            }
        }
    }
}
