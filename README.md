# HR-Leave_App
# prequsite
lanuch ec2 instance = name: HR-Leave-App,  os=ubuntu, instance_type=t2.medium or t2.large stoarge=30GB key=vikas-key

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
--attach-policy-arn <past as it is your arn here like above >/AmazonEKS_EBS_CSI_Driver_Policy \

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
--------------------------------------------------------------------------------------------------------------------------------------------------------------------
output -- 
ebs-csi-controller-6cfcb8b5b-56lzk  5/5     Running   0           86s
ebs-csi-controller-6cfcb8b5b-6s98z   5/5     Running   0          86s
ebs-csi-node-h6wpr                   3/3     Running   0          86s
ebs-csi-node-wdtz5                   3/3     Running   0          86s
ebs-csi-node-xgshv
--------------------------------------------------------------------------------------------------------------------------------------------------------------------

now sucessfully we have done our work now we need to deploy pipeline.
add below given pipeline in jenkins dashbord -- new inte -- then paste give pipeline.
```

```










        










.








