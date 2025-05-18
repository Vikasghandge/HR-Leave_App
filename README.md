# 🧑‍💼 HR Leave Management App - CI/CD & K8s Deployment


### 🚀 Project Overview


This project sets up a complete CI/CD pipeline using Jenkins and Docker, with deployment via Docker Compose and Kubernetes (EKS). It includes image scanning tools like Trivy and Docker Scout, and SonarQube for code quality.


## #📋 Prerequisites


AWS EC2 Instance:
Name: HR-Leave-App
OS: Ubuntu
Instance Type: t2.medium or t2.large
Storage: 30GB
Key Pair: vikas-key


ssh into your server -
first of all install jenkins on your ubuntu instance 
vi jenkins.sh

```
#!/bin/bash

# Install OpenJDK 17 JRE Headless
sudo apt install openjdk-17-jre-headless -y

# Download Jenkins GPG key
sudo wget -O /usr/share/keyrings/jenkins-keyring.asc \
  https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key

# Add Jenkins repository to package manager sources
echo deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] \
  https://pkg.jenkins.io/debian-stable binary/ | sudo tee \
  /etc/apt/sources.list.d/jenkins.list > /dev/null

# Update package manager repositories
sudo apt-get update

# Install Jenkins
sudo apt-get install jenkins -y

```
check status of jenkins 
```
systemctl status jenkins
```

then login into your jenkins server 
```
http://<your server ip>:8080
```

now we need to install docker.

```
#!/bin/bash

# Update package manager repositories
sudo apt-get update

# Install necessary dependencies
sudo apt-get install -y ca-certificates curl

# Create directory for Docker GPG key
sudo install -m 0755 -d /etc/apt/keyrings

# Download Docker's GPG key
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc

# Ensure proper permissions for the key
sudo chmod a+r /etc/apt/keyrings/docker.asc

# Add Docker repository to Apt sources
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
$(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Update package manager repositories
sudo apt-get update

sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

```

check docker status 
```
docker --version
```

add current user into the docker group 
```
usermod -aG docker $USER
```

verify user added or not 
```
cat /etc/group | grep docker
```

give permisions to the docker sock
```
chmod 666 /var/run/docker.sock
```

now we need to install docker compose.
use following command to install docker compose

```
sudo curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 -o /usr/local/bin/docker-compose
```
give permissios to docker-compose 
```
chmod +x /usr/local/bin/docker-compose
```

check version of docker compose.
```
docker-compose --version
```
now we need to login into your docker hub repo to store our images inside to docker hub.

```
docker login -u ghandgevikas
```
then add password of dockerhub.

now we to install trivy as image and files scanner to check vaulnarbalities.

vi trivy.sh
```
#!/bin/bash
sudo apt-get install wget apt-transport-https gnupg
wget -qO - https://aquasecurity.github.io/trivy-repo/deb/public.key | gpg --dearmor | sudo tee /usr/share/keyrings/trivy.gpg > /dev/null
echo "deb [signed-by=/usr/share/keyrings/trivy.gpg] https://aquasecurity.github.io/trivy-repo/deb generic main" | sudo tee -a /etc/apt/sources.list.d/trivy.list
sudo apt-get update
sudo apt-get install trivy

```

then install docker scout it is also security checker tool.
run below give command to install docker scout.

```
curl -sSfL https://raw.githubusercontent.com/docker/scout-cli/main/install.sh 
```
now we have to run docker container of sonarqube this source code quality checker tool checks the insecurity, bugs, duplications inside the code.

run container useing following docker command.
```
docker run -d --name sonar -p 9000:9000 sonarqube:lts-community
```
check contanier running or not using command.
```
docker ps
```

now we have setup tools now we need to setup jenkins plugins.
login inside jenkins go to dashboard --> manage jenkins --> plugins --> available plugins 
downloade below plugins.

