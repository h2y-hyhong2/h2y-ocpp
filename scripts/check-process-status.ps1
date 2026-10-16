param(
    [string]$Target = "all",
    [switch]$Once
)

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 > $null

$targetMode = if ($Target) { $Target.ToLower() } else { "all" }
$scopeName = if ($targetMode -eq "infra") { "인프라 미들웨어" } elseif ($targetMode -eq "core") { "코어 백엔드 엔진" } else { "전체 시스템" }

if ($host.UI.RawUI) {
    $host.UI.RawUI.WindowTitle = "ELVIS - [$scopeName] 프로세스 및 포트 점유 실시간 진단"
}

function Show-Dashboard {
    Clear-Host
    Write-Host "========================================================================================================" -ForegroundColor DarkCyan
    Write-Host ("  ELVIS CSMS [" + $scopeName + "] 프로세스 및 포트 점유 실시간 진단 현황") -ForegroundColor Cyan
    Write-Host ("  점검 시각: " + (Get-Date -Format "yyyy-MM-dd HH:mm:ss") + " | 점검 범위: " + $scopeName) -ForegroundColor DarkGray
    Write-Host "========================================================================================================" -ForegroundColor DarkCyan
    Write-Host ""

    $allTargets = @(
        @{ Category = "런처UI"; Name = "elvis-launcher (통합 제어)"; Port = 1420; Group = "launcher" },
        @{ Category = "인프라"; Name = "MySQL 9.71"; Port = 13306; Group = "infra" },
        @{ Category = "인프라"; Name = "Apache Kafka KRaft"; Port = 19092; Group = "infra" },
        @{ Category = "코어엔진"; Name = "elvis-connect-ws"; Port = 18080; Group = "core" },
        @{ Category = "코어엔진"; Name = "elvis-connect-sync"; Port = 18081; Group = "core" },
        @{ Category = "코어엔진"; Name = "elvis-control-api"; Port = 18082; Group = "core" },
        @{ Category = "관제웹UI"; Name = "elvis-control-ui"; Port = 1421; Group = "ui" },
        @{ Category = "독립테스터"; Name = "elvis-tester-ui"; Port = 1422; Group = "tester" }
    )

    $targets = if ($targetMode -eq "infra") {
        $allTargets | Where-Object { $_["Group"] -eq "infra" }
    } elseif ($targetMode -eq "core") {
        $allTargets | Where-Object { $_["Group"] -eq "core" }
    } else {
        $allTargets
    }

    $header = ("{0,-8} | {1,-24} | {2,-7} | {3,-12} | {4,-8} | {5,-16} | {6,-10}" -f "[구분]", "[서비스명]", "[포트]", "[상태]", "[PID]", "[프로세스명]", "[메모리]")
    Write-Host $header -ForegroundColor Yellow
    Write-Host ("-" * 96) -ForegroundColor DarkGray

    $activeCount = 0
    foreach ($item in $targets) {
        $port = $item["Port"]
        $conns = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
        if ($conns) {
            $activeCount++
            $pids = $conns | Select-Object -ExpandProperty OwningProcess -Unique
            foreach ($p in $pids) {
                $proc = Get-Process -Id $p -ErrorAction SilentlyContinue
                $pName = if ($proc) { $proc.ProcessName } else { "Unknown" }
                $mem = if ($proc) { [math]::Round($proc.WorkingSet64 / 1MB, 1) } else { 0 }
                
                $line = ("{0,-8} | {1,-24} | :{2,-6} | {3,-12} | {4,-8} | {5,-16} | {6,-10}" -f $item["Category"], $item["Name"], $port, "[RUNNING]", $p, $pName, ($mem.ToString() + " MB"))
                Write-Host $line -ForegroundColor Green
            }
        } else {
            $line = ("{0,-8} | {1,-24} | :{2,-6} | {3,-12} | {4,-8} | {5,-16} | {6,-10}" -f $item["Category"], $item["Name"], $port, "[STOPPED]", "-", "-", "-")
            Write-Host $line -ForegroundColor DarkGray
        }
    }

    Write-Host ("-" * 96) -ForegroundColor DarkGray
    $totalTarget = ($targets | Measure-Object).Count
    Write-Host (" [요약] " + $scopeName + " (" + $totalTarget + "개 대상) 중 " + $activeCount + "개 포트가 정상 활성화(LISTEN)되어 있습니다.") -ForegroundColor Cyan
    Write-Host ""

    Write-Host "▶ 실행 중인 서비스 프로세스 세부 PID 현황 (MySQL / Kafka / Core / Web / Tester):" -ForegroundColor Yellow
    $detectedProcs = @()

    # 1) MySQL 데이터베이스 프로세스 감지
    $mysqlProcs = Get-CimInstance Win32_Process -Filter "Name = 'mysqld.exe'" -ErrorAction SilentlyContinue
    if ($mysqlProcs) {
        foreach ($mp in $mysqlProcs) {
            $pObj = Get-Process -Id $mp.ProcessId -ErrorAction SilentlyContinue
            $memMb = if ($pObj) { [math]::Round($pObj.WorkingSet64 / 1MB, 1) } else { 0 }
            $detectedProcs += [PSCustomObject]@{
                Group = "infra"
                Category = "인프라"
                Name = "MySQL 9.71 Database Engine"
                PID = $mp.ProcessId
                ProcessName = "mysqld.exe"
                PortStr = ":13306"
                Memory = $memMb
            }
        }
    }

    # 2) Java 프로세스 감지 (Kafka 및 코어 백엔드)
    $javaProcs = Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" -ErrorAction SilentlyContinue
    if ($javaProcs) {
        foreach ($jp in $javaProcs) {
            $cmd = $jp.CommandLine
            $tag = $null
            $grp = "other"
            $cat = "기타"
            $pStr = "-"
            if ($cmd -match "kafka\.Kafka") {
                $tag = "Apache Kafka KRaft Broker"
                $grp = "infra"
                $cat = "인프라"
                $pStr = ":19092"
            } elseif ($cmd -match "elvis-connect-ws") {
                $tag = "elvis-connect-ws Gateway"
                $grp = "core"
                $cat = "코어엔진"
                $pStr = ":18080"
            } elseif ($cmd -match "elvis-connect-sync") {
                $tag = "elvis-connect-sync Engine"
                $grp = "core"
                $cat = "코어엔진"
                $pStr = ":18081"
            } elseif ($cmd -match "elvis-control-api") {
                $tag = "elvis-control-api Server"
                $grp = "core"
                $cat = "코어엔진"
                $pStr = ":18082"
            } elseif ($cmd -match "GradleDaemon") {
                $tag = "Gradle Build Daemon"
                $grp = "build"
                $cat = "빌드도구"
                $pStr = "-"
            }

            if ($tag) {
                $pObj = Get-Process -Id $jp.ProcessId -ErrorAction SilentlyContinue
                $memMb = if ($pObj) { [math]::Round($pObj.WorkingSet64 / 1MB, 1) } else { 0 }
                $detectedProcs += [PSCustomObject]@{
                    Group = $grp
                    Category = $cat
                    Name = $tag
                    PID = $jp.ProcessId
                    ProcessName = "java.exe"
                    PortStr = $pStr
                    Memory = $memMb
                }
            }
        }
    }

    # 3) Node.js 프로세스 감지 (관제 UI, 테스터 UI, 런처)
    $nodeProcs = Get-CimInstance Win32_Process -Filter "Name = 'node.exe'" -ErrorAction SilentlyContinue
    if ($nodeProcs) {
        foreach ($np in $nodeProcs) {
            $cmd = $np.CommandLine
            $tag = $null
            $grp = "other"
            $cat = "웹서버"
            $pStr = "-"
            if ($cmd -match "elvis-launcher") {
                $tag = "elvis-launcher (Dev Host)"
                $grp = "launcher"
                $cat = "런처UI"
                $pStr = ":1420"
            } elseif ($cmd -match "elvis-control-ui") {
                $tag = "elvis-control-ui (Admin Web)"
                $grp = "ui"
                $cat = "관제웹UI"
                $pStr = ":1421"
            } elseif ($cmd -match "elvis-tester-ui") {
                $tag = "elvis-tester-ui (Test Web)"
                $grp = "tester"
                $cat = "독립테스터"
                $pStr = ":1422"
            }

            if ($tag) {
                $pObj = Get-Process -Id $np.ProcessId -ErrorAction SilentlyContinue
                $memMb = if ($pObj) { [math]::Round($pObj.WorkingSet64 / 1MB, 1) } else { 0 }
                $detectedProcs += [PSCustomObject]@{
                    Group = $grp
                    Category = $cat
                    Name = $tag
                    PID = $np.ProcessId
                    ProcessName = "node.exe"
                    PortStr = $pStr
                    Memory = $memMb
                }
            }
        }
    }

    # 범위 필터링 적용
    $filteredProcs = $detectedProcs | Where-Object {
        if ($targetMode -eq "infra") { $_.Group -eq "infra" }
        elseif ($targetMode -eq "core") { $_.Group -eq "core" }
        else { $true }
    }

    if ($filteredProcs -and ($filteredProcs | Measure-Object).Count -gt 0) {
        foreach ($item in $filteredProcs) {
            $catLabel = ("[{0}]" -f $item.Category).PadRight(8)
            $nameLabel = $item.Name.PadRight(28)
            $pidLabel = ("PID: {0}" -f $item.PID).PadRight(12)
            $memLabel = ("메모리: {0,6} MB" -f $item.Memory).PadRight(18)
            $portLabel = ("포트: {0,-7}" -f $item.PortStr)
            $exeLabel = ("({0})" -f $item.ProcessName)
            Write-Host ("  - " + $catLabel + " " + $nameLabel + " | " + $pidLabel + " | " + $memLabel + " | " + $portLabel + " | " + $exeLabel) -ForegroundColor White
        }
    } else {
        Write-Host "  (감지된 서비스 프로세스가 없습니다)" -ForegroundColor DarkGray
    }

    Write-Host ""
    Write-Host "========================================================================================================" -ForegroundColor DarkCyan
    Write-Host "  [R] 새로고침  |  [K] 특정 포트 강제 종료  |  [Q] 창 닫기" -ForegroundColor Gray
    Write-Host "========================================================================================================" -ForegroundColor DarkCyan
}

if ($Once) {
    Show-Dashboard
    exit 0
}

while ($true) {
    Show-Dashboard
    $key = $host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
    if ($key.Character -eq "q" -or $key.Character -eq "Q" -or $key.VirtualKeyCode -eq 27) {
        break
    } elseif ($key.Character -eq "r" -or $key.Character -eq "R") {
        continue
    } elseif ($key.Character -eq "k" -or $key.Character -eq "K") {
        Write-Host ""
        $targetPort = Read-Host " 종료할 포트 번호를 입력하세요 (예: 18080, 취소는 Enter)"
        if ($targetPort) {
            $conns = Get-NetTCPConnection -LocalPort $targetPort -State Listen -ErrorAction SilentlyContinue
            if ($conns) {
                $conns | ForEach-Object {
                    Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue
                    Write-Host (" PID " + $_.OwningProcess + " (Port " + $targetPort + ") 강제 종료 완료!") -ForegroundColor Red
                }
                Start-Sleep -Seconds 1
            } else {
                Write-Host " 해당 포트를 점유 중인 프로세스가 없습니다." -ForegroundColor Yellow
                Start-Sleep -Seconds 1
            }
        }
    }
}