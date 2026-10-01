# ==============================================================================
# SCRIPT DE ARRANQUE Y CONSTRUCCION AUTOMATICA - ACADEMIA ADMIN
# ==============================================================================
$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  SISTEMA DE ADMINISTRACION PARA ACADEMIA" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Habilitar TLS 1.2 para descargas seguras
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

# 2. Comprobar si 'mvn' esta disponible en PATH
$mvnCmd = Get-Command mvn -ErrorAction SilentlyContinue

if (-not $mvnCmd) {
    Write-Host "[*] Maven no detectado en PATH. Configurando Maven portable..." -ForegroundColor Yellow
    $baseDir = Split-Path -Parent $MyInvocation.MyCommand.Path
    if (-not $baseDir) { $baseDir = (Get-Location).Path }

    $toolsDir = Join-Path $baseDir ".tools"
    $mavenDir = Join-Path $toolsDir "apache-maven-3.9.6"
    $mvnExecutable = Join-Path $mavenDir "bin\mvn.cmd"

    if (-not (Test-Path $mvnExecutable)) {
        if (-not (Test-Path $toolsDir)) { New-Item -ItemType Directory -Path $toolsDir | Out-Null }
        $zipPath = Join-Path $toolsDir "maven.zip"
        $mavenUrl = "https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip"
        
        Write-Host "[*] Descargando Apache Maven 3.9.6 portable..." -ForegroundColor Yellow
        (New-Object System.Net.WebClient).DownloadFile($mavenUrl, $zipPath)
        
        Write-Host "[*] Descomprimiendo Maven..." -ForegroundColor Yellow
        Expand-Archive -Path $zipPath -DestinationPath $toolsDir -Force
        Remove-Item $zipPath -Force
    }
    $mvnPath = $mvnExecutable
} else {
    $mvnPath = "mvn"
}

Write-Host "[OK] Usando Maven: $mvnPath" -ForegroundColor Green
Write-Host "[*] Compilando e iniciando la aplicacion de escritorio..." -ForegroundColor Cyan

& $mvnPath javafx:run