SonarQube scanner, Docker, Docker Commons, Docker Pipeline, Docker API, docker-build-step, Pipeline stage view, Email Extension Template, Kubernetes, Kubernetes CLI, Kubernetes Client API, Kubernetes Credentials, Kubernetes Credentials Provider, Config File Provider, Prometheus metrics,  BlueOcean,  Eclipse Temurin Installer, Owasp Dependency Check. aws cli.




now we need to setup tools 
go to dashbord  --> manage jenkins --> tools 
add tool jdk name: jdk17 -- install automatically Install from adoptium.net  version: jdk-17.0.11+9
add tool maven  name: maven3 -- install automatically - choose latest
add tool docker name: docker -- install automativally --from dcoker.com
now tools setups is done but in this setup we have not added sonarqube i will update it later.

now we need to add docker credentials into the jenkins.
dashboard --> manage jenkins --> security --> credentilas --> global --> add credentilas.
dcoker-hub username and password and also add ID
docker username -- ghandgevikas 
passoword -- *****
ID=docker-creds  description=docker-creds   add creds and save it.

now till the setps we have done our steup now we need to deploy our application.

go to the jenkins dashbord new item add your pipeline below
```
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

```

now we have deployed our application using docker and docker-compose check 

```
http://<ip addreess server>:8090
```

## k8s part starts ##

first we need to create one IAM user
go and create IAM user name: HR-user
attach policy to the user.  
ec2-full-access, IAM-full-access, cloudformation-full-access, administration access, AmazonEKS_CNI_Policy,
AmazoneEKSWorkerNodePolicy, amazoneEKSClusterPolicy
add this above give policies to the user & create user click on user add permmsions inline policy
add this inline policy 
```
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
```
give any name like custo-leave-policy and attach that policy to the user now we have to create credentilas of our user HR-user create secrete key and secrete accesskey save it very safe place.

go to the jenkins manage jenkins  --> security --> credeintlas --> global --> aws credentilas  -->
add your access key and secrete access key 
ID= aws-eks-creds
Description: aws-eks-creds
save it.

install aws cli
---
```
apt install unzip
```
```
curl -o kubectl https://amazon-eks.s3.us-west-2.amazonaws.com/1.19.6/2021-01-05/bin/linux/amd64/kubectl
chmod +x ./kubectl
sudo mv ./kubectl /usr/local/bin
kubectl version --short --client

```
now we need to configure aws 
```
aws configure
```
add access key of HR-user
add secret access key of HR-user
add region = ap-south-1

command to check credentilas
```
aws sts get-caller identity
```

now lets create eks cluster.
we are here using cloudformation here for create cluster
first we need to steup kubectl.
vi kubectl.sh
```
#!/bin/bash
curl -o kubectl https://amazon-eks.s3.us-west-2.amazonaws.com/1.19.6/2021-01-05/bin/linux/amd64/kubectl
chmod +x ./kubectl
sudo mv ./kubectl /usr/local/bin
kubectl version --short --client
```

then we need to setup  eks-ctl
vi eks-ctl.sh
```
#!/bi/bash
curl --silent --location "https://github.com/weaveworks/eksctl/releases/latest/download/eksctl_$(uname -s)_amd64.tar.gz" | tar xz -C /tmp
sudo mv /tmp/eksctl /usr/local/bin
eksctl version

```

now need to setup eks cluster.
```
eksctl create cluster --name=my-eks \
                      --region=ap-south-1 \
                      --zones=ap-south-1a,ap-south-1b \
                      --version=1.30 \
                      --without-nodegroup
```
once cluster got created then add next command.

```
eksctl utils associate-iam-oidc-provider \
    --region ap-south-1 \
    --cluster my-eks \
    --approve
```

then create node groups according to your need if you want to customize you can do add your cluster-name then add region where u want to create cluster
select instance type add nodes according to you make sure to add ssh public key example= "vikas-key".
```
eksctl create nodegroup --cluster=my-eks \             
                       --region=ap-south-1 \
                       --name=node2 \
                       --node-type=t3.medium \
                       --nodes=3 \
                       --nodes-min=2 \
                       --nodes-max=4 \
                       --node-volume-size=20 \
                       --ssh-access \
                       --ssh-public-key=vikas-key \
                       --managed \
                       --asg-access \
                       --external-dns-access \
                       --full-ecr-access \
                       --appmesh-access \
                       --alb-ingress-access
```
 once your node got created go to aws console go to eks then select cluster you cluster go to networking check additional security groups 
 add - all traffic allow - to interact worknode to master node they can communicate with each other.

 now done go to your ubuntu server run some commands. to check available storage clasess.
```
kubectl get storageclass
```
to check ebs-csi driver we need to run below command currently we dont have any ebs-csi driver.
```
kubectl get pods -n kube-system | grep ebs-csi
```

### now we need to install EBS-CSI driver (if missing)
create iam policy for worker nodes 
downloade policy json.
```
curl -o example-iam-policy.json https://raw.githubusercontent.com/kubernetes-sigs/aws-ebs-csi-driver/master/docs/example-iam-policy.json
```

create iam policy to grants permissions to create/delete/modify ebs volumes 
- applied at the service account level (not node level only service account level) for security
```
aws iam create-policy \
--policy-name AmazonEKS_EBS_CSI_Driver_Policy \
--policy-document file://example-iam-policy.json  
```

above given command will generate one arn copy it example ##  arn:aws:iam::543157869024:policy/AmazonEKS_EBS_CSI_Driver_Policy ## like this copy it from prompt.


create iam role for service account.
set your cluste name.

```
EKS_CLUSTER_NAME="my-eks"        # ✅ Sets the EKS cluster name
AWS_REGION="ap-south-1"          # ✅ Sets the AWS region to Mumbai
ACCOUNT_ID=$(aws sts get-caller-identity --query "Account" --output text)  # ✅ Retrieves your AWS account ID
```

create iam oidc provider 
```
eksctl utils associate-iam-oidc-provider \
    --cluster ${EKS_CLUSTER_NAME} \
    --approve

```

now crate service account with IAM role.

```
eksctl create iamserviceaccount \
--name ebs-csi-controller-sa \
--namespace kube-system \
--cluster ${EKS_CLUSTER_NAME} \
--attach-policy-arn arn:aws:iam::543157869024:policy/AmazonEKS_EBS_CSI_Driver_Policy \
--approve \
--override-existing-serviceaccounts
```
now above command be careful bcz here you need to past that arn token which you have created above.

example in your commadn paste token like this way

--attach-policy-arn arn:aws:iam::543157869024:policy/AmazonEKS_EBS_CSI_Driver_Policy \

--attach-policy-arn -arn token- /AmazonEKS_EBS_CSI_Driver_Policy \

now we have sucessfully setup our service account

now we need to setup or install helm chat
run below command to install helm.
```
sudo snap install helm --classic
```
```
helm version
```

add aws-ebs-csi-driver helm repo.
add repo
```
helm repo add aws-ebs-csi-driver https://kubernetes-sigs.github.io/aws-ebs-csi-driver
```
update repo
```
helm repo update
```

install ebs-csi driver
```
helm upgrade --install aws-ebs-csi-driver \
--namespace kube-system \
--set controller.serviceAccount.create=false \
--set controller.serviceAccount.name=ebs-csi-controller-sa \
aws-ebs-csi-driver/aws-ebs-csi-driver
```

after running above use below command.
```
kubectl get pods -n kube-system | grep ebs-csi
```

then output should be like this.

output ..
ebs-csi-controller-6cfcb8b5b-56lzk  5/5     Running   0           86s
ebs-csi-controller-6cfcb8b5b-6s98z   5/5     Running   0          86s
ebs-csi-node-h6wpr                   3/3     Running   0          86s
ebs-csi-node-wdtz5                   3/3     Running   0          86s
ebs-csi-node-xgshv


