#!/bin/bash
# ============================================================
#  CloudSystem Node Installer GUI Launcher
# ============================================================
#  Lança o instalador gráfico JavaFX do nó de armazenamento.
#
#  Uso:
#     ./run-installer-gui.sh
#
#  Requisitos:
#     - Java 17+ (o script tenta instalar automaticamente se não tiver)
# ============================================================

set -e

JAR_NAME="cloudsystem-0.0.1-SNAPSHOT.jar"
JAR_PATH="target/$JAR_NAME"

# Cores
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${YELLOW}CloudSystem Node Installer GUI${NC}"
echo "================================"

# Verificar se o JAR existe
if [ ! -f "$JAR_PATH" ]; then
    echo -e "${YELLOW}JAR não encontrado. A compilar...${NC}"
    ./mvnw package -DskipTests -q
fi

# Verificar Java
if ! command -v java &> /dev/null; then
    echo -e "${YELLOW}Java não encontrado. A instalar Java 17...${NC}"
    if command -v apt-get &> /dev/null; then
        sudo apt-get update && sudo apt-get install -y openjdk-17-jre
    elif command -v dnf &> /dev/null; then
        sudo dnf install -y java-17-openjdk
    elif command -v yum &> /dev/null; then
        sudo yum install -y java-17-openjdk
    elif command -v brew &> /dev/null; then
        brew install openjdk@17
    else
        echo -e "${RED}Erro:${NC} Instale Java 17 manualmente em https://adoptium.net"
        exit 1
    fi
fi

JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo -e "${RED}Erro:${NC} Java 17+ necessário (encontrado Java $JAVA_VERSION)"
    exit 1
fi

echo -e "${GREEN}✓${NC} Java $JAVA_VERSION detectado"
echo -e "${GREEN}✓${NC} JAR encontrado: $JAR_PATH"
echo ""
echo "A iniciar instalador gráfico..."
echo ""

# Executar a classe JavaFX principal
java --module-path target/classes:target/dependency \
     --add-modules javafx.controls,javafx.fxml,javafx.web \
     -cp "$JAR_PATH" \
     com.assemblyproject.cloudsystem.agent.NodeInstallerGUI