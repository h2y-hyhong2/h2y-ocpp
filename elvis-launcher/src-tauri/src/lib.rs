use std::process::Command;
use std::sync::atomic::{AtomicU8, Ordering};
use tauri::{
    menu::{Menu, MenuItem, PredefinedMenuItem},
    tray::{MouseButton, MouseButtonState, TrayIconBuilder, TrayIconEvent},
    Emitter, Manager, WindowEvent,
};

// 서비스별 비동기 작업 진행 상태 추적 (0: Idle, 1: Starting, 2: Stopping)
static PENDING_MYSQL: AtomicU8 = AtomicU8::new(0);
static PENDING_KAFKA: AtomicU8 = AtomicU8::new(0);
static PENDING_WS: AtomicU8 = AtomicU8::new(0);
static PENDING_SYNC: AtomicU8 = AtomicU8::new(0);
static PENDING_API: AtomicU8 = AtomicU8::new(0);

// 그룹 작업 진행 상태 추적
// 0: Idle, 1: StartingAll, 2: StoppingAll, 3: RestartingAll, 4: RestartingBackend, 5: RestartingInfra
static PENDING_GROUP: AtomicU8 = AtomicU8::new(0);

fn check_single_port(port: u16) -> bool {
    let addr = format!("127.0.0.1:{}", port);
    if let Ok(socket_addr) = addr.parse() {
        std::net::TcpStream::connect_timeout(&socket_addr, std::time::Duration::from_millis(250)).is_ok()
    } else {
        false
    }
}

fn check_all_service_ports() -> (bool, bool, bool, bool, bool, usize) {
    let mysql = check_single_port(13306);
    let kafka = check_single_port(19092);
    let ws = check_single_port(18080);
    let sync = check_single_port(18081);
    let api = check_single_port(18082);
    let mut count = 0;
    if mysql { count += 1; }
    if kafka { count += 1; }
    if ws { count += 1; }
    if sync { count += 1; }
    if api { count += 1; }
    (mysql, kafka, ws, sync, api, count)
}