now sucessfully we have done our work now we need to deploy pipeline.
add below given pipeline in jenkins dashbord -- new inte -- then paste give pipeline.
```
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
        EKS_CLUSTER = "my-eks"
        AWS_REGION = "ap-south-1"
        K8S_NAMESPACE = "leave-management"
        MYSQL_WAIT_TIMEOUT = "300"
        APP_WAIT_TIMEOUT = "300"
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
                    sh """
                    echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                    docker push ${DOCKER_IMAGE}:${DOCKER_TAG}
                    docker push ${DOCKER_IMAGE}:latest
                    """
                }
            }
        }

        /*
        stage('Deploy with Docker Compose') {
            steps {
                dir("${APP_DIR}") {
                    sh "docker compose down || true"
                    sh "DOCKER_TAG=${DOCKER_TAG} docker compose pull || true"
                    sh "DOCKER_TAG=${DOCKER_TAG} docker compose up -d"
                }
            }
        }
        */

        stage('Configure AWS EKS Access') {
            steps {
                withCredentials([[
                    $class: 'AmazonWebServicesCredentialsBinding',
                    credentialsId: 'aws-eks-creds',
                    accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                    secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'
                ]]) {
                    sh """
                        aws configure set aws_access_key_id ${AWS_ACCESS_KEY_ID}
                        aws configure set aws_secret_access_key ${AWS_SECRET_ACCESS_KEY}
                        aws configure set region ${AWS_REGION}
                        aws eks --region ${AWS_REGION} update-kubeconfig --name ${EKS_CLUSTER}
                        kubectl create namespace ${K8S_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -
                        kubectl config set-context --current --namespace=${K8S_NAMESPACE}
                        kubectl cluster-info
                        kubectl get nodes
                    """
                }
            }
        }

        stage('Deploy MySQL') {
            steps {
                dir('leave-app-kastro-master/k8s') {
                    script {
                        sh """
                            kubectl apply -f ebs-sc.yml -n ${K8S_NAMESPACE}
                            kubectl apply -f mysql-secret.yml -n ${K8S_NAMESPACE}
                            kubectl apply -f mysql-pvc.yml -n ${K8S_NAMESPACE}
                            kubectl apply -f mysql-deployment.yml -n ${K8S_NAMESPACE}
                            kubectl apply -f mysql-service.yml -n ${K8S_NAMESPACE}
                        """

                        sh """
                            kubectl patch deployment mysql -n ${K8S_NAMESPACE} --type=json \
                                -p='[{"op": "remove", "path": "/spec/template/spec/containers/0/env/2"}]' || true
                            kubectl patch deployment mysql -n ${K8S_NAMESPACE} --type=json \
                                -p='[{"op": "remove", "path": "/spec/template/spec/containers/0/env/3"}]' || true
                        """

                        timeout(time: 10, unit: 'MINUTES') {
                            sh """
                                kubectl rollout status deployment/mysql -n ${K8S_NAMESPACE}
                                kubectl wait --for=condition=Ready pod -l app=mysql --timeout=600s -n ${K8S_NAMESPACE}
                            """
                        }
                    }
                }
            }
        }

        stage('Deploy Application') {
            steps {
                dir('leave-app-kastro-master/k8s') {
                    script {
                        sh """
                            sed -i "s|image:.*|image: ${DOCKER_IMAGE}:${DOCKER_TAG}|g" deployment.yml
                            kubectl apply -f deployment.yml -n ${K8S_NAMESPACE}
                            kubectl apply -f service.yml -n ${K8S_NAMESPACE}
                        """
                    }
                }

                script {
                    timeout(time: 5, unit: 'MINUTES') {
                        sh """
                            kubectl rollout status deployment/leave-management-app --timeout=${APP_WAIT_TIMEOUT}s -n ${K8S_NAMESPACE}
                            for i in {1..10}; do
                                kubectl get pods -l app=leave-management -n ${K8S_NAMESPACE} | grep -q 'Running' && break || sleep 10
                            done
                        """
                    }
                }
            }
        }

        stage('Verify Deployment') {
            steps {
                script {
                    def appUrl = sh(returnStdout: true, script: """
                        kubectl get svc leave-management-service -n ${K8S_NAMESPACE} -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'
                    """).trim()
                    echo "##[success] Application deployed: http://${appUrl}:8090"
                }
            }
        }
    }

    post {
        always {
            sh 'docker system prune -f'
        }
        failure {
            sh """
                kubectl get pods -n ${K8S_NAMESPACE} > k8s-logs/failure.log
                kubectl logs -l app=mysql -n ${K8S_NAMESPACE} --tail=50 >> k8s-logs/failure.log
            """
        }
    }
}

```

