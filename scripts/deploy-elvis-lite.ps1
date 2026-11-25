param(
    [string]$TargetDir = "D:\elvis-lite",
    [switch]$SkipBinaries,
    [switch]$SkipJdk,
    [switch]$BuildJars
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$workspaceRoot = Split-Path -Parent $scriptDir
$TargetDir = $TargetDir.TrimEnd('\', '/')
$targetSlash = $TargetDir.Replace('\', '/')

Write-Host "========================================================================" -ForegroundColor Cyan
Write-Host "  ELVIS Lite 표준 인스톨 디렉터리 구조 세팅" -ForegroundColor Cyan
Write-Host "  Workspace : $workspaceRoot"
Write-Host "  TargetDir : $TargetDir"
Write-Host "========================================================================" -ForegroundColor Cyan

# 1. 기본 디렉터리 생성
$dirs = @(
    $TargetDir,
    (Join-Path $TargetDir "apps"),
    (Join-Path $TargetDir "binaries"),
    (Join-Path $TargetDir "config"),
    (Join-Path $TargetDir "scripts"),
    (Join-Path $TargetDir "data"),
    (Join-Path $TargetDir "logs")
)
foreach ($d in $dirs) {
    if (-not (Test-Path -LiteralPath $d)) {
        New-Item -ItemType Directory -Path $d -Force | Out-Null
    }
}

function Invoke-RoboCopyDir {
    param([string]$Src, [string]$Dst)
    if (-not (Test-Path -LiteralPath $Src)) {
        Write-Host "[WARN] 원본 경로가 없습니다: $Src" -ForegroundColor Yellow
        return
    }
    if (-not (Test-Path -LiteralPath $Dst)) {
        New-Item -ItemType Directory -Path $Dst -Force | Out-Null
    }
    & robocopy $Src $Dst /E /XO /R:1 /W:1 /NFL /NDL /NJH /NJS /NC /NS /NP | Out-Null
    if ($LASTEXITCODE -ge 8) {
        throw "robocopy 실패 (exit=$LASTEXITCODE): $Src -> $Dst"
    }
}

function Write-Utf8NoBomLines {
    param([string]$FilePath, [string[]]$Lines)
    $utf8NoBom = [System.Text.UTF8Encoding]::new($false)
    [System.IO.File]::WriteAllText($FilePath, (($Lines -join "`r`n") + "`r`n"), $utf8NoBom)
}

# 2. config/ 복사 및 대상 경로 기준 paths.env / my.ini / server.properties 보정 (UTF-8 No BOM)
Write-Host "[1/6] config/ 복사 및 대상 경로 보정..." -ForegroundColor Green
Invoke-RoboCopyDir -Src (Join-Path $workspaceRoot "config") -Dst (Join-Path $TargetDir "config")

$pathsEnvDst = Join-Path $TargetDir "config\paths.env"
if (Test-Path -LiteralPath $pathsEnvDst) {
    $envLines = [System.IO.File]::ReadAllLines($pathsEnvDst, [System.Text.Encoding]::UTF8) | ForEach-Object {
        if ($_ -match '^\s*ELVIS_DATA_DIR=') { "ELVIS_DATA_DIR=$TargetDir\data" }
        elseif ($_ -match '^\s*ELVIS_LOG_DIR=') { "ELVIS_LOG_DIR=$TargetDir\logs" }
        else { $_ }
    }
    Write-Utf8NoBomLines -FilePath $pathsEnvDst -Lines $envLines
}

$myIniDst = Join-Path $TargetDir "config\mysql\my.ini"
if (Test-Path -LiteralPath $myIniDst) {
    $iniLines = [System.IO.File]::ReadAllLines($myIniDst, [System.Text.Encoding]::UTF8) | ForEach-Object {
        if ($_ -match '^\s*datadir=') { "datadir=$targetSlash/data/mysql" }
        elseif ($_ -match '^\s*log-error=') { "log-error=$targetSlash/logs/mysql.log" }
        else { $_ }
    }
    Write-Utf8NoBomLines -FilePath $myIniDst -Lines $iniLines
}

$kafkaPropDst = Join-Path $TargetDir "config\kafka\server.properties"
if (Test-Path -LiteralPath $kafkaPropDst) {
    $propLines = [System.IO.File]::ReadAllLines($kafkaPropDst, [System.Text.Encoding]::UTF8) | ForEach-Object {
        if ($_ -match '^\s*log\.dirs=') { "log.dirs=$targetSlash/data/kafka" }
        else { $_ }
    }
    Write-Utf8NoBomLines -FilePath $kafkaPropDst -Lines $propLines
}

# 3. 운영 필수 scripts/ 복사
Write-Host "[2/6] 운영 scripts/ 복사..." -ForegroundColor Green
$runtimeScripts = @(
    "run-daemon.ps1",
    "run-daemon.vbs",
    "start-mysql.bat",
    "init-mysql.bat",
    "load-mysql-schema.bat",
    "clear-mysql-data.bat",
    "start-kafka-kraft.bat",
    "create-kafka-topics.bat",
    "start-elvis-connect-ws.bat",
    "start-elvis-connect-sync.bat",
    "start-elvis-control-api.bat",
    "check-status.bat",
    "check-process-status.ps1"
)
foreach ($f in $runtimeScripts) {
    $srcFile = Join-Path $scriptDir $f
    if (Test-Path -LiteralPath $srcFile) {
        Copy-Item -LiteralPath $srcFile -Destination (Join-Path $TargetDir "scripts\$f") -Force
    }
}

# 4. binaries/ (MySQL, Kafka) 복사
if (-not $SkipBinaries) {
    Write-Host "[3/6] binaries/ (MySQL 9.71, Apache Kafka 4.3.1) 복사..." -ForegroundColor Green
    Invoke-RoboCopyDir -Src (Join-Path $workspaceRoot "binaries\mysql-9.7.1-winx64") -Dst (Join-Path $TargetDir "binaries\mysql-9.7.1-winx64")
    Invoke-RoboCopyDir -Src (Join-Path $workspaceRoot "binaries\kafka_2.13-4.3.1") -Dst (Join-Path $TargetDir "binaries\kafka_2.13-4.3.1")
} else {
    Write-Host "[3/6] binaries/ (MySQL, Kafka) 복사 생략 (-SkipBinaries)" -ForegroundColor Yellow
}

# 5. binaries/jdk 복사
if (-not $SkipJdk) {
    Write-Host "[4/6] binaries/jdk 복사..." -ForegroundColor Green
    $jdkCandidates = @(
        "D:\project\lselink\ocpp-lite\jdk\jdk-25.0.4.1+1",
        "D:\jdk\windows\jdk-25.0.4.1+1",
        "D:\jdk\windows\jdk-21.0.12.1+1"
    )
    $jdkSrc = $jdkCandidates | Where-Object { Test-Path -LiteralPath (Join-Path $_ "bin\java.exe") } | Select-Object -First 1
    if ($jdkSrc) {
        Invoke-RoboCopyDir -Src $jdkSrc -Dst (Join-Path $TargetDir "binaries\jdk")
    } else {
        Write-Host "[WARN] 로컬 JDK를 찾지 못해 binaries\jdk 복사를 건너뛰었습니다." -ForegroundColor Yellow
    }
} else {
    Write-Host "[4/6] binaries/jdk 복사 생략 (-SkipJdk)" -ForegroundColor Yellow
}

# 6. 코어 백엔드 JAR (apps\elvis-*.jar) 빌드(선택) 및 복사
Write-Host "[5/6] apps/ 코어 백엔드 JAR 배치..." -ForegroundColor Green
if ($BuildJars) {
    $jdkBuildCandidates = @(
        "D:\project\lselink\ocpp-lite\jdk\jdk-25.0.4.1+1",
        "D:\jdk\windows\jdk-25.0.4.1+1",
        "D:\jdk\windows\jdk-21.0.12.1+1"
    )
    $jdkBuildHome = $jdkBuildCandidates | Where-Object { Test-Path -LiteralPath (Join-Path $_ "bin\java.exe") } | Select-Object -First 1
    if ($jdkBuildHome) {
        $env:JAVA_HOME = $jdkBuildHome
        $env:PATH = "$jdkBuildHome\bin;$env:PATH"
    }
    if (-not $env:GRADLE_USER_HOME -and (Test-Path -LiteralPath "D:\project\lselink\ocpp-lite\gradle")) {
        $env:GRADLE_USER_HOME = "D:\project\lselink\ocpp-lite\gradle\.gradle"
    }
    $gradleBat = "D:\project\lselink\ocpp-lite\gradle\gradle-9.9.1\bin\gradle.bat"
    if (-not (Test-Path -LiteralPath $gradleBat)) {
        $gradleBat = Join-Path $workspaceRoot "gradlew.bat"
    }
    Write-Host "  - Gradle bootJar 실행 중..." -ForegroundColor Gray
    Push-Location $workspaceRoot
    try {
        & $gradleBat :elvis-connect-ws:bootJar :elvis-connect-sync:bootJar :elvis-control-api:bootJar
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle bootJar 빌드 실패 (exit=$LASTEXITCODE)"
        }
    } finally {
        Pop-Location
    }
}

$backendModules = @("elvis-connect-ws", "elvis-connect-sync", "elvis-control-api")
foreach ($mod in $backendModules) {
    $libsDir = Join-Path $workspaceRoot "$mod\build\libs"
    $jar = $null
    if (Test-Path -LiteralPath $libsDir) {
        $jar = Get-ChildItem -LiteralPath $libsDir -Filter "$mod*.jar" -File |
            Where-Object { $_.Name -notlike "*-plain.jar" } |
            Sort-Object LastWriteTime -Descending |
            Select-Object -First 1
    }
    if ($jar) {
        $destJar = Join-Path $TargetDir "apps\$mod.jar"
        Copy-Item -LiteralPath $jar.FullName -Destination $destJar -Force
        Write-Host ("  [OK] {0} -> {1}" -f $jar.Name, $destJar) -ForegroundColor Green
    } else {
        Write-Host ("  [미설치] {0} 빌드 JAR 없음 ({1}) - -BuildJars 옵션으로 빌드 후 재실행하세요." -f $mod, $libsDir) -ForegroundColor Yellow
    }
}

# 7. 데스크톱 4대 앱 실행파일(.exe) 복사 (빌드 산출물이 있는 경우)
Write-Host "[6/6] 데스크톱 4대 앱 (.exe) 확인 및 복사..." -ForegroundColor Green
$exeMap = @{
    "elvis-launcher.exe"   = Join-Path $workspaceRoot "elvis-launcher\src-tauri\target\release\elvis-launcher.exe"
    "elvis-log-viewer.exe" = Join-Path $workspaceRoot "elvis-log-viewer\target\release\elvis-log-viewer.exe"
    "elvis-control-ui.exe" = Join-Path $workspaceRoot "elvis-control-ui\src-tauri\target\release\elvis-control-ui.exe"
    "elvis-tester-ui.exe"  = Join-Path $workspaceRoot "elvis-tester-ui\src-tauri\target\release\elvis-tester-ui.exe"
}
foreach ($name in $exeMap.Keys) {
    $srcExe = $exeMap[$name]
    $dstExe = Join-Path $TargetDir $name
    if (Test-Path -LiteralPath $srcExe) {
        try {
            Copy-Item -LiteralPath $srcExe -Destination $dstExe -Force
            Write-Host "  [OK] $dstExe" -ForegroundColor Green
        } catch {
            Write-Host "  [WARN] $name 복사 건너뜀 (실행 중이거나 잠김): $($_.Exception.Message)" -ForegroundColor Yellow
        }
    } elseif (Test-Path -LiteralPath $dstExe) {
        Write-Host "  [KEEP] 기존 파일 유지: $dstExe" -ForegroundColor Gray
    } else {
        Write-Host "  [미설치] $name 없음" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "========================================================================" -ForegroundColor Cyan
Write-Host "  세팅 완료: $TargetDir" -ForegroundColor Cyan
Write-Host "========================================================================" -ForegroundColor Cyan
