@echo off
setlocal
chcp 65001 > nul

set "ROOT_DIR=%~dp0.."
set "TARGET=%~1"
if "%TARGET%"=="" set "TARGET=all"

echo ========================================================================
echo   ELVIS 데스크톱 4대 앱 실행파일(.exe) 릴리스 빌드 및 D:\elvis-lite 배포
echo   대상: %TARGET%  (all ^| log-viewer ^| launcher ^| control-ui ^| tester-ui)
echo ========================================================================

if /I "%TARGET%"=="all" goto :build_log_viewer
if /I "%TARGET%"=="log-viewer" goto :build_log_viewer
if /I "%TARGET%"=="launcher" goto :build_launcher
if /I "%TARGET%"=="control-ui" goto :build_control_ui
if /I "%TARGET%"=="tester-ui" goto :build_tester_ui

echo [오류] 알 수 없는 대상입니다: %TARGET%
echo 사용법: build-desktop-exes.bat [all^|log-viewer^|launcher^|control-ui^|tester-ui]
exit /b 1

:build_log_viewer
echo.
echo [1/4] elvis-log-viewer 빌드 및 D:\elvis-lite\elvis-log-viewer.exe 복사...
call "%ROOT_DIR%\elvis-log-viewer\build-release.bat"
if errorlevel 1 exit /b 1
if /I "%TARGET%"=="log-viewer" goto :done

:build_launcher
echo.
echo [2/4] elvis-launcher 빌드 및 D:\elvis-lite\elvis-launcher.exe 복사...
pushd "%ROOT_DIR%\elvis-launcher"
call pnpm tauri:build:exe
if errorlevel 1 (
    popd
    exit /b 1
)
popd
if /I "%TARGET%"=="launcher" goto :done

:build_control_ui
echo.
echo [3/4] elvis-control-ui 빌드 및 D:\elvis-lite\elvis-control-ui.exe 복사...
pushd "%ROOT_DIR%\elvis-control-ui"
call pnpm tauri:build:exe
if errorlevel 1 (
    popd
    exit /b 1
)
popd
if /I "%TARGET%"=="control-ui" goto :done

:build_tester_ui
echo.
echo [4/4] elvis-tester-ui 빌드 및 D:\elvis-lite\elvis-tester-ui.exe 복사...
pushd "%ROOT_DIR%\elvis-tester-ui"
call pnpm tauri:build:exe
if errorlevel 1 (
    popd
    exit /b 1
)
popd

:done
echo.
echo ========================================================================
echo   실행파일 배포 완료 (D:\elvis-lite)
echo     - D:\elvis-lite\elvis-log-viewer.exe
echo     - D:\elvis-lite\elvis-launcher.exe
echo     - D:\elvis-lite\elvis-control-ui.exe
echo     - D:\elvis-lite\elvis-tester-ui.exe
echo ========================================================================
endlocal