### error trouble shooting
1) pipeline is not able to find you docker file or pox.xml file in pipeline add correct path
2) example my docker file is in "leave-app-kastro-master" you need to mention it
3) then pom.xml check it should be first folder it should be this folder "leave-app-kastro-master/k8s"
4) very verify credeintilas and id your aws credentilas should be match with pipeline
5) example docker hub credentilas ID "docker-creds" it should match with pipeline
6) more maven, sonarQube, owasp, aws credeintilas.
7) tools configure make sure you have configured all required tools.

## steps to access our application using loadbalancer.
run some below commands.
first check your namespace.
```
kubectl get namespace
```
to access pods in you name space
```
kubectl get pods -n leave-management
or
kubectl get pods -A
```

```
kubectl get deployment -n leave-management
or
kubectl get deployment -A
```

## to access loadbalancer link
```
kubectl get svc -n leave-management
or
kubectl get svc -A
```
copy your load balancer and paste as it is in google and access application.
![Alt text](images/Screenshot%2025-05-17%025503.png)

## trouble shooting commnads
```
kubectl describe node <node-name>   # node level issue.
```

```
kubect describe get pod -n leave-management  # pod level issue
```
you can also go for 
check resources 
```  
kubectl top nodes   # resorces use node level
or
kubectl top pods   # use pod level
```

 Cluster & Node Health
```
kubectl cluster-info                # View cluster info
kubectl get nodes                  # List all nodes
kubectl describe node <node-name> # Node details (CPU, memory, pods)
kubectl top nodes                 # View node resource usage

```

 Pod Troubleshooting
 ```
kubectl get pods -A                          # List all pods in all namespaces
kubectl describe pod <pod-name>              # Details of pod (events, status, etc.)
kubectl logs <pod-name>                      # View logs of pod (single container)
kubectl logs <pod-name> -c <container-name>  # Logs for multi-container pod
kubectl exec -it <pod-name> -- /bin/sh       # Shell into pod (or use /bin/bash)
kubectl top pod <pod-name>                   # Pod resource usage

```

Deployment & ReplicaSet
```
kubectl get deployments                     # List deployments
kubectl describe deployment <deployment>    # Detailed info & events
kubectl rollout status deployment/<name>    # Rollout status
kubectl rollout history deployment/<name>   # Check previous versions
kubectl rollout undo deployment/<name>      # Rollback to previous version

```

CrashLoopBackOff / Failed Pods
```
kubectl get pods -n <namespace>                              # See pod status
kubectl describe pod <pod-name> -n <namespace>               # Check events
kubectl logs <pod-name> --previous -n <namespace>            # Get logs from previous failed pod
kubectl delete pod <pod-name> -n <namespace>                 # Restart a stuck pod manually

```

Service & Networking
```
kubectl get svc -A                             # List all services
kubectl describe svc <service-name>            # Details of a service
kubectl get endpoints                          # Check endpoint mappings
kubectl get ingress -A                         # Check ingress resources
kubectl describe ingress <ingress-name>        # Debug ingress

```

volume and pv and pvc
```
kubectl get pvc -A                             # List all Persistent Volume Claims
kubectl describe pvc <pvc-name>                # Details of a PVC
kubectl get pv                                 # List Persistent Volumes

```

config and secrets
  ```
kubectl get configmap -A
kubectl describe configmap <name>

kubectl get secret -A
kubectl describe secret <name>

```

```
kubectl delete pod <pod-name>                 # Delete a pod
kubectl delete deployment <name>              # Delete deployment
kubectl delete svc <svc-name>                 # Delete service

```










        










.








