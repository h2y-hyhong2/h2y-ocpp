param(
    [Parameter(Mandatory=$true)][string]$BatName,
    [Parameter(Mandatory=$true)][string]$ServiceName
)

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$batPath = Join-Path $scriptDir $BatName
$logDir = "D:\elvis-lite\logs"
if (-not (Test-Path $logDir)) {
    New-Item -ItemType Directory -Path $logDir -Force | Out-Null
}

# ClickHouse는 바이너리 내부(run-clickhouse.sh)에서 자체적으로 clickhouse.log에 기록하므로 startup 로그 분리
if ($ServiceName -eq "clickhouse") {
    $logPath = Join-Path $logDir "clickhouse-startup.log"
} else {
    $logPath = Join-Path $logDir "$ServiceName.log"
}

$vbsPath = Join-Path $scriptDir "run-daemon.vbs"

# 1. wscript.exe는 GUI 서브시스템 바이너리이므로 Windows가 콘솔 창(conhost.exe)을 0% 전혀 생성하지 않음 (100% 완전 무창)
# 2. WMI (Invoke-CimMethod Win32_Process.Create)를 통해 WmiPrvSE.exe 시스템 서비스 하위 독립 프로세스로 생성
# 3. 런처 창을 닫거나 터미널에서 Ctrl+C를 눌러도 종료되지 않고 독립 데몬으로 영구 지속
$wscriptExe = "C:\Windows\System32\wscript.exe"
$cmdLine = "$wscriptExe //B //Nologo `"$vbsPath`" `"$batPath`" `"$logPath`""

$res = Invoke-CimMethod -ClassName Win32_Process -MethodName Create -Arguments @{
    # Split-Path 결과는 PSObject로 래핑될 수 있어 CIM 마샬링 시 ReturnValue 21(잘못된 매개변수)이 발생하므로 명시적 [string] 캐스팅
    CommandLine = [string]$cmdLine
    CurrentDirectory = [string]$scriptDir
}

if ($res.ReturnValue -eq 0) {
    Write-Output "SUCCESS: $ServiceName started as invisible daemon (PID $($res.ProcessId))"
} else {
    Write-Error "FAILED: $ServiceName failed to start with code $($res.ReturnValue)"
}
