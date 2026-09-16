[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$Host.UI.RawUI.WindowTitle = "ELVIS-CSMS 관제 UI 실행기"

Write-Host "===============================================================================" -ForegroundColor Cyan
Write-Host "   🚀 ELVIS-CSMS 관제 UI 실행기 (Vite Web and Prototype)" -ForegroundColor Yellow
Write-Host "===============================================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "  [1] 단독 프로토타입 브라우저 실행 (서버 구동 불필요, 즉시 열림)" -ForegroundColor Green
Write-Host "  [2] Vite 개발 서버 웹 UI 실행 (http://127.0.0.1:1420)" -ForegroundColor Green
Write-Host "  [3] Tauri 데스크톱 네이티브 앱 실행 (.exe 윈도우 모드)" -ForegroundColor Green
Write-Host ""
Write-Host "===============================================================================" -ForegroundColor Cyan

$choice = Read-Host "실행할 모드를 선택하세요 (1, 2, 3) [기본값: 1]"
if ([string]::IsNullOrWhiteSpace($choice)) {
    $choice = "1"
} else {
    $choice = $choice.Trim()
}

$h2yDir = Split-Path -Parent $PSScriptRoot
$controlUiDir = Join-Path $h2yDir "elvis-control-ui"
$prototypePath = "d:\project\lselink\lselink-work\2026\과제\[3368]2026-07-02-002-CSMS Lite 버전(POC)\prototype\index.html"

switch ($choice) {
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
        Write-Host ""
        Write-Host "🌐 Vite 개발 서버를 기동하고 브라우저(http://127.0.0.1:1420)를 엽니다..." -ForegroundColor Cyan
        Set-Location $controlUiDir
        Start-Process "http://127.0.0.1:1420"
        pnpm dev
    }
    "3" {
        Write-Host ""
        Write-Host "🖥️ Tauri 데스크톱 애플리케이션을 개발 모드로 실행합니다..." -ForegroundColor Cyan
        Set-Location $controlUiDir
        pnpm tauri dev
    }
    default {
        Write-Host "잘못된 입력입니다: $choice" -ForegroundColor Red
        Start-Sleep -Seconds 2
    }
}