fn update_tray_status_view(
    tray: &tauri::tray::TrayIcon,
    summary_item: &MenuItem<tauri::Wry>,
    mysql_item: &MenuItem<tauri::Wry>,
    kafka_item: &MenuItem<tauri::Wry>,
    ws_item: &MenuItem<tauri::Wry>,
    sync_item: &MenuItem<tauri::Wry>,
    api_item: &MenuItem<tauri::Wry>,
    start_all_item: &MenuItem<tauri::Wry>,
    stop_all_item: &MenuItem<tauri::Wry>,
    restart_all_item: &MenuItem<tauri::Wry>,
    restart_backend_item: &MenuItem<tauri::Wry>,
    restart_infra_item: &MenuItem<tauri::Wry>,
) {
    let (mysql, kafka, ws, sync, api, count) = check_all_service_ports();
    let infra_count = (if mysql { 1 } else { 0 }) + (if kafka { 1 } else { 0 });
    let core_count = (if ws { 1 } else { 0 }) + (if sync { 1 } else { 0 }) + (if api { 1 } else { 0 });

    let p_group = PENDING_GROUP.load(Ordering::Relaxed);

    // 1) 전체 요약 텍스트 (완전 기동/종료될 때까지 진행 상태 유지)
    let summary_text = match p_group {
        1 => {
            if count == 5 {
                PENDING_GROUP.store(0, Ordering::Relaxed);
                "● [ STATUS: 5/5 전체 정상 가동 중 ]".to_string()
            } else {
                format!("⏳ [ STATUS: 전체 순차 기동 중... ({}/5) ]", count)
            }
        }
        2 => {
            if count == 0 {
                PENDING_GROUP.store(0, Ordering::Relaxed);
                "○ [ STATUS: 0/5 모든 서비스 정지됨 ]".to_string()
            } else {
                format!("⏳ [ STATUS: 전체 순차 종료 중... ({}/5 잔여) ]", count)
            }
        }
        3 => {
            if count == 5 {
                PENDING_GROUP.store(0, Ordering::Relaxed);
                "● [ STATUS: 5/5 전체 정상 가동 중 ]".to_string()
            } else {
                format!("⏳ [ STATUS: 전체 재기동 중... ({}/5) ]", count)
            }
        }
        _ => {
            if count == 5 {
                "● [ STATUS: 5/5 전체 정상 가동 중 ]".to_string()
            } else if count > 0 {
                format!("◐ [ STATUS: {}/5 서비스 실행 중 ]", count)
            } else {
                "○ [ STATUS: 0/5 모든 서비스 정지됨 ]".to_string()
            }
        }
    };
    let _ = summary_item.set_text(summary_text);

    // 2) MySQL (완전히 뜰 때까지 기동중, 완전히 꺼질 때까지 종료중)
    let p_mysql = PENDING_MYSQL.load(Ordering::Relaxed);
    if p_mysql == 1 {
        if mysql {
            PENDING_MYSQL.store(0, Ordering::Relaxed);
            let _ = mysql_item.set_text("● [인프라] MySQL 9.71 (:13306) [ON]");
        } else {
            let _ = mysql_item.set_text("⏳ [인프라] MySQL 9.71 (:13306) [기동 중...]");
        }
    } else if p_mysql == 2 {
        if !mysql {
            PENDING_MYSQL.store(0, Ordering::Relaxed);
            let _ = mysql_item.set_text("○ [인프라] MySQL 9.71 (:13306) [OFF]");
        } else {
            let _ = mysql_item.set_text("⏳ [인프라] MySQL 9.71 (:13306) [종료 중...]");
        }
    } else {
        if mysql {
            let _ = mysql_item.set_text("● [인프라] MySQL 9.71 (:13306) [ON]");
        } else {
            let _ = mysql_item.set_text("○ [인프라] MySQL 9.71 (:13306) [OFF]");
        }
    }

    // 3) Kafka (완전히 뜰 때까지 기동중, 완전히 꺼질 때까지 종료중)
    let p_kafka = PENDING_KAFKA.load(Ordering::Relaxed);
    if p_kafka == 1 {
        if kafka {
            PENDING_KAFKA.store(0, Ordering::Relaxed);
            let _ = kafka_item.set_text("● [인프라] Apache Kafka (:19092) [ON]");
        } else {
            let _ = kafka_item.set_text("⏳ [인프라] Apache Kafka (:19092) [기동 중...]");
        }
    } else if p_kafka == 2 {
        if !kafka {
            PENDING_KAFKA.store(0, Ordering::Relaxed);
            let _ = kafka_item.set_text("○ [인프라] Apache Kafka (:19092) [OFF]");
        } else {
            let _ = kafka_item.set_text("⏳ [인프라] Apache Kafka (:19092) [종료 중...]");
        }
    } else {
        if kafka {
            let _ = kafka_item.set_text("● [인프라] Apache Kafka (:19092) [ON]");
        } else {
            let _ = kafka_item.set_text("○ [인프라] Apache Kafka (:19092) [OFF]");
        }
    }

    // 4) WS (완전히 뜰 때까지 기동중, 완전히 꺼질 때까지 종료중)
    let p_ws = PENDING_WS.load(Ordering::Relaxed);
    if p_ws == 1 {
        if ws {
            PENDING_WS.store(0, Ordering::Relaxed);
            let _ = ws_item.set_text("● [코어] elvis-connect-ws (:18080) [ON]");
        } else {
            let _ = ws_item.set_text("⏳ [코어] elvis-connect-ws (:18080) [기동 중...]");
        }
    } else if p_ws == 2 {
        if !ws {
            PENDING_WS.store(0, Ordering::Relaxed);
            let _ = ws_item.set_text("○ [코어] elvis-connect-ws (:18080) [OFF]");
        } else {
            let _ = ws_item.set_text("⏳ [코어] elvis-connect-ws (:18080) [종료 중...]");
        }
    } else {
        if ws {
            let _ = ws_item.set_text("● [코어] elvis-connect-ws (:18080) [ON]");
        } else {
            let _ = ws_item.set_text("○ [코어] elvis-connect-ws (:18080) [OFF]");
        }
    }

    // 5) Sync (완전히 뜰 때까지 기동중, 완전히 꺼질 때까지 종료중)
    let p_sync = PENDING_SYNC.load(Ordering::Relaxed);
    if p_sync == 1 {
        if sync {
            PENDING_SYNC.store(0, Ordering::Relaxed);
            let _ = sync_item.set_text("● [코어] elvis-connect-sync (:18081) [ON]");
        } else {
            let _ = sync_item.set_text("⏳ [코어] elvis-connect-sync (:18081) [기동 중...]");
        }
    } else if p_sync == 2 {
        if !sync {
            PENDING_SYNC.store(0, Ordering::Relaxed);
            let _ = sync_item.set_text("○ [코어] elvis-connect-sync (:18081) [OFF]");
        } else {
            let _ = sync_item.set_text("⏳ [코어] elvis-connect-sync (:18081) [종료 중...]");
        }
    } else {
        if sync {
            let _ = sync_item.set_text("● [코어] elvis-connect-sync (:18081) [ON]");
        } else {
            let _ = sync_item.set_text("○ [코어] elvis-connect-sync (:18081) [OFF]");
        }
    }

    // 6) API (완전히 뜰 때까지 기동중, 완전히 꺼질 때까지 종료중)
    let p_api = PENDING_API.load(Ordering::Relaxed);
    if p_api == 1 {
        if api {
            PENDING_API.store(0, Ordering::Relaxed);
            let _ = api_item.set_text("● [코어] elvis-control-api (:18082) [ON]");
        } else {
            let _ = api_item.set_text("⏳ [코어] elvis-control-api (:18082) [기동 중...]");
        }
    } else if p_api == 2 {
        if !api {
            PENDING_API.store(0, Ordering::Relaxed);
            let _ = api_item.set_text("○ [코어] elvis-control-api (:18082) [OFF]");
        } else {
            let _ = api_item.set_text("⏳ [코어] elvis-control-api (:18082) [종료 중...]");
        }
    } else {
        if api {
            let _ = api_item.set_text("● [코어] elvis-control-api (:18082) [ON]");
        } else {
            let _ = api_item.set_text("○ [코어] elvis-control-api (:18082) [OFF]");
        }
    }

    // 7) 그룹 메뉴 버튼 상태 및 텍스트 갱신
    match p_group {
        1 => {
            if count == 5 {
                PENDING_GROUP.store(0, Ordering::Relaxed);
                let _ = start_all_item.set_text("▶ 전체 서비스 시작");
                let _ = start_all_item.set_enabled(false);
                let _ = stop_all_item.set_enabled(true);
            } else {
                let _ = start_all_item.set_text(format!("⏳ 전체 서비스 시작 중... ({}/5)", count));
                let _ = start_all_item.set_enabled(false);
                let _ = stop_all_item.set_enabled(true);
            }
        }
        2 => {
            if count == 0 {
                PENDING_GROUP.store(0, Ordering::Relaxed);
                let _ = stop_all_item.set_text("■ 전체 서비스 종료");
                let _ = stop_all_item.set_enabled(false);
                let _ = start_all_item.set_enabled(true);
            } else {
                let _ = stop_all_item.set_text(format!("⏳ 전체 서비스 종료 중... ({}/5)", 5 - count));
                let _ = stop_all_item.set_enabled(false);
                let _ = start_all_item.set_enabled(true);
            }
        }
        3 => {
            if count == 5 {
                PENDING_GROUP.store(0, Ordering::Relaxed);
                let _ = restart_all_item.set_text("🔄 전체 재기동");
                let _ = restart_all_item.set_enabled(true);
            } else {
                let _ = restart_all_item.set_text(format!("⏳ 전체 재기동 중... ({}/5)", count));
                let _ = restart_all_item.set_enabled(false);
            }
        }
        4 => {
            if core_count == 3 {
                PENDING_GROUP.store(0, Ordering::Relaxed);
                let _ = restart_backend_item.set_text("⚡ 백엔드만 재기동 (WS/Sync/API)");
                let _ = restart_backend_item.set_enabled(true);
            } else {
                let _ = restart_backend_item.set_text(format!("⏳ 백엔드 재기동 중... ({}/3)", core_count));
                let _ = restart_backend_item.set_enabled(false);
            }
        }
        5 => {
            if infra_count == 2 {
                PENDING_GROUP.store(0, Ordering::Relaxed);
                let _ = restart_infra_item.set_text("📦 인프라만 재기동 (MySQL/Kafka)");
                let _ = restart_infra_item.set_enabled(true);
            } else {
                let _ = restart_infra_item.set_text(format!("⏳ 인프라 재기동 중... ({}/2)", infra_count));
                let _ = restart_infra_item.set_enabled(false);
            }
        }
        _ => {
            let _ = start_all_item.set_text("▶ 전체 서비스 시작");
            let _ = stop_all_item.set_text("■ 전체 서비스 종료");
            let _ = restart_all_item.set_text("🔄 전체 재기동");
            let _ = restart_backend_item.set_text("⚡ 백엔드만 재기동 (WS/Sync/API)");
            let _ = restart_infra_item.set_text("📦 인프라만 재기동 (MySQL/Kafka)");

            let _ = start_all_item.set_enabled(count < 5);
            let _ = stop_all_item.set_enabled(count > 0);
            let _ = restart_all_item.set_enabled(count > 0);
            let _ = restart_backend_item.set_enabled(core_count > 0);
            let _ = restart_infra_item.set_enabled(infra_count > 0);
        }
    }

    let tooltip = format!("ELVIS Launcher [5개 중 {}개 가동 중]", count);
    let _ = tray.set_tooltip(Some(tooltip));
}

#[tauri::command]
fn check_middleware_status(
    mysql_port: Option<u16>,
    kafka_port: Option<u16>,
    api_port: Option<u16>,
    ws_port: Option<u16>,
    sync_port: Option<u16>,
) -> serde_json::Value {
    let p_mysql = mysql_port.unwrap_or(13306);
    let p_kafka = kafka_port.unwrap_or(19092);
    let p_api = api_port.unwrap_or(18082);
    let p_ws = ws_port.unwrap_or(18080);
    let p_sync = sync_port.unwrap_or(18081);

    let kafka = check_single_port(p_kafka);
    let mysql = check_single_port(p_mysql);
    let api = check_single_port(p_api);
    let ws = check_single_port(p_ws);
    let sync = check_single_port(p_sync);

    serde_json::json!({
        "kafka": kafka,
        "mysql": mysql,
        "controlApi": api,
        "connectWs": ws,
        "connectSync": sync,
        "ports": {
            "mysql": p_mysql,
            "kafka": p_kafka,
            "api": p_api,
            "ws": p_ws,
            "sync": p_sync
        }
    })
}

fn find_script_file(script_file: &str) -> Result<std::path::PathBuf, String> {
    let fixed_path = std::path::PathBuf::from("D:\\project\\lselink\\ocpp-lite\\git\\h2y-ocpp\\scripts").join(script_file);
    if fixed_path.exists() {
        return Ok(fixed_path);
    }

    if let Ok(cur_dir) = std::env::current_dir() {
        for candidate in [
            cur_dir.join("scripts").join(script_file),
            cur_dir.join("../scripts").join(script_file),
            cur_dir.join("../../scripts").join(script_file),
        ] {
            if candidate.exists() {
                return Ok(candidate);
            }
        }
    }

    if let Ok(cur_exe) = std::env::current_exe() {
        for ancestor in cur_exe.ancestors().take(5) {
            let candidate = ancestor.join("scripts").join(script_file);
            if candidate.exists() {
                return Ok(candidate);
            }
        }
    }

    Err(format!("스크립트 파일을 찾을 수 없습니다: {}", script_file))
}

