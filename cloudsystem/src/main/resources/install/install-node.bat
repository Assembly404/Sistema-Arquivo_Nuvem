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
REM ============================================================

setlocal EnableDelayedExpansion

if "%SERVER_URL%"=="" set SERVER_URL=https://cloudsystem-backend.onrender.com
if "%NODE_PORT%"=="" set NODE_PORT=8081
if "%QUOTA_GB%"=="" set QUOTA_GB=100
if "%STORAGE_PATH%"=="" set STORAGE_PATH=%USERPROFILE%\cloudstorage

set PAIRING_CODE=%~1

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
echo [2/4] A preparar agente...
if not exist "cloudsystem-node.jar" (
    echo A descarregar o agente do servidor...
    powershell -Command "try { Invoke-WebRequest -Uri '%SERVER_URL%/nodes/jar' -OutFile 'cloudsystem-node.jar' } catch { Write-Host 'ERRO: Falha ao descarregar de %SERVER_URL%'; exit 1 }"
    if not exist "cloudsystem-node.jar" (
        echo [ERRO] Falha ao descarregar o agente.
        pause
        exit /b 1
    )
)
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
) > node.properties
echo Configuracao guardada em node.properties

REM --- Iniciar ---
echo.
echo [4/4] A iniciar o agente...
echo.
java -jar cloudsystem-node.jar --node.agent=true

pause
