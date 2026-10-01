@echo off
title Sistema de Administracion para Academia

cd /d "%~dp0"

echo ==========================================================
echo   SISTEMA DE ADMINISTRACION PARA ACADEMIA
echo ==========================================================

:: 1. Detectar JAVA_HOME si no esta definido
if "%JAVA_HOME%"=="" (
    for /d %%D in ("C:\Program Files\Java\jdk-21*") do (
        set "JAVA_HOME=%%D"
    )
)
if "%JAVA_HOME%"=="" (
    for /d %%D in ("C:\Program Files\Java\jdk*") do (
        set "JAVA_HOME=%%D"
    )
)

if not "%JAVA_HOME%"=="" (
    echo [*] Java JDK detectado en: %JAVA_HOME%
) else (
    echo [*] Usando Java configurado en PATH...
)

:: 2. Comprobar si 'mvn' ya existe en PATH
where mvn >nul 2>nul
if %ERRORLEVEL% equ 0 goto USAR_SISTEMA

:: 3. Usar o descargar Maven portable en .tools
set "TOOLS_DIR=%~dp0.tools"
set "MAVEN_DIR=%TOOLS_DIR%\apache-maven-3.9.6"
set "MVN_BIN=%MAVEN_DIR%\bin\mvn.cmd"

if exist "%MVN_BIN%" goto EJECUTAR_PORTABLE

echo [*] Maven no encontrado. Descargando version portable...
if not exist "%TOOLS_DIR%" mkdir "%TOOLS_DIR%"

echo [*] Descargando componentes (toma solo unos segundos)...
powershell -NoProfile -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; (New-Object System.Net.WebClient).DownloadFile('https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip', '%TOOLS_DIR%\maven.zip')"

echo [*] Descomprimiendo herramientas...
powershell -NoProfile -Command "Expand-Archive -Path '%TOOLS_DIR%\maven.zip' -DestinationPath '%TOOLS_DIR%' -Force; Remove-Item '%TOOLS_DIR%\maven.zip' -Force"

if not exist "%MVN_BIN%" goto ERROR_MAVEN

:EJECUTAR_PORTABLE
echo [OK] Entorno preparado.
echo [*] Compilando y ejecutando la aplicacion...
echo ==========================================================
call "%MVN_BIN%" compile javafx:run
goto FIN

:USAR_SISTEMA
echo [*] Maven detectado en el sistema.
echo [*] Compilando y ejecutando la aplicacion...
echo ==========================================================
call mvn compile javafx:run
goto FIN

:ERROR_MAVEN
echo.
echo [ERROR] No se pudo descargar Maven portable.
echo Verifica que tu conexion a internet este activa.
goto FIN

:FIN
echo.
echo ==========================================================
echo La ejecucion ha finalizado.
echo Presiona cualquier tecla para cerrar esta ventana...
pause >nul
