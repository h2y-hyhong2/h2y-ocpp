[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$Host.UI.RawUI.WindowTitle = "ELVIS-CSMS 관제 UI 실행기"

Write-Host "===============================================================================" -ForegroundColor Cyan
Write-Host "   🚀 ELVIS-CSMS 관제 UI & 테스터 & 미들웨어 콘솔 실행기" -ForegroundColor Yellow
Write-Host "===============================================================================" -ForegroundColor Cyan
Write-Host "  [0] 🚀 ELVIS Launcher 실행 (인프라 & 미들웨어 백그라운드 관리 센터: http://127.0.0.1:1420)" -ForegroundColor Yellow
Write-Host "  [1] Vite 관제 웹 UI 실행 (http://127.0.0.1:1421)" -ForegroundColor Green
Write-Host "  [2] Tauri 데스크톱 관제 앱 실행" -ForegroundColor Green
Write-Host "  [3] 미들웨어 콘솔만 단독 실행 (MySQL 9.71 & Kafka KRaft 콘솔 창)" -ForegroundColor Green
Write-Host "  [4] 🔌 가상 충전기 테스터 웹 UI 실행 (http://127.0.0.1:1422)" -ForegroundColor Cyan
Write-Host "  [5] 🔌 가상 충전기 테스터 Tauri 데스크톱 앱 실행" -ForegroundColor Cyan
Write-Host ""
Write-Host "===============================================================================" -ForegroundColor Cyan

$choice = Read-Host "실행할 모드를 선택하세요 (0, 1, 2, 3, 4, 5) [기본값: 1]"
if ([string]::IsNullOrWhiteSpace($choice)) {
    $choice = "1"
} else {
    $choice = $choice.Trim()
}

$h2yDir = Split-Path -Parent $PSScriptRoot
$controlUiDir = Join-Path $h2yDir "elvis-control-ui"
$testerUiDir = Join-Path $h2yDir "elvis-tester-ui"
$scriptsDir = $PSScriptRoot

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
            Start-Process "http://127.0.0.1:1420"
            pnpm dev
        }
    }
    "1" {
        # 1. 미들웨어 콘솔 창 함께 띄우기
        Start-MiddlewareConsoles

        # 2. Vite 웹 UI 기동
        Write-Host ""
        Write-Host "🌐 Vite 관제 웹 개발 서버를 기동하고 브라우저(http://127.0.0.1:1421)를 엽니다..." -ForegroundColor Cyan
        Set-Location $controlUiDir
        Start-Process "http://127.0.0.1:1421"
        pnpm dev
    }
    "2" {
        # 1. 미들웨어 콘솔 창 함께 띄우기
        Start-MiddlewareConsoles

        # 2. Tauri 데스크톱 애플리케이션 기동
        Write-Host ""
        Write-Host "🖥️ Tauri 관제 데스크톱 애플리케이션을 개발 모드로 실행합니다..." -ForegroundColor Cyan
        Set-Location $controlUiDir
        pnpm tauri dev
    }
    "3" {
        # 미들웨어 콘솔만 단독 실행
        Start-MiddlewareConsoles
        Write-Host "✅ 미들웨어 콘솔 윈도우가 화면에 실행되었습니다." -ForegroundColor Green
    }
    "4" {
        # 가상 충전기 테스터 웹 UI 기동
        Write-Host ""
        Write-Host "🔌 Vite 가상 충전기 테스터 UI를 기동하고 브라우저(http://127.0.0.1:1422)를 엽니다..." -ForegroundColor Cyan
        $testerBat = Join-Path $scriptsDir "start-elvis-tester-ui.bat"
        if (Test-Path -LiteralPath $testerBat) {
            Start-Process -FilePath "cmd.exe" -ArgumentList "/c `"$testerBat`"" -WorkingDirectory $scriptsDir
        } else {
            Set-Location $testerUiDir
            Start-Process "http://127.0.0.1:1422"
            pnpm dev --open
        }
    }
    "5" {
        # 가상 충전기 테스터 Tauri 데스크톱 앱 기동
        Write-Host ""
        Write-Host "🔌 가상 충전기 테스터 Tauri 데스크톱 애플리케이션을 실행합니다..." -ForegroundColor Cyan
        $testerTauriBat = Join-Path $scriptsDir "start-elvis-tester-ui-tauri.bat"
        if (Test-Path -LiteralPath $testerTauriBat) {
            Start-Process -FilePath "cmd.exe" -ArgumentList "/c `"$testerTauriBat`"" -WorkingDirectory $scriptsDir
        } else {
            Set-Location $testerUiDir
            pnpm tauri:dev
        }
    }
    default {
        Write-Host "잘못된 입력입니다: $choice" -ForegroundColor Red
        Start-Sleep -Seconds 2
    }
}

