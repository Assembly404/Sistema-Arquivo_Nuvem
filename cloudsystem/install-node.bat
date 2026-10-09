@echo off
REM CloudSystem Node Installer - Windows

echo === CloudSystem Node Installer ===

REM Verificar Java
java -version 2>NUL
if errorlevel 1 (
    echo A instalar Java...
    winget install Eclipse.Adoptium.JDK17
)

REM Baixar .jar
echo A baixar o nó...
set SERVER_URL=http://localhost:5000
powershell -Command "Invoke-WebRequest -Uri '%SERVER_URL%/nodes/jar' -OutFile cloudsystem-node.jar"
if errorlevel 1 (
    echo Erro: Falhou o download do .jar.
    exit /b 1
)

REM Configurar
set /p SERVER_IP="IP do servidor [localhost]: "
if "%SERVER_IP%"=="" set SERVER_IP=localhost
set /p NODE_PORT="Porta do nó [8081]: "
if "%NODE_PORT%"=="" set NODE_PORT=8081
set /p QUOTA_GB="Quota em GB [100]: "
if "%QUOTA_GB%"=="" set QUOTA_GB=100

(
    echo cloud.node.server-url=http://%SERVER_IP%:5000
    echo cloud.node.server-port=%NODE_PORT%
    echo cloud.node.quota-bytes=%QUOTA_GB%
    echo cloud.node.auto-register=true
) > node.properties

REM Iniciar
echo A iniciar o nó...
java -jar cloudsystem-node.jar --spring.config.location=node.properties