fn execute_bat_script(script_file: &str, service_name: &str) -> Result<String, String> {
    #[cfg(target_os = "windows")]
    {
        use std::os::windows::process::CommandExt;
        use std::process::Command;

        const CREATE_NO_WINDOW: u32 = 0x08000000;
        const CREATE_NEW_PROCESS_GROUP: u32 = 0x00000200;

        let target_bat = find_script_file(script_file)?;
        let cwd = target_bat.parent().unwrap();

        let log_dir = std::path::PathBuf::from("D:\\elvis-lite\\logs");
        if !log_dir.exists() {
            let _ = std::fs::create_dir_all(&log_dir);
        }
        let log_file_path = log_dir.join(format!("{}.log", service_name));

        let bat_str = target_bat.to_str().ok_or("배치 스크립트 경로 변환 실패")?;
        let log_str = log_file_path.to_str().ok_or("로그 경로 변환 실패")?;

        // 런처(vite.config.ts)의 무창 백그라운드 기동 방식과 100% 동일하게 구성:
        // cmd.exe /c start /b cmd /c "call \"bat_path\" >> \"log_path\" 2>&1"
        // Rust의 자동 따옴표 이스케이프 문제를 피하기 위해 Windows raw_arg 사용
        let raw_cmd = format!(
            "/c start /b cmd /c \"call \"{}\" >> \"{}\" 2>&1\"",
            bat_str, log_str
        );

        Command::new("cmd")
            .raw_arg(&raw_cmd)
            .current_dir(cwd)
            .env("NO_PAUSE", "1")
            .creation_flags(CREATE_NO_WINDOW | CREATE_NEW_PROCESS_GROUP)
            .spawn()
            .map_err(|e| format!("프로세스 기동 실패: {}", e))?;

        Ok(format!("{} 서비스가 백그라운드 독립 프로세스로 기동되었습니다.", service_name))
    }

    #[cfg(not(target_os = "windows"))]
    {
        Err("Windows OS에서만 지원됩니다.".to_string())
    }
}

fn start_all_services_sequence() {
    std::thread::spawn(|| {
        let _ = execute_bat_script("start-kafka-kraft.bat", "kafka");
        std::thread::sleep(std::time::Duration::from_millis(2500));
        let _ = execute_bat_script("start-mysql.bat", "mysql");
        std::thread::sleep(std::time::Duration::from_millis(3000));
        let _ = execute_bat_script("start-elvis-connect-ws.bat", "ws");
        let _ = execute_bat_script("start-elvis-connect-sync.bat", "sync");
        std::thread::sleep(std::time::Duration::from_millis(2500));
        let _ = execute_bat_script("start-elvis-control-api.bat", "api");
    });
}

fn stop_service_processes(service: &str) -> Result<String, String> {
    #[cfg(target_os = "windows")]
    {
        use std::process::Command;
        let cmd = match service {
            "kafka" => "Get-NetTCPConnection -LocalPort 19092,9092 -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /fi \"WINDOWTITLE eq ELVIS - Apache Kafka*\" 2>$null; Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -match 'kafka.Kafka' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }",
            "mysql" => "taskkill /f /im mysqld.exe 2>$null; Get-NetTCPConnection -LocalPort 13306,3306 -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /fi \"WINDOWTITLE eq ELVIS - MySQL*\" 2>$null",
            "ws" => "Get-NetTCPConnection -LocalPort 18080 -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /fi \"WINDOWTITLE eq ELVIS - Connect WS*\" 2>$null",
            "sync" => "Get-NetTCPConnection -LocalPort 18081 -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /fi \"WINDOWTITLE eq ELVIS - Connect Sync*\" 2>$null",
            "api" => "Get-NetTCPConnection -LocalPort 18082 -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /fi \"WINDOWTITLE eq ELVIS - Control API*\" 2>$null",
            "all-services" => "Get-NetTCPConnection -LocalPort 18080,18081,18082 -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /fi \"WINDOWTITLE eq ELVIS - Connect*\" 2>$null; taskkill /f /fi \"WINDOWTITLE eq ELVIS - Control*\" 2>$null",
            "all-infra" => "taskkill /f /im mysqld.exe 2>$null; Get-NetTCPConnection -LocalPort 13306,19092,3306,9092 -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /fi \"WINDOWTITLE eq ELVIS - Apache Kafka*\" 2>$null; taskkill /f /fi \"WINDOWTITLE eq ELVIS - MySQL*\" 2>$null; Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -match 'kafka.Kafka' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }",
            "all" => "taskkill /f /im mysqld.exe 2>$null; Get-NetTCPConnection -LocalPort 13306,19092,18080,18081,18082,3306,9092 -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /fi \"WINDOWTITLE eq ELVIS - Connect*\" 2>$null; taskkill /f /fi \"WINDOWTITLE eq ELVIS - Control*\" 2>$null; taskkill /f /fi \"WINDOWTITLE eq ELVIS - Apache*\" 2>$null; taskkill /f /fi \"WINDOWTITLE eq ELVIS - MySQL*\" 2>$null; Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -match 'kafka.Kafka' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }",
            _ => return Err("지원되지 않는 서비스입니다.".to_string()),
        };

        let _ = Command::new("powershell")
            .args(["-Command", cmd])
            .spawn()
            .map_err(|e| format!("중지 실패: {}", e))?;

        Ok(format!("{} 서비스가 중지되었습니다.", service))
    }

    #[cfg(not(target_os = "windows"))]
    {
        Err("Windows OS에서만 지원됩니다.".to_string())
    }
}

