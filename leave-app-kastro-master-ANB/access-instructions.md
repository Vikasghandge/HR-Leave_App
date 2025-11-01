# Leave Management Application - Access Instructions

## Accessing the Application

### When using Docker Compose

After deploying with Docker Compose, access the application at:

- Employee Portal: `http://<EC2-PUBLIC-IP>:8090/login`
- Admin Dashboard: `http://<EC2-PUBLIC-IP>:8090/login` (Login with HR credentials)

### When using Kubernetes

After deploying to Kubernetes (EKS), access the application at:

- Employee Portal: `http://<EKS-LOADBALANCER-IP>:8090/login`
- Admin Dashboard: `http://<EKS-LOADBALANCER-IP>:8090/login` (Login with HR credentials)

To get the Load Balancer IP:
```bash
kubectl get svc leave-management
```

## Default Login Credentials

The application comes with two pre-configured accounts:

### HR Admin
- Employee ID: `HR001`
- Password: `admin123`

### Test Employee
- Employee ID: `EMP001`
- Password: `emp123`

## Database Access

To access the MySQL database running in Docker:

```bash
docker exec -it leave-management-mysql mysql -u root -p
```
When prompted for password, enter: `password`

To access the MySQL database running in Kubernetes:

```bash
kubectl exec -it $(kubectl get pods -l app=mysql -o name | head -n 1) -- mysql -u root -p
```
When prompted for password, enter: `password`

## Port Information

- Application server runs on port **8090** (not the default 8080 as requested)
- MySQL server runs on port **3306**

## Troubleshooting

If you're unable to access the application:

1. Check if the containers/pods are running:
   ```bash
   # For Docker Compose
   docker ps
   
   # For Kubernetes
   kubectl get pods
   ```

2. Check application logs:
   ```bash
   # For Docker Compose
   docker logs leave-management-app
   
   # For Kubernetes
   kubectl logs -l app=leave-management
   ```

3. Verify the security group on your EC2 instance allows traffic on port 8090