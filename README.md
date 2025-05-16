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

SonarQube scanner, Docker, Docker Commons, Docker Pipeline, Docker API, docker-build-step, Pipeline stage view, Email Extension Template, Kubernetes, Kubernetes CLI, Kubernetes Client API, Kubernetes Credentials, Kubernetes Credentials Provider, Config File Provider, Prometheus metrics,  BlueOcean,  Eclipse Temurin Installer, Owasp Dependency Check.

















