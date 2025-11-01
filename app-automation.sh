#!/bin/bash

# Usage: ./deploy-leave-app.sh <instance_name> <app_port> <db_port>
# Example: ./deploy-leave-app.sh cosmos 8093 3308

INSTANCE=$1
APP_PORT=$2
DB_PORT=$3

if [ -z "$INSTANCE" ] || [ -z "$APP_PORT" ] || [ -z "$DB_PORT" ]; then
  echo "Usage: $0 <instance_name> <app_port> <db_port>"
  exit 1
fi

SOURCE_DIR="/home/ubuntu/HR-Leave_App/leave-app-kastro-master"
TARGET_DIR="/home/ubuntu/HR-Leave_App/leave-app-kastro-master-$INSTANCE"

# Step 1: Copy the project folder
cp -r "$SOURCE_DIR" "$TARGET_DIR"
cd "$TARGET_DIR" || exit

# Step 2: Update docker-compose.yml with new names and ports
sed -i "s/leave-management-app/leave-management-app-$INSTANCE/g" docker-compose.yml
sed -i "s/leave-management-mysql/leave-management-mysql-$INSTANCE/g" docker-compose.yml
sed -i "s/8090:8090/$APP_PORT:8090/g" docker-compose.yml
sed -i "s/3306:3306/$DB_PORT:3306/g" docker-compose.yml

# Step 3: Optional - change database name (for isolation)
sed -i "s/MYSQL_DATABASE=.*/MYSQL_DATABASE=leave_db_$INSTANCE/g" docker-compose.yml

# Step 4: Start containers
docker compose up -d

echo ""
echo "✅ Leave Management instance '$INSTANCE' deployed successfully!"
echo "🌐 Access URL: http://<YOUR_EC2_PUBLIC_IP>:$APP_PORT"
echo "🗄️ MySQL running on port $DB_PORT"