#[tauri::command]
fn start_middleware_detached(service: String) -> Result<String, String> {
    match service.as_str() {
        "all" => {
            start_all_services_sequence();
            Ok("전체 5대 서비스가 순차적으로 백그라운드 기동됩니다.".to_string())
        }
        "all-infra" => {
            std::thread::spawn(|| {
                let _ = execute_bat_script("start-kafka-kraft.bat", "kafka");
                std::thread::sleep(std::time::Duration::from_millis(2500));
                let _ = execute_bat_script("start-mysql.bat", "mysql");
            });
            Ok("인프라(Kafka, MySQL)가 순차적으로 백그라운드 기동됩니다.".to_string())
        }
        "all-services" => {
            std::thread::spawn(|| {
                let _ = execute_bat_script("start-elvis-connect-ws.bat", "ws");
                let _ = execute_bat_script("start-elvis-connect-sync.bat", "sync");
                std::thread::sleep(std::time::Duration::from_millis(2500));
                let _ = execute_bat_script("start-elvis-control-api.bat", "api");
            });
            Ok("코어 백엔드(WS, Sync, API)가 순차적으로 백그라운드 기동됩니다.".to_string())
        }
        "kafka" => execute_bat_script("start-kafka-kraft.bat", "kafka"),
        "mysql" => execute_bat_script("start-mysql.bat", "mysql"),
        "ws" => execute_bat_script("start-elvis-connect-ws.bat", "ws"),
        "sync" => execute_bat_script("start-elvis-connect-sync.bat", "sync"),
        "api" => execute_bat_script("start-elvis-control-api.bat", "api"),
        "tester" => execute_bat_script("start-all-tester.bat", "tester"),
        "topics" => execute_bat_script("create-kafka-topics.bat", "topics"),
        _ => Err("알 수 없는 서비스 이름입니다.".to_string()),
    }
}

#[tauri::command]
fn stop_middleware_detached(service: String) -> Result<String, String> {
    stop_service_processes(&service)
}

#[tauri::command]
fn open_log_window(service: Option<String>) -> Result<String, String> {
    #[cfg(target_os = "windows")]
    {
        use std::process::Command;
        let svc = service.unwrap_or_else(|| "mysql".to_string());

        let (log_file_name, display_name) = match svc.as_str() {
            "mysql" => ("mysql.log", "MySQL 9.71 (:13306)"),
            "kafka" => ("kafka.log", "Apache Kafka (:19092)"),
            "ws" => ("ws.log", "elvis-connect-ws (:18080)"),
            "sync" => ("sync.log", "elvis-connect-sync (:18081)"),
            "api" => ("api.log", "elvis-control-api (:18082)"),
            "tester" => ("tester.log", "elvis-tester (:8085)"),
            _ => ("mysql.log", "서비스"),
        };

        let log_dir = "D:\\elvis-lite\\logs";
        let full_log_path = format!("{}\\{}", log_dir, log_file_name);
        let win_title = format!("ELVIS - {} 실시간 콘솔 로그", display_name);

        // 1) 기존 동일 서비스 로그창이 이미 열려 있는지 검사 -> 활성화(Focus) 성공 시 중복 생성 방지
        let check_script = format!(
            "$w = New-Object -ComObject WScript.Shell; \
             if ($w.AppActivate('{title}')) {{ exit 10 }}; \
             $p = Get-Process -ErrorAction SilentlyContinue | Where-Object {{ $_.MainWindowTitle -like '*{title}*' }}; \
             if ($p) {{ if ($w.AppActivate($p[0].Id)) {{ exit 10 }} }}; \
             exit 0",
            title = win_title
        );

        if let Ok(st) = Command::new("powershell")
            .args(["-NoProfile", "-NonInteractive", "-Command", &check_script])
            .status()
        {
            if st.code() == Some(10) {
                return Ok(format!("기존 {} 실시간 콘솔 창을 활성화했습니다.", display_name));
            }
        }

        // 2) 열려 있지 않은 경우 단독 신규 창 기동
        let ps_cmd = format!(
            "[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; \
             chcp 65001 >$null; \
             $host.UI.RawUI.WindowTitle = '{title}'; \
             Clear-Host; \
             Write-Host '========================================================================================================' -ForegroundColor DarkCyan; \
             Write-Host '  ELVIS CSMS Lite - {display_name} 실시간 콘솔 로그' -ForegroundColor Cyan; \
             Write-Host '  로그 경로: {path}' -ForegroundColor DarkGray; \
             Write-Host '  (종료: 창 닫기 또는 Ctrl+C)' -ForegroundColor DarkGray; \
             Write-Host '========================================================================================================' -ForegroundColor DarkCyan; \
             Write-Host ''; \
             if (Test-Path '{path}') {{ \
                 Get-Content -Path '{path}' -Tail 150 -Wait -Encoding utf8 \
             }} else {{ \
                 Write-Host '[안내] 아직 {log_file_name} 파일이 생성되지 않았습니다.' -ForegroundColor Yellow; \
                 Write-Host '서비스를 기동하면 실시간 로그가 자동으로 기록됩니다.' -ForegroundColor Gray; \
                 Write-Host '파일 생성을 대기하는 중... (Ctrl+C로 종료)' -ForegroundColor DarkGray; \
                 while (-not (Test-Path '{path}')) {{ Start-Sleep -Seconds 2 }}; \
                 Clear-Host; \
                 Write-Host '[연결됨] {log_file_name} 로그 감시를 시작합니다.' -ForegroundColor Green; \
                 Get-Content -Path '{path}' -Tail 150 -Wait -Encoding utf8 \
             }}",
            title = win_title,
            display_name = display_name,
            path = full_log_path,
            log_file_name = log_file_name
        );

        Command::new("cmd")
            .args(["/c", "start", &win_title, "powershell", "-NoExit", "-Command", &ps_cmd])
            .spawn()
            .map_err(|e| format!("PowerShell 콘솔 기동 실패: {}", e))?;

        Ok(format!("{} 서비스의 실시간 터미널 콘솔이 열렸습니다.", svc))
    }
    #[cfg(not(target_os = "windows"))]
    {
        Err("Windows OS에서만 지원됩니다.".to_string())
    }
}

