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
#     --public-url=URL         endereço pelo qual o servidor alcança este PC
#                              (túnel cloudflared/ngrok). Necessário atrás de NAT.
# ============================================================

set -e

# --- Cores ---
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# --- Defaults ---
SERVER_URL="${SERVER_URL:-https://debesis.onrender.com}"
NODE_PORT="${NODE_PORT:-8081}"
QUOTA_GB="${QUOTA_GB:-100}"
STORAGE_PATH="${STORAGE_PATH:-$HOME/cloudstorage}"
PAIRING_CODE=""
PUBLIC_URL="${PUBLIC_URL:-}"

# --- Processar argumentos ---
FIRST_ARG="${1:-}"
for arg in "$@"; do
    case "$arg" in
        --path=*)     STORAGE_PATH="${arg#*=}" ;;
        --quota=*)    QUOTA_GB="${arg#*=}" ;;
        --server=*)   SERVER_URL="${arg#*=}" ;;
        --port=*)     NODE_PORT="${arg#*=}" ;;
        --public-url=*) PUBLIC_URL="${arg#*=}" ;;
        -h|--help)
            echo "Uso: bash install-node.sh CODIGO-DE-PAREAMENTO [opções]"
            echo ""
            echo "O código de pareamento é obtido no site: Definições → Dispositivos → Adicionar"
            echo ""
            echo "Opções:"
            echo "  --path=/pasta    Onde guardar os ficheiros (default: ~/cloudstorage)"
            echo "  --quota=GB       Espaço a partilhar em GB (default: 100)"
            echo "  --server=URL     URL do servidor (default: $SERVER_URL)"
            echo "  --public-url=URL Endereço pelo qual o servidor alcança este PC"
            echo "                   (túnel cloudflared/ngrok). Obrigatório se estiveres atrás de NAT."
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
echo -e "${YELLOW}[2/4]${NC} A preparar agente (o download tem ~62 MB, pode demorar)..."
NODE_JAR="cloudsystem-node.jar"
if [ -f "$NODE_JAR" ]; then
    echo -e "${GREEN}✓${NC} Agente já existe localmente, a reutilizar ($NODE_JAR)"
else
    echo "A descarregar o agente de: $SERVER_URL/nodes/jar"
    DL_START=$(date +%s)
    # Nota: mostramos a barra de progresso (sem -s / -q) para que o utilizador
    # saiba que o passo [2/4] está a avançar e não "pendurado".
    if command -v curl &> /dev/null; then
        curl -fL --progress-bar "$SERVER_URL/nodes/jar" -o "$NODE_JAR" || {
            echo -e "${RED}Erro:${NC} Falha ao descarregar. Verifica a URL: $SERVER_URL"
            exit 1
        }
    elif command -v wget &> /dev/null; then
        wget --show-progress --progress=bar:force:noscroll "$SERVER_URL/nodes/jar" -O "$NODE_JAR" || {
            echo -e "${RED}Erro:${NC} Falha ao descarregar. Verifica a URL: $SERVER_URL"
            exit 1
        }
    else
        echo -e "${RED}Erro:${NC} Precisas de curl ou wget instalado."
        exit 1
    fi
    DL_SECS=$(( $(date +%s) - DL_START ))
    echo -e "${GREEN}✓${NC} Agente descarregado em ${DL_SECS}s ($(du -h "$NODE_JAR" 2>/dev/null | cut -f1))"
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
cloud.node.public-url=$PUBLIC_URL
PROPS
echo "Configuração guardada em node.properties"

# --- Aviso de alcance (NAT) ---
# Se o servidor está na nuvem e este PC está atrás de um router/NAT, o servidor
# não consegue abrir ligação a este dispositivo. Sem um túnel (public-url), o
# dispositivo aparece como ligado mas as operações de armazenamento falham.
if [ -z "$PUBLIC_URL" ]; then
    echo ""
    echo -e "${YELLOW}Aviso:${NC} cloud.node.public-url não foi definido."
    echo "Se o servidor CloudSystem estiver na Internet e este computador"
    echo "estiver atrás de um router (NAT), o servidor NÃO conseguirá"
    echo "aceder ao armazenamento deste dispositivo."
    echo ""
    echo "Solução: expõe o agente com um túnel, por exemplo:"
    echo "  cloudflared tunnel --url http://localhost:$NODE_PORT"
    echo "e volta a correr:"
    echo "  bash install-node.sh $PAIRING_CODE --public-url=https://O-TEU-TUNEL.trycloudflare.com"
    echo ""
fi

# --- Iniciar agente ---
echo ""
echo -e "${YELLOW}[4/4]${NC} A iniciar o agente (Ctrl+C para parar)..."
echo "Endpoint local: http://localhost:$NODE_PORT/nodes/health"
echo ""
java -jar "$NODE_JAR" --node.agent=true
