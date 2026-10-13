@echo off
REM ============================================================
REM  CloudSystem Node Installer  (Windows)
REM ============================================================
REM  Torna o teu computador parte da nuvem de armazenamento.
REM
REM  Modo mais simples:
REM     install-node.bat SEU-CODIGO-DE-PAREAMENTO
REM
REM  Exemplo:
REM     install-node.bat A7K9M2P4
REM
REM  Variaveis opcionais (define antes de correr):
REM     set STORAGE_PATH=C:\cloudstorage
REM     set QUOTA_GB=100
REM     set SERVER_URL=https://cloudsystem-backend.onrender.com
REM     set PUBLIC_URL=https://meu-pc.trycloudflare.com   (tunel, para NAT)
REM ============================================================

setlocal EnableDelayedExpansion

if "%SERVER_URL%"=="" set SERVER_URL=https://debesis.onrender.com
if "%NODE_PORT%"=="" set NODE_PORT=8081
if "%QUOTA_GB%"=="" set QUOTA_GB=100
if "%STORAGE_PATH%"=="" set STORAGE_PATH=%USERPROFILE%\cloudstorage

set PAIRING_CODE=%~1

REM --- Opcao --public-url=URL (tunel para funcionar atras de NAT) ---
set "PUBLIC_URL="
for %%a in (%*) do (
    set "ARG=%%a"
    if "!ARG:~0,13!"=="--public-url=" set "PUBLIC_URL=!ARG:~13!"
)

REM --- Verificar Java ---
echo [1/4] A verificar Java...
where java >nul 2>nul
if errorlevel 1 (
    echo Java nao encontrado.
    echo.
    echo Por favor instala Java 17 em: https://adoptium.net
    echo Escolhe "Eclipse Temurin 17 LTS" para Windows.
    echo Depois de instalar, corre este script novamente.
    pause
    exit /b 1
)
echo [OK] Java detectado

REM --- Pedir codigo de pareamento ---
if "%PAIRING_CODE%"=="" (
    echo.
    echo ==============================================
    echo   Registo de dispositivo CloudSystem
    echo ==============================================
    echo 1. Abre o site do CloudSystem
    echo 2. Vai a: Definicoes - Dispositivos - Adicionar dispositivo
    echo 3. Copia o codigo de pareamento
    echo.
    set /p PAIRING_CODE="Cola aqui o codigo de pareamento: "
    if "!PAIRING_CODE!"=="" (
        echo [ERRO] Codigo de pareamento e obrigatorio.
        pause
        exit /b 1
    )
)

REM --- Descarregar agente ---
echo.
echo [2/4] A preparar agente (download ~62 MB, pode demorar)...
if exist "cloudsystem-node.jar" (
    echo [OK] Agente ja existe localmente, a reutilizar.
    goto :agent_ready
)
echo A descarregar o agente de: %SERVER_URL%/nodes/jar
REM $ProgressPreference acelera muito o Invoke-WebRequest (a barra de progresso
REM torna o download de ficheiros grandes extremamente lento no PowerShell).
powershell -NoProfile -Command "$ProgressPreference='SilentlyContinue'; try { Invoke-WebRequest -Uri '%SERVER_URL%/nodes/jar' -OutFile 'cloudsystem-node.jar' -UseBasicParsing } catch { Write-Host ('ERRO: Falha ao descarregar de %SERVER_URL% -> ' + $_.Exception.Message); exit 1 }"
if not exist "cloudsystem-node.jar" (
    echo [ERRO] Falha ao descarregar o agente.
    pause
    exit /b 1
)
:agent_ready
echo [OK] Agente pronto

REM --- Criar pasta ---
echo.
echo [3/4] A criar pasta de armazenamento...
if not exist "%STORAGE_PATH%" mkdir "%STORAGE_PATH%"
echo [OK] Pasta: %STORAGE_PATH%

REM --- Calcular quota em bytes ---
set /a QUOTA_BYTES=%QUOTA_GB% * 1073741824

REM --- Criar node.properties ---
(
    echo # CloudSystem Node - configuracao local
    echo cloud.node.server-url=%SERVER_URL%
    echo cloud.node.server-port=%NODE_PORT%
    echo cloud.node.storage-dir=%STORAGE_PATH%
    echo cloud.node.quota-bytes=%QUOTA_BYTES%
    echo cloud.node.auto-register=true
    echo cloud.node.pairing-code=%PAIRING_CODE%
    echo cloud.node.public-url=%PUBLIC_URL%
) > node.properties
echo Configuracao guardada em node.properties

REM --- Aviso de alcance (NAT) ---
if "%PUBLIC_URL%"=="" (
    echo.
    echo [AVISO] cloud.node.public-url nao foi definido.
    echo Se o servidor CloudSystem estiver na Internet e este PC estiver
    echo atras de um router (NAT), o servidor NAO conseguira aceder ao
    echo armazenamento deste dispositivo.
    echo.
    echo Solucao: expoe o agente com um tunel, por exemplo:
    echo   cloudflared tunnel --url http://localhost:%NODE_PORT%
    echo e volta a correr:
    echo   install-node.bat %PAIRING_CODE% --public-url=https://O-TEU-TUNEL.trycloudflare.com
    echo.
)

REM --- Iniciar ---
echo.
echo [4/4] A iniciar o agente (Ctrl+C para parar)...
echo Endpoint local: http://localhost:%NODE_PORT%/nodes/health
echo.
java -jar cloudsystem-node.jar --node.agent=true

pause
