#!/bin/bash
# CloudSystem Node Installer - Linux/Mac

echo "=== CloudSystem Node Installer ==="

# 1. Verificar Java
if ! command -v java &> /dev/null; then
    echo "A instalar Java..."
    if command -v apt-get &> /dev/null; then
        apt-get update && apt-get install -y default-jre
    elif command -v yum &> /dev/null; then
        yum install -y java-17-openjdk
    elif command -v brew &> /dev/null; then
        brew install openjdk@17
    else
        echo "Erro: Não foi possível instalar Java automaticamente."
        echo "Instale Java 17 manualmente e volte a executar este script."
        exit 1
    fi
fi

# 2. Baixar .jar
echo "A baixar o nó..."
SERVER_URL=${SERVER_URL:-http://localhost:5000}
wget -q "$SERVER_URL/nodes/jar" -O cloudsystem-node.jar
if [ $? -ne 0 ]; then
    echo "Erro: Falhou o download do .jar."
    exit 1
fi

# 3. Configurar
read -p "IP do servidor [localhost]: " SERVER_IP
SERVER_IP=${SERVER_IP:-localhost}
read -p "Porta do nó [8081]: " NODE_PORT
NODE_PORT=${NODE_PORT:-8081}
read -p "Quota em GB [100]: " QUOTA_GB
QUOTA_GB=${QUOTA_GB:-100}

cat > node.properties << PROPERTIES
cloud.node.server-url=http://$SERVER_IP:5000
cloud.node.server-port=$NODE_PORT
cloud.node.quota-bytes=$(($QUOTA_GB * 1024 * 1024 * 1024))
cloud.node.auto-register=true
PROPERTIES

# 4. Iniciar
echo "A iniciar o nó..."
java -jar cloudsystem-node.jar --spring.config.location=node.properties
