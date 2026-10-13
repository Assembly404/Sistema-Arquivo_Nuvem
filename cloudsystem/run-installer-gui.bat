@echo off
REM ============================================================
REM  CloudSystem Node Installer GUI Launcher (Windows)
REM ============================================================
REM  Lança o instalador gráfico JavaFX do nó de armazenamento.
REM
REM  Uso:
REM     run-installer-gui.bat
REM
REM  Requisitos:
REM     - Java 17+ instalado e no PATH
REM ============================================================

set JAR_NAME=cloudsystem-0.0.1-SNAPSHOT.jar
set JAR_PATH=target\%JAR_NAME%

echo CloudSystem Node Installer GUI
echo ================================

REM Verificar se o JAR existe
if not exist "%JAR_PATH%" (
    echo JAR nao encontrado. A compilar...
    call mvnw.cmd package -DskipTests -q
)

REM Verificar Java
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo.
    echo ERRO: Java nao encontrado.
    echo Instale Java 17+ em https://adoptium.net
    pause
    exit /b 1
)

echo.
echo A iniciar instalador grafico...
echo.

REM Executar a classe JavaFX principal
java --module-path target\classes;target\dependency ^
     --add-modules javafx.controls,javafx.fxml,javafx.web ^
     -cp "%JAR_PATH%" ^
     com.assemblyproject.cloudsystem.agent.NodeInstallerGUI

if %errorlevel% neq 0 (
    echo.
    echo Erro ao iniciar o instalador.
    echo Verifique se o Java 17+ esta instalado corretamente.
    pause
)