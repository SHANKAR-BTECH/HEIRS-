param(
    [string]$ProjectRoot = (Resolve-Path "$PSScriptRoot\..").Path
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

function Write-Status  { param([string]$Message) Write-Host "[STATUS] $Message" -ForegroundColor Cyan }
function Write-Success { param([string]$Message) Write-Host "[SUCCESS] $Message" -ForegroundColor Green }
function Write-Warning { param([string]$Message) Write-Host "[WARNING] $Message" -ForegroundColor Yellow }
function Write-ErrorMsg { param([string]$Message) Write-Host "[ERROR] $Message" -ForegroundColor Red }

Write-Host "=== HEIRS Desktop Packaging Script (J8) ===" -ForegroundColor Green
Write-Host "Project root: $ProjectRoot" -ForegroundColor Yellow

# 1. Verify Java/jpackage environment
Write-Status "1. Verifying Java/jpackage environment..."

if (-not (Get-Command jpackage -ErrorAction SilentlyContinue)) {
    if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\jpackage.exe")) {
        $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    } elseif (Test-Path "C:\Program Files\Java\jdk-21.0.10\bin\jpackage.exe") {
        $env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.10"
        $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    }
}

$javaCmd = Get-Command java -ErrorAction SilentlyContinue
if (-not $javaCmd) {
    Write-ErrorMsg "java executable not found on PATH or JAVA_HOME."
    exit 1
}

$javaVersion = & java --version 2>&1 | Select-Object -First 1
Write-Host "Java version: $javaVersion" -ForegroundColor White

if ($javaVersion -notmatch "21\.") {
    Write-ErrorMsg "Java 21 is required. Found: $javaVersion"
    exit 1
}

$jpackageCmd = Get-Command jpackage -ErrorAction SilentlyContinue
if (-not $jpackageCmd) {
    Write-ErrorMsg "jpackage not found. A JDK 21 with jpackage is required."
    exit 1
}
$jpackageVersion = & jpackage --version 2>&1 | Select-Object -First 1
Write-Host "jpackage version: $jpackageVersion" -ForegroundColor White

$javafxAdminDir = Join-Path $ProjectRoot "javafx-admin"
Push-Location $javafxAdminDir
try {
    # 2. Clean and build application JAR
    Write-Status "2. Building JavaFX application JAR..."
    & .\mvnw.cmd clean package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        Write-ErrorMsg "Maven package failed."
        exit 1
    }

    $mainJar = Get-ChildItem "target\heirs-desktop-admin-*.jar" | Where-Object { $_.Name -notlike "*sources*" -and $_.Name -notlike "*javadoc*" } | Select-Object -First 1
    if (-not $mainJar) {
        Write-ErrorMsg "Target application JAR not found."
        exit 1
    }
    Write-Host "Application JAR: $($mainJar.Name)" -ForegroundColor White

    # 3. Copy runtime dependencies
    Write-Status "3. Resolving and copying runtime dependencies..."
    $packageJarsDir = "target\package-jars"
    if (Test-Path $packageJarsDir) { Remove-Item -Recurse -Force $packageJarsDir }
    & .\mvnw.cmd dependency:copy-dependencies "-DoutputDirectory=$packageJarsDir" "-DincludeScope=runtime"
    if ($LASTEXITCODE -ne 0) {
        Write-ErrorMsg "Dependency copy failed."
        exit 1
    }

    # 4. Prepare module and application directories
    Write-Status "4. Preparing modular JavaFX and classpath JARs..."
    $javafxModulesDir = "target\javafx-modules"
    $appJarsDir = "target\app-jars"

    if (Test-Path $javafxModulesDir) { Remove-Item -Recurse -Force $javafxModulesDir }
    if (Test-Path $appJarsDir) { Remove-Item -Recurse -Force $appJarsDir }

    New-Item -ItemType Directory -Force -Path $javafxModulesDir | Out-Null
    New-Item -ItemType Directory -Force -Path $appJarsDir | Out-Null

    # Copy JavaFX Windows modular jars to module-path
    Get-ChildItem "$packageJarsDir\*win.jar" | Where-Object { $_.Name -like "javafx-*" } | Copy-Item -Destination $javafxModulesDir

    # Copy application JAR and non-JavaFX runtime dependencies to app-jars
    Copy-Item $mainJar.FullName -Destination $appJarsDir
    Get-ChildItem "$packageJarsDir\*.jar" | Where-Object { $_.Name -notlike "javafx-*" } | Copy-Item -Destination $appJarsDir

    # 5. Execute jpackage for Windows app-image
    Write-Status "5. Creating Windows app-image with jpackage..."
    $distDir = "dist"
    $appImageDir = Join-Path $distDir "HEIRS"
    if (Test-Path $appImageDir) {
        Remove-Item -Recurse -Force $appImageDir
    }

    $mainClass = "com.heirs.desktop.HeirsDesktopApplication"
    $jpackageArgs = @(
        "--type", "app-image",
        "--name", "HEIRS",
        "--app-version", "1.0.0",
        "--vendor", "HEIRS Project",
        "--copyright", "Copyright 2026 HEIRS Project",
        "--description", "Higher Education Information Retrieval System Desktop Client",
        "--module-path", $javafxModulesDir,
        "--add-modules", "javafx.controls,javafx.fxml,javafx.swing",
        "--input", $appJarsDir,
        "--main-jar", $mainJar.Name,
        "--main-class", $mainClass,
        "--dest", $distDir,
        "--java-options", "-Xmx512m",
        "--java-options", "-Xms128m"
    )

    & jpackage @jpackageArgs
    if ($LASTEXITCODE -ne 0) {
        Write-ErrorMsg "jpackage failed with exit code $LASTEXITCODE"
        exit 1
    }

    # 6. Verify App-Image Structure
    Write-Status "6. Verifying app-image structure..."
    $exePath = Join-Path $appImageDir "HEIRS.exe"
    if (-not (Test-Path $exePath)) {
        Write-ErrorMsg "HEIRS.exe not found at $exePath"
        exit 1
    }
    Write-Success "HEIRS.exe exists at $exePath"

    $appContentDir = Join-Path $appImageDir "app"
    $runtimeContentDir = Join-Path $appImageDir "runtime"

    if (-not (Test-Path $appContentDir) -or -not (Test-Path $runtimeContentDir)) {
        Write-ErrorMsg "Required app/ or runtime/ directory missing in app-image."
        exit 1
    }
    Write-Success "App-image structure complete (HEIRS.exe, app/, runtime/)."

    # 7. Check for optional installer tooling
    Write-Status "7. Checking optional Windows installer tooling..."
    $wixTools = @("candle.exe", "light.exe", "wix.exe")
    $hasWix = $false
    foreach ($tool in $wixTools) {
        if (Get-Command $tool -ErrorAction SilentlyContinue) {
            $hasWix = $true
            break
        }
    }

    if ($hasWix) {
        Write-Host "WiX toolchain detected. Optional installer generation possible." -ForegroundColor Green
    } else {
        Write-Host "Installer skipped -- required external packaging tool unavailable." -ForegroundColor Yellow
    }

    Write-Success "=== HEIRS Desktop 1.0.0 Windows App-Image Ready ==="
    Write-Host "Output: $exePath" -ForegroundColor Green
}
finally {
    Pop-Location
}
