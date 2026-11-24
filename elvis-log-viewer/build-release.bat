@echo off
setlocal
chcp 65001 > nul

set "SCRIPT_DIR=%~dp0"
set "OUT_DIR=D:\"
set "SRC_EXE=%SCRIPT_DIR%target\release\elvis-log-viewer.exe"
set "DST_EXE=%OUT_DIR%elvis-log-viewer.exe"

echo [elvis-log-viewer] 릴리스 빌드 시작...
pushd "%SCRIPT_DIR%"
cargo build --release
if errorlevel 1 (
    echo [elvis-log-viewer] 빌드 실패!
    popd
    exit /b 1
)
popd

echo [elvis-log-viewer] 실행파일 복사: "%DST_EXE%"
copy /Y "%SRC_EXE%" "%DST_EXE%" > nul
if errorlevel 1 (
    echo [elvis-log-viewer] 복사 실패! 실행 중인 elvis-log-viewer.exe를 종료한 뒤 다시 시도해 주세요.
    exit /b 1
)

echo [elvis-log-viewer] 완료: "%DST_EXE%"
endlocal
