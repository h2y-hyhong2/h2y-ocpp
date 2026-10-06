[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$Host.UI.RawUI.WindowTitle = "ELVIS-CSMS 관제 UI 실행기"

Write-Host "===============================================================================" -ForegroundColor Cyan
Write-Host "   🚀 ELVIS-CSMS 관제 UI & 미들웨어 콘솔 실행기" -ForegroundColor Yellow
Write-Host "===============================================================================" -ForegroundColor Cyan
Write-Host "  [0] 🚀 ELVIS Launcher 실행 (인프라 & 미들웨어 백그라운드 관리 센터: http://127.0.0.1:1422)" -ForegroundColor Yellow
Write-Host "  [1] 단독 프로토타입 브라우저 실행 (서버 구동 불필요, 즉시 열림)" -ForegroundColor Green
Write-Host "  [2] Vite 관제 웹 UI 실행 (http://127.0.0.1:1420)" -ForegroundColor Green
Write-Host "  [3] Tauri 데스크톱 관제 앱 실행" -ForegroundColor Green
Write-Host "  [4] 미들웨어 콘솔만 단독 실행 (MySQL 9.71 & Kafka KRaft 콘솔 창)" -ForegroundColor Green
Write-Host ""
Write-Host "===============================================================================" -ForegroundColor Cyan

$choice = Read-Host "실행할 모드를 선택하세요 (0, 1, 2, 3, 4) [기본값: 2]"
if ([string]::IsNullOrWhiteSpace($choice)) {
    $choice = "2"
} else {
    $choice = $choice.Trim()
}

$h2yDir = Split-Path -Parent $PSScriptRoot
$controlUiDir = Join-Path $h2yDir "elvis-control-ui"
$scriptsDir = $PSScriptRoot
$prototypePath = "d:\project\lselink\lselink-work\2026\과제\[3368]2026-07-02-002-CSMS Lite 버전(POC)\prototype\index.html"

# 미들웨어 콘솔(Kafka & MySQL) 기동 함수
function Start-MiddlewareConsoles {
    Write-Host ""
    Write-Host "🖥️  로컬 미들웨어 콘솔(MySQL 9.71 & Kafka KRaft)을 화면에 띄웁니다..." -ForegroundColor Yellow
    $allBat = Join-Path $scriptsDir "start-all-middleware.bat"
    if (Test-Path -LiteralPath $allBat) {
        Start-Process -FilePath "cmd.exe" -ArgumentList "/c `"$allBat`"" -WorkingDirectory $scriptsDir
        Start-Sleep -Seconds 2
    } else {
        Write-Host "[WARN] start-all-middleware.bat 파일을 찾을 수 없습니다." -ForegroundColor Yellow
    }
}

switch ($choice) {
    "0" {
        Write-Host ""
        Write-Host "🚀 ELVIS Launcher (인프라 & 미들웨어 관리자)를 실행합니다..." -ForegroundColor Cyan
        $launcherBat = Join-Path $scriptsDir "start-elvis-launcher.bat"
        if (Test-Path -LiteralPath $launcherBat) {
            Start-Process -FilePath "cmd.exe" -ArgumentList "/c `"$launcherBat`"" -WorkingDirectory $scriptsDir
        } else {
            $launcherDir = Join-Path $h2yDir "elvis-launcher"
            Set-Location $launcherDir
            Start-Process "http://127.0.0.1:1422"
            pnpm dev
        }
    }
    "1" {
        Write-Host ""
        Write-Host "🎯 프로토타입 관제 UI를 기본 브라우저에서 실행합니다..." -ForegroundColor Cyan
        if (Test-Path -LiteralPath $prototypePath) {
            Start-Process -FilePath $prototypePath
        } else {
            Write-Host "[ERROR] 프로토타입 파일을 찾을 수 없습니다: $prototypePath" -ForegroundColor Red
            Read-Host "계속하려면 엔터를 누르세요..."
        }
    }
    "2" {
        # 1. 미들웨어 콘솔 창 함께 띄우기
        Start-MiddlewareConsoles

        # 2. Vite 웹 UI 기동
        Write-Host ""
        Write-Host "🌐 Vite 개발 서버를 기동하고 브라우저(http://127.0.0.1:1420)를 엽니다..." -ForegroundColor Cyan
        Set-Location $controlUiDir
        Start-Process "http://127.0.0.1:1420"
        pnpm dev
    }
    "3" {
        # 1. 미들웨어 콘솔 창 함께 띄우기
        Start-MiddlewareConsoles

        # 2. Tauri 데스크톱 애플리케이션 기동
        Write-Host ""
        Write-Host "🖥️ Tauri 데스크톱 애플리케이션을 개발 모드로 실행합니다..." -ForegroundColor Cyan
        Set-Location $controlUiDir
        pnpm tauri dev
    }
    "4" {
        # 미들웨어 콘솔만 단독 실행
        Start-MiddlewareConsoles
        Write-Host "✅ 미들웨어 콘솔 윈도우가 화면에 실행되었습니다." -ForegroundColor Green
    }
    default {
        Write-Host "잘못된 입력입니다: $choice" -ForegroundColor Red
        Start-Sleep -Seconds 2
    }
}

