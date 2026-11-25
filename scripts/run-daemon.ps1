param(
    [Parameter(Mandatory=$true)][string]$BatName,
    [Parameter(Mandatory=$true)][string]$ServiceName,
    [string]$ElvisHome = ""
)

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
if ([string]::IsNullOrWhiteSpace($ElvisHome)) {
    if (-not [string]::IsNullOrWhiteSpace($env:ELVIS_HOME)) {
        $ElvisHome = $env:ELVIS_HOME
    } else {
        $ElvisHome = Split-Path -Parent $scriptDir
    }
}
$ElvisHome = $ElvisHome.TrimEnd('\', '/')

$batPath = Join-Path $scriptDir $BatName
$logDir = Join-Path $ElvisHome "logs"
$pathsEnv = Join-Path $ElvisHome "config\paths.env"
if (Test-Path -LiteralPath $pathsEnv) {
    foreach ($line in [System.IO.File]::ReadAllLines($pathsEnv, [System.Text.Encoding]::UTF8)) {
        if ($line -match '^\s*ELVIS_LOG_DIR=(.+)$') {
            $val = $matches[1].Trim()
            if (-not [string]::IsNullOrWhiteSpace($val)) {
                $logDir = $val.Replace('%ELVIS_HOME%', $ElvisHome)
            }
        }
    }
}
if (-not (Test-Path -LiteralPath $logDir)) {
    New-Item -ItemType Directory -Path $logDir -Force | Out-Null
}

$logPath = Join-Path $logDir "$ServiceName.log"
$vbsPath = Join-Path $scriptDir "run-daemon.vbs"

# Launch detached invisible daemon via wscript.exe + WMI Win32_Process.Create
$wscriptExe = "C:\Windows\System32\wscript.exe"
$cmdLine = "$wscriptExe //B //Nologo `"$vbsPath`" `"$batPath`" `"$logPath`" `"$ElvisHome`""

$res = Invoke-CimMethod -ClassName Win32_Process -MethodName Create -Arguments @{
    CommandLine = [string]$cmdLine
    CurrentDirectory = [string]$scriptDir
}

if ($res.ReturnValue -eq 0) {
    Write-Output "SUCCESS: $ServiceName started as invisible daemon (PID $($res.ProcessId), HOME=$ElvisHome)"
} else {
    Write-Error "FAILED: $ServiceName failed to start with code $($res.ReturnValue)"
}
