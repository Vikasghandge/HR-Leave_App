
# 🧑‍💼 HR Leave Management App - CI/CD & K8s Deployment

## 🚀 Project Overview

This project sets up a complete CI/CD pipeline using Jenkins and Docker, with deployment via Docker Compose and Kubernetes (EKS). It includes image scanning tools like Trivy and Docker Scout, and SonarQube for code quality.

---

## 📋 Prerequisites

- AWS EC2 Instance:
  - **Name:** `HR-Leave-App`
  - **OS:** Ubuntu
  - **Instance Type:** `t2.medium` or `t2.large`
  - **Storage:** 30GB
  - **Key Pair:** `vikas-key`

---

## 🔐 SSH into Your Server

```bash
ssh -i vikas-key.pem ubuntu@<your-ec2-ip>
⚙️ Step 1: Install Jenkins
Create and run a script:

bash
Copy
Edit
vi jenkins.sh
Paste the following:

bash
Copy
Edit
#!/bin/bash
sudo apt update
sudo apt install openjdk-17-jre-headless -y
sudo wget -O /usr/share/keyrings/jenkins-keyring.asc https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key
echo deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] https://pkg.jenkins.io/debian-stable binary/ | sudo tee /etc/apt/sources.list.d/jenkins.list > /dev/null
sudo apt-get update
sudo apt-get install jenkins -y
Start Jenkins and check status:

bash
Copy
Edit
sudo systemctl start jenkins
sudo systemctl enable jenkins
systemctl status jenkins
Access Jenkins:

cpp
Copy
Edit
http://<your-ec2-ip>:8080
🐳 Step 2: Install Docker
bash
Copy
Edit
#!/bin/bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo tee /etc/apt/keyrings/docker.asc > /dev/null
sudo chmod a+r /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
Post-install steps:

bash
Copy
Edit
docker --version
sudo usermod -aG docker $USER
cat /etc/group | grep docker
sudo chmod 666 /var/run/docker.sock
🧱 Step 3: Install Docker Compose
bash
Copy
Edit
sudo curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
docker-compose --version
🔐 Step 4: Docker Hub Login
bash
Copy
Edit
docker login -u ghandgevikas
Enter your Docker Hub password.

🛡 Step 5: Install Trivy (Vulnerability Scanner)
bash
Copy
Edit
vi trivy.sh
Paste:

bash
Copy
Edit
#!/bin/bash
sudo apt-get install wget apt-transport-https gnupg
wget -qO - https://aquasecurity.github.io/trivy-repo/deb/public.key | gpg --dearmor | sudo tee /usr/share/keyrings/trivy.gpg > /dev/null
echo "deb [signed-by=/usr/share/keyrings/trivy.gpg] https://aquasecurity.github.io/trivy-repo/deb generic main" | sudo tee -a /etc/apt/sources.list.d/trivy.list
sudo apt-get update
sudo apt-get install trivy
🕵️ Step 6: Install Docker Scout
bash
Copy
Edit
curl -sSfL https://raw.githubusercontent.com/docker/scout-cli/main/install.sh | sh
✅ Step 7: Run SonarQube Container
bash
Copy
Edit
docker run -d --name sonar -p 9000:9000 sonarqube:lts-community
docker ps
🔌 Step 8: Install Jenkins Plugins
Go to Jenkins:

Dashboard > Manage Jenkins > Plugins > Available Plugins

Install:

SonarQube Scanner

Docker, Docker Commons, Docker Pipeline, Docker API, docker-build-step

Pipeline Stage View

Email Extension Template

Kubernetes-related plugins

Prometheus metrics

BlueOcean

Eclipse Temurin Installer

OWASP Dependency Check

AWS CLI

🧰 Step 9: Configure Jenkins Tools
Dashboard > Manage Jenkins > Global Tool Configuration

JDK: jdk17, install from adoptium.net

Maven: maven3, install latest

Docker: docker, install automatically

🔑 Step 10: Add Jenkins Credentials
Dashboard > Manage Jenkins > Credentials > Global > Add Credentials

Kind: Username & Password

Username: ghandgevikas

Password: ******

ID: docker-creds

Description: docker-creds

🧪 Step 11: Add Jenkins Pipeline
groovy
Copy
Edit
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
        stage('Git Clone') {
            steps {
                git branch: 'main', url: 'https://github.com/Vikasghandge/HR-Leave_App.git'
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
                    dos2unix wait-for-it.sh || sed -i 's/\\r$//' wait-for-it.sh
                    chmod +x wait-for-it.sh
                    '''
                }
            }
        }

        stage('Create Docker Image') {
            steps {
                dir("${APP_DIR}") {
                    sh "docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} ."
                    sh "docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${DOCKER_IMAGE}:latest"
                }
            }
        }

        stage('Push Docker Image') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker-creds', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    sh '''
                    echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                    docker push ${DOCKER_IMAGE}:${DOCKER_TAG}
                    docker push ${DOCKER_IMAGE}:latest
                    '''
                }
            }
        }

        stage('Deploy with Docker Compose') {
            steps {
                dir("${APP_DIR}") {
                    sh "docker compose down || true"
                    sh "DOCKER_TAG=${DOCKER_TAG} docker compose pull || true"
                    sh "DOCKER_TAG=${DOCKER_TAG} docker compose up -d"
                }
            }
        }
    }
}
Visit:

cpp
Copy
Edit
http://<your-server-ip>:8090
☸️ Step 12: Kubernetes (EKS) Setup
👥 Create IAM User: HR-user
Attach these policies:

AmazonEC2FullAccess

IAMFullAccess

CloudFormationFullAccess

AdministratorAccess

AmazonEKS_CNI_Policy

AmazonEKSWorkerNodePolicy

AmazonEKSClusterPolicy

Add inline policy:

json
Copy
Edit
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "VisualEditor",
      "Effect": "Allow",
      "Action": "eks:*",
      "Resource": "*"
    }
  ]
}
Generate and store Access Key & Secret Key.

🔐 Add AWS Credentials in Jenkins
Manage Jenkins > Credentials > Global > Add Credentials

Kind: AWS Credentials

Access Key ID / Secret Key

ID: aws-eks-creds

Description: aws-eks-creds

☁️ Install AWS CLI & Kubectl
bash
Copy
Edit
sudo apt install unzip
curl -o kubectl https://amazon-eks.s3.us-west-2.amazonaws.com/1.19.6/2021-01-05/bin/linux/amd64/kubectl
chmod +x ./kubectl
sudo mv ./kubectl /usr/local/bin
kubectl version --short --client
Configure AWS:

bash
Copy
Edit
aws configure
# Enter access key, secret, and region (ap-south-1)
aws sts get-caller-identity
📦 Setup kubectl.sh Script
bash
Copy
Edit
vi kubectl.sh
bash
Copy
Edit
#!/bin/bash
curl -o kubectl https://amazon-eks.s3.us-west-2.amazonaws.com/1.19.6/2021-01-05/bin/linux/amd64/kubectl
chmod +x ./kubectl
sudo mv ./kubectl /usr/local/bin
kubectl version --short --client
🏗 Setup eksctl.sh Script
bash
Copy
Edit
vi eksctl.sh
bash
Copy
Edit
#!/bin/bash
curl --silent --location "https://github.com/weaveworks/eksctl/releases/latest/download/eksctl_$(uname -s)_amd64.tar.gz" | tar xz -C /tmp
sudo mv /tmp/eksctl /usr/local/bin
eksctl version
✅ Next Steps: Set up eksctl EKS Cluster and integrate Kubernetes deployment in your Jenkins pipeline.

Author: Vikas Ghandge
Repo: HR-Leave_App