#[tauri::command]
fn open_process_status_window(target: Option<String>) -> Result<String, String> {
    #[cfg(target_os = "windows")]
    {
        let win_title = "ELVIS - 프로세스 및 포트 점유 실시간 진단";

        // 기존 진단창이 열려 있으면 포커스 활성화 (중복 기동 방지)
        let check_script = format!(
            "$w = New-Object -ComObject WScript.Shell; \
             if ($w.AppActivate('{title}')) {{ exit 10 }}; \
             $p = Get-Process -ErrorAction SilentlyContinue | Where-Object {{ $_.MainWindowTitle -like '*{title}*' }}; \
             if ($p) {{ if ($w.AppActivate($p[0].Id)) {{ exit 10 }} }}; \
             exit 0",
            title = win_title
        );

        if let Ok(st) = Command::new("powershell")
            .args(["-NoProfile", "-NonInteractive", "-Command", &check_script])
            .status()
        {
            if st.code() == Some(10) {
                return Ok("기존 프로세스 진단 터미널을 활성화했습니다.".to_string());
            }
        }

        let target_bat = find_script_file("check-status.bat")?;
        let target_arg = target.unwrap_or_else(|| "all".to_string());
        let cwd = target_bat.parent().unwrap();
        Command::new("cmd")
            .args(["/c", "start", win_title, target_bat.to_str().unwrap(), &target_arg])
            .current_dir(cwd)
            .spawn()
            .map_err(|e| format!("프로세스 진단창 실행 실패: {}", e))?;

        Ok("프로세스 및 포트 실시간 진단 터미널이 열렸습니다.".to_string())
    }
    #[cfg(not(target_os = "windows"))]
    {
        Err("Windows OS에서만 지원됩니다.".to_string())
    }
}

#[tauri::command]
fn show_main_window(app: tauri::AppHandle) -> Result<(), String> {
    if let Some(win) = app.get_webview_window("main") {
        let _ = win.show();
        let _ = win.set_focus();
    }
    Ok(())
}

fn trigger_ui_command(app: &tauri::AppHandle, action: &str, service: &str) {
    if let Some(window) = app.get_webview_window("main") {
        let js = format!(
            "window.__triggerTrayCommand && window.__triggerTrayCommand('{}', '{}');",
            action, service
        );
        let _ = window.eval(&js);
    }
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_shell::init())
        // 메인 창 닫기(X) 요청 시 완전 종료 대신 시스템 트레이로 숨기기 (상주)
        .on_window_event(|window, event| {
            if let WindowEvent::CloseRequested { api, .. } = event {
                api.prevent_close();
                let _ = window.hide();
            }
        })
        .setup(|app| {
            #[cfg(debug_assertions)]
            {
                if let Some(window) = app.get_webview_window("main") {
                    let _ = window.open_devtools();
                }
            }

            // 시스템 트레이 메뉴 아이템 정의
            let summary_i = MenuItem::with_id(app, "status_summary", "● [ STATUS: 점검 중... ]", false, None::<&str>)?;
            let sep1 = PredefinedMenuItem::separator(app)?;

            // 1. 인프라 미들웨어 상태 (클릭 시 단독 기동/종료 토글)
            let mysql_i = MenuItem::with_id(app, "status_mysql", "○ [인프라] MySQL 9.71 (:13306) [OFF]", true, None::<&str>)?;
            let kafka_i = MenuItem::with_id(app, "status_kafka", "○ [인프라] Apache Kafka (:19092) [OFF]", true, None::<&str>)?;
            let sep_infra = PredefinedMenuItem::separator(app)?;

            // 2. 코어 백엔드 엔진 상태 (클릭 시 단독 기동/종료 토글)
            let ws_i = MenuItem::with_id(app, "status_ws", "○ [코어] elvis-connect-ws (:18080) [OFF]", true, None::<&str>)?;
            let sync_i = MenuItem::with_id(app, "status_sync", "○ [코어] elvis-connect-sync (:18081) [OFF]", true, None::<&str>)?;
            let api_i = MenuItem::with_id(app, "status_api", "○ [코어] elvis-control-api (:18082) [OFF]", true, None::<&str>)?;

            let sep2 = PredefinedMenuItem::separator(app)?;

            // 3. 일괄 제어 명령
            let start_all_i = MenuItem::with_id(app, "start_all", "▶ 전체 서비스 시작", true, None::<&str>)?;
            let stop_all_i = MenuItem::with_id(app, "stop_all", "■ 전체 서비스 종료", true, None::<&str>)?;
            let restart_all_i = MenuItem::with_id(app, "restart_all", "🔄 전체 재기동", true, None::<&str>)?;
            let restart_backend_i = MenuItem::with_id(app, "restart_backend", "⚡ 백엔드만 재기동 (WS/Sync/API)", true, None::<&str>)?;
            let restart_infra_i = MenuItem::with_id(app, "restart_infra", "📦 인프라만 재기동 (MySQL/Kafka)", true, None::<&str>)?;

            let sep3 = PredefinedMenuItem::separator(app)?;

            // 4. 진단 도구 및 창 제어
            let check_proc_i = MenuItem::with_id(app, "check_proc", "🔍 프로세스 & 포트 점유 진단", true, None::<&str>)?;
            let show_i = MenuItem::with_id(app, "show", "런처 창 열기", true, None::<&str>)?;

            let sep4 = PredefinedMenuItem::separator(app)?;

            // 5. 종료
            let quit_i = MenuItem::with_id(app, "quit", "런처 종료", true, None::<&str>)?;

            let menu = Menu::with_items(
                app,
                &[
                    &summary_i,
                    &sep1,
                    &mysql_i,
                    &kafka_i,
                    &sep_infra,
                    &ws_i,
                    &sync_i,
                    &api_i,
                    &sep2,
                    &start_all_i,
                    &stop_all_i,
                    &restart_all_i,
                    &restart_backend_i,
                    &restart_infra_i,
                    &sep3,
                    &check_proc_i,
                    &show_i,
                    &sep4,
                    &quit_i,
                ],
            )?;

            // 시스템 트레이 아이콘 등록
            if let Some(icon) = app.default_window_icon() {
                let m_summary = summary_i.clone();
                let m_mysql = mysql_i.clone();
                let m_kafka = kafka_i.clone();
                let m_ws = ws_i.clone();
                let m_sync = sync_i.clone();
                let m_api = api_i.clone();
                let m_start_all = start_all_i.clone();
                let m_stop_all = stop_all_i.clone();
                let m_restart_all = restart_all_i.clone();
                let m_restart_backend = restart_backend_i.clone();
                let m_restart_infra = restart_infra_i.clone();

                let tray = TrayIconBuilder::new()
                    .icon(icon.clone())
                    .tooltip("ELVIS Launcher [상태 점검 중...]")
                    .menu(&menu)
                    .on_menu_event(move |app, event| match event.id.as_ref() {
                        "show" => {
                            if let Some(window) = app.get_webview_window("main") {
                                let _ = window.show();
                                let _ = window.set_focus();
                            }
                        }
                        "check_proc" => {
                            let _ = open_process_status_window(None);
                        }
                        "status_mysql" => {
                            let (mysql, ..) = check_all_service_ports();
                            if mysql {
                                PENDING_MYSQL.store(2, Ordering::Relaxed);
                                let _ = m_mysql.set_text("⏳ [인프라] MySQL 9.71 (:13306) [종료 중...]");
                                trigger_ui_command(app, "stop", "mysql");
                            } else {
                                PENDING_MYSQL.store(1, Ordering::Relaxed);
                                let _ = m_mysql.set_text("⏳ [인프라] MySQL 9.71 (:13306) [기동 중...]");
                                trigger_ui_command(app, "start", "mysql");
                            }
                        }
                        "status_kafka" => {
                            let (_, kafka, ..) = check_all_service_ports();
                            if kafka {
                                PENDING_KAFKA.store(2, Ordering::Relaxed);
                                let _ = m_kafka.set_text("⏳ [인프라] Apache Kafka (:19092) [종료 중...]");
                                trigger_ui_command(app, "stop", "kafka");
                            } else {
                                PENDING_KAFKA.store(1, Ordering::Relaxed);
                                let _ = m_kafka.set_text("⏳ [인프라] Apache Kafka (:19092) [기동 중...]");
                                trigger_ui_command(app, "start", "kafka");
                            }
                        }
                        "status_ws" => {
                            let (_, _, ws, ..) = check_all_service_ports();
                            if ws {
                                PENDING_WS.store(2, Ordering::Relaxed);
                                let _ = m_ws.set_text("⏳ [코어] elvis-connect-ws (:18080) [종료 중...]");
                                trigger_ui_command(app, "stop", "ws");
                            } else {
                                PENDING_WS.store(1, Ordering::Relaxed);
                                let _ = m_ws.set_text("⏳ [코어] elvis-connect-ws (:18080) [기동 중...]");
                                trigger_ui_command(app, "start", "ws");
                            }
                        }
                        "status_sync" => {
                            let (_, _, _, sync, ..) = check_all_service_ports();
                            if sync {
                                PENDING_SYNC.store(2, Ordering::Relaxed);
                                let _ = m_sync.set_text("⏳ [코어] elvis-connect-sync (:18081) [종료 중...]");
                                trigger_ui_command(app, "stop", "sync");
                            } else {
                                PENDING_SYNC.store(1, Ordering::Relaxed);
                                let _ = m_sync.set_text("⏳ [코어] elvis-connect-sync (:18081) [기동 중...]");
                                trigger_ui_command(app, "start", "sync");
                            }
                        }
                        "status_api" => {
                            let (_, _, _, _, api, ..) = check_all_service_ports();
                            if api {
                                PENDING_API.store(2, Ordering::Relaxed);
                                let _ = m_api.set_text("⏳ [코어] elvis-control-api (:18082) [종료 중...]");
                                trigger_ui_command(app, "stop", "api");
                            } else {
                                PENDING_API.store(1, Ordering::Relaxed);
                                let _ = m_api.set_text("⏳ [코어] elvis-control-api (:18082) [기동 중...]");
                                trigger_ui_command(app, "start", "api");
                            }
                        }
                        "start_all" => {
                            PENDING_GROUP.store(1, Ordering::Relaxed);
                            PENDING_MYSQL.store(1, Ordering::Relaxed);
                            PENDING_KAFKA.store(1, Ordering::Relaxed);
                            PENDING_WS.store(1, Ordering::Relaxed);
                            PENDING_SYNC.store(1, Ordering::Relaxed);
                            PENDING_API.store(1, Ordering::Relaxed);
                            let _ = m_summary.set_text("⏳ [ STATUS: 전체 서비스 순차 기동 중... ]");
                            let _ = m_start_all.set_text("⏳ 전체 서비스 시작 중...");
                            trigger_ui_command(app, "start_all", "");
                        }
                        "stop_all" => {
                            PENDING_GROUP.store(2, Ordering::Relaxed);
                            PENDING_MYSQL.store(2, Ordering::Relaxed);
                            PENDING_KAFKA.store(2, Ordering::Relaxed);
                            PENDING_WS.store(2, Ordering::Relaxed);
                            PENDING_SYNC.store(2, Ordering::Relaxed);
                            PENDING_API.store(2, Ordering::Relaxed);
                            let _ = m_summary.set_text("⏳ [ STATUS: 전체 서비스 종료 중... ]");
                            let _ = m_stop_all.set_text("⏳ 전체 서비스 종료 중...");
                            trigger_ui_command(app, "stop_all", "");
                        }
                        "restart_all" => {
                            PENDING_GROUP.store(3, Ordering::Relaxed);
                            PENDING_MYSQL.store(1, Ordering::Relaxed);
                            PENDING_KAFKA.store(1, Ordering::Relaxed);
                            PENDING_WS.store(1, Ordering::Relaxed);
                            PENDING_SYNC.store(1, Ordering::Relaxed);
                            PENDING_API.store(1, Ordering::Relaxed);
                            let _ = m_summary.set_text("⏳ [ STATUS: 전체 서비스 재기동 중... ]");
                            let _ = m_restart_all.set_text("⏳ 전체 재기동 중...");
                            trigger_ui_command(app, "restart_all", "");
                        }
                        "restart_backend" => {
                            PENDING_GROUP.store(4, Ordering::Relaxed);
                            PENDING_WS.store(1, Ordering::Relaxed);
                            PENDING_SYNC.store(1, Ordering::Relaxed);
                            PENDING_API.store(1, Ordering::Relaxed);
                            let _ = m_restart_backend.set_text("⏳ 백엔드 재기동 중...");
                            trigger_ui_command(app, "restart_backend", "");
                        }
                        "restart_infra" => {
                            PENDING_GROUP.store(5, Ordering::Relaxed);
                            PENDING_MYSQL.store(1, Ordering::Relaxed);
                            PENDING_KAFKA.store(1, Ordering::Relaxed);
                            let _ = m_restart_infra.set_text("⏳ 인프라 재기동 중...");
                            trigger_ui_command(app, "restart_infra", "");
                        }
                        "quit" => {
                            app.exit(0);
                        }
                        _ => {}
                    })
                    .on_tray_icon_event(|tray, event| {
                        // 트레이 아이콘 좌클릭 시 메인 런처 창 표시
                        if let TrayIconEvent::Click {
                            button: MouseButton::Left,
                            button_state: MouseButtonState::Up,
                            ..
                        } = event
                        {
                            let app = tray.app_handle();
                            if let Some(window) = app.get_webview_window("main") {
                                let _ = window.show();
                                let _ = window.set_focus();
                            }
                        }
                    })
                    .build(app)?;

                // 초기 트레이 메뉴 상태 즉시 갱신
                update_tray_status_view(
                    &tray,
                    &summary_i,
                    &mysql_i,
                    &kafka_i,
                    &ws_i,
                    &sync_i,
                    &api_i,
                    &start_all_i,
                    &stop_all_i,
                    &restart_all_i,
                    &restart_backend_i,
                    &restart_infra_i,
                );

                // 백그라운드 3초 주기 자동 감시 스레드 기동
                let t_tray = tray.clone();
                let t_summary = summary_i.clone();
                let t_mysql = mysql_i.clone();
                let t_kafka = kafka_i.clone();
                let t_ws = ws_i.clone();
                let t_sync = sync_i.clone();
                let t_api = api_i.clone();
                let t_start_all = start_all_i.clone();
                let t_stop_all = stop_all_i.clone();
                let t_restart_all = restart_all_i.clone();
                let t_restart_backend = restart_backend_i.clone();
                let t_restart_infra = restart_infra_i.clone();

                std::thread::spawn(move || {
                    loop {
                        std::thread::sleep(std::time::Duration::from_secs(3));
                        update_tray_status_view(
                            &t_tray,
                            &t_summary,
                            &t_mysql,
                            &t_kafka,
                            &t_ws,
                            &t_sync,
                            &t_api,
                            &t_start_all,
                            &t_stop_all,
                            &t_restart_all,
                            &t_restart_backend,
                            &t_restart_infra,
                        );
                    }
                });
            }

            Ok(())
        })
        .invoke_handler(tauri::generate_handler![
            check_middleware_status,
            start_middleware_detached,
            stop_middleware_detached,
            open_log_window,
            open_process_status_window,
            show_main_window
        ])
        .run(tauri::generate_context!())
        .expect("error while running ELVIS launcher application");
}
