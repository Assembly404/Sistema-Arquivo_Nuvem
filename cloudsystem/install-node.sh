#!/bin/bash
# ============================================================
#  CloudSystem Node Installer  (Linux / macOS)
# ============================================================
#  Torna o teu computador parte da nuvem de armazenamento.
#
#  Modo mais simples:
#     bash install-node.sh SEU-CODIGO-DE-PAREAMENTO
#
#  Exemplo:
#     bash install-node.sh A7K9M2P4
#
#  Opções avançadas (todas opcionais):
#     --path=/pasta/local      onde guardar os ficheiros (default: ~/cloudstorage)
#     --quota=GB               quota em GB (default: 100)
#     --server=URL             URL do servidor (default: https://cloudsystem-backend.onrender.com)
# ============================================================

set -e

# --- Cores ---
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# --- Defaults ---
SERVER_URL="${SERVER_URL:-https://cloudsystem-backend.onrender.com}"
NODE_PORT="${NODE_PORT:-8081}"
QUOTA_GB="${QUOTA_GB:-100}"
STORAGE_PATH="${STORAGE_PATH:-$HOME/cloudstorage}"
PAIRING_CODE=""

# --- Processar argumentos ---
FIRST_ARG="${1:-}"
for arg in "$@"; do
    case "$arg" in
        --path=*)     STORAGE_PATH="${arg#*=}" ;;
        --quota=*)    QUOTA_GB="${arg#*=}" ;;
        --server=*)   SERVER_URL="${arg#*=}" ;;
        --port=*)     NODE_PORT="${arg#*=}" ;;
        -h|--help)
            echo "Uso: bash install-node.sh CODIGO-DE-PAREAMENTO [opções]"
            echo ""
            echo "O código de pareamento é obtido no site: Definições → Dispositivos → Adicionar"
            echo ""
            echo "Opções:"
            echo "  --path=/pasta    Onde guardar os ficheiros (default: ~/cloudstorage)"
            echo "  --quota=GB       Espaço a partilhar em GB (default: 100)"
            echo "  --server=URL     URL do servidor (default: $SERVER_URL)"
            exit 0
            ;;
        -*) ;;  # ignora outras flags
        *)  [ -z "$PAIRING_CODE" ] && PAIRING_CODE="$arg" ;;
    esac
done

# --- Verificar Java ---
echo -e "${YELLOW}[1/4]${NC} A verificar Java..."
if ! command -v java &> /dev/null; then
    echo "Java não encontrado. A instalar Java 17..."
    if command -v apt-get &> /dev/null; then
        sudo apt-get update && sudo apt-get install -y openjdk-17-jre
    elif command -v dnf &> /dev/null; then
        sudo dnf install -y java-17-openjdk
    elif command -v yum &> /dev/null; then
        sudo yum install -y java-17-openjdk
    elif command -v brew &> /dev/null; then
        brew install openjdk@17
    else
        echo -e "${RED}Erro:${NC} Instala Java 17 manualmente em https://adoptium.net"
        exit 1
    fi
fi
echo -e "${GREEN}✓${NC} Java detectado"

# --- Pedir código de pareamento se não foi dado ---
if [ -z "$PAIRING_CODE" ]; then
    echo ""
    echo "=============================================="
    echo "  Registo de dispositivo CloudSystem"
    echo "=============================================="
    echo "1. Abre o site do CloudSystem"
    echo "2. Vai a: Definições → Dispositivos → Adicionar dispositivo"
    echo "3. Copia o código de pareamento"
    echo ""
    read -p "Cola aqui o código de pareamento: " PAIRING_CODE
    if [ -z "$PAIRING_CODE" ]; then
        echo -e "${RED}Erro:${NC} Código de pareamento é obrigatório."
        exit 1
    fi
fi

# --- Descarregar agente ---
echo ""
echo -e "${YELLOW}[2/4]${NC} A preparar agente..."
NODE_JAR="cloudsystem-node.jar"
if [ ! -f "$NODE_JAR" ]; then
    echo "A descarregar o agente do servidor..."
    if command -v curl &> /dev/null; then
        curl -fsSL "$SERVER_URL/nodes/jar" -o "$NODE_JAR" || {
            echo -e "${RED}Erro:${NC} Falha ao descarregar. Verifica a URL: $SERVER_URL"
            exit 1
        }
    elif command -v wget &> /dev/null; then
        wget -q "$SERVER_URL/nodes/jar" -O "$NODE_JAR" || {
            echo -e "${RED}Erro:${NC} Falha ao descarregar. Verifica a URL: $SERVER_URL"
            exit 1
        }
    else
        echo -e "${RED}Erro:${NC} Precisas de curl ou wget instalado."
        exit 1
    fi
fi
echo -e "${GREEN}✓${NC} Agente pronto"

# --- Criar pasta de armazenamento ---
echo ""
echo -e "${YELLOW}[3/4]${NC} A criar pasta de armazenamento..."
mkdir -p "$STORAGE_PATH"
echo -e "${GREEN}✓${NC} Pasta: $STORAGE_PATH"

# --- Calcular quota em bytes ---
QUOTA_BYTES=$((QUOTA_GB * 1024 * 1024 * 1024))

# --- Criar ficheiro de configuração ---
cat > node.properties <<PROPS
# CloudSystem Node - configuração local
cloud.node.server-url=$SERVER_URL
cloud.node.server-port=$NODE_PORT
cloud.node.storage-dir=$STORAGE_PATH
cloud.node.quota-bytes=$QUOTA_BYTES
cloud.node.auto-register=true
cloud.node.pairing-code=$PAIRING_CODE
PROPS
echo "Configuração guardada em node.properties"

# --- Iniciar agente ---
echo ""
echo -e "${YELLOW}[4/4]${NC} A iniciar o agente..."
echo ""
java -jar "$NODE_JAR" --node.agent=true
