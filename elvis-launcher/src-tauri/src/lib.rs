use tauri::{
    menu::{Menu, MenuItem, PredefinedMenuItem},
    tray::{MouseButton, MouseButtonState, TrayIconBuilder, TrayIconEvent},
    Manager, WindowEvent,
};

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
    let sync = check_single_port(8082);
    let api = check_single_port(18088);
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
) {
    let (mysql, kafka, ws, sync, api, count) = check_all_service_ports();

    let summary_text = if count == 5 {
        "● [ STATUS: 5/5 전체 정상 가동 중 ]".to_string()
    } else if count > 0 {
        format!("◐ [ STATUS: {}/5 서비스 실행 중 ]", count)
    } else {
        "○ [ STATUS: 0/5 모든 서비스 정지됨 ]".to_string()
    };
    let _ = summary_item.set_text(summary_text);

    let _ = mysql_item.set_text(if mysql {
        "[ON]  ● MySQL 9.71 (:13306)"
    } else {
        "[OFF] ○ MySQL 9.71 (:13306)"
    });

    let _ = kafka_item.set_text(if kafka {
        "[ON]  ● Apache Kafka (:19092)"
    } else {
        "[OFF] ○ Apache Kafka (:19092)"
    });

    let _ = ws_item.set_text(if ws {
        "[ON]  ● elvis-connect-ws (:18080)"
    } else {
        "[OFF] ○ elvis-connect-ws (:18080)"
    });

    let _ = sync_item.set_text(if sync {
        "[ON]  ● elvis-connect-sync (:8082)"
    } else {
        "[OFF] ○ elvis-connect-sync (:8082)"
    });

    let _ = api_item.set_text(if api {
        "[ON]  ● elvis-control-api (:18088)"
    } else {
        "[OFF] ○ elvis-control-api (:18088)"
    });

    let tooltip = format!(
        "ELVIS Launcher [5개 중 {}개 가동 중]",
        count
    );
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
    let p_api = api_port.unwrap_or(18088);
    let p_ws = ws_port.unwrap_or(18080);
    let p_sync = sync_port.unwrap_or(8082);

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

fn execute_bat_script(script_file: &str, service_name: &str) -> Result<String, String> {
    #[cfg(target_os = "windows")]
    {
        use std::os::windows::process::CommandExt;
        use std::process::Command;

        const DETACHED_PROCESS: u32 = 0x00000008;
        const CREATE_NEW_PROCESS_GROUP: u32 = 0x00000200;

        let script_path = std::env::current_dir().unwrap_or_default();
        let candidates = [
            script_path.join("../scripts").join(script_file),
            script_path.join("scripts").join(script_file),
            script_path.join("../../scripts").join(script_file),
            std::path::PathBuf::from("D:\\project\\lselink\\ocpp-lite\\git\\h2y-ocpp\\scripts").join(script_file),
        ];

        let target_bat = candidates
            .into_iter()
            .find(|p| p.exists())
            .ok_or_else(|| format!("스크립트 파일을 찾을 수 없습니다: {}", script_file))?;

        let cwd = target_bat.parent().unwrap();

        let title = format!("ELVIS-LAUNCHER-{}", service_name);
        Command::new("cmd")
            .args(["/c", "start", &title, target_bat.to_str().unwrap()])
            .current_dir(cwd)
            .creation_flags(DETACHED_PROCESS | CREATE_NEW_PROCESS_GROUP)
            .spawn()
            .map_err(|e| format!("프로세스 기동 실패: {}", e))?;

        Ok(format!("{} 서비스가 백그라운드 독립 프로세스로 기동되었습니다.", service_name))
    }

    #[cfg(not(target_os = "windows"))]
    {
        Err("Windows OS에서만 지원됩니다.".to_string())
    }
}

fn stop_service_processes(service: &str) -> Result<String, String> {
    #[cfg(target_os = "windows")]
    {
        use std::process::Command;
        let cmd = match service {
            "kafka" => "Get-NetTCPConnection -LocalPort 19092,9092 -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /fi \"WINDOWTITLE eq ELVIS - Apache Kafka*\" 2>$null; Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -match 'kafka.Kafka' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }",
            "mysql" => "taskkill /f /im mysqld.exe 2>$null; Get-NetTCPConnection -LocalPort 13306,3306 -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /fi \"WINDOWTITLE eq ELVIS - MySQL*\" 2>$null",
            "ws" => "Get-NetTCPConnection -LocalPort 18080 -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /fi \"WINDOWTITLE eq ELVIS - Connect WS*\" 2>$null",
            "sync" => "Get-NetTCPConnection -LocalPort 8082 -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /fi \"WINDOWTITLE eq ELVIS - Connect Sync*\" 2>$null",
            "api" => "Get-NetTCPConnection -LocalPort 18088 -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /fi \"WINDOWTITLE eq ELVIS - Control API*\" 2>$null",
            "all-services" => "Get-NetTCPConnection -LocalPort 18080,8082,18088 -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /fi \"WINDOWTITLE eq ELVIS - Connect*\" 2>$null; taskkill /f /fi \"WINDOWTITLE eq ELVIS - Control*\" 2>$null",
            "all-infra" => "taskkill /f /im mysqld.exe 2>$null; Get-NetTCPConnection -LocalPort 13306,19092,3306,9092 -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /fi \"WINDOWTITLE eq ELVIS - Apache Kafka*\" 2>$null; taskkill /f /fi \"WINDOWTITLE eq ELVIS - MySQL*\" 2>$null; Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -match 'kafka.Kafka' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }",
            "all" => "taskkill /f /im mysqld.exe 2>$null; Get-NetTCPConnection -LocalPort 13306,19092,18080,8082,18088,3306,9092 -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /fi \"WINDOWTITLE eq ELVIS - Connect*\" 2>$null; taskkill /f /fi \"WINDOWTITLE eq ELVIS - Control*\" 2>$null; taskkill /f /fi \"WINDOWTITLE eq ELVIS - Apache*\" 2>$null; taskkill /f /fi \"WINDOWTITLE eq ELVIS - MySQL*\" 2>$null; Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -match 'kafka.Kafka' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }",
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
    let script = match service.as_str() {
        "kafka" => "start-kafka-kraft.bat",
        "mysql" => "start-mysql.bat",
        "ws" => "start-elvis-connect-ws.bat",
        "sync" => "start-elvis-connect-sync.bat",
        "api" => "start-elvis-control-api.bat",
        "tester" => "start-elvis-tester.bat",
        "all-infra" => "start-all-middleware.bat",
        "all" => "start-all-middleware.bat",
        "topics" => "create-kafka-topics.bat",
        _ => return Err("알 수 없는 서비스 이름입니다.".to_string()),
    };
    execute_bat_script(script, &service)
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
            "sync" => ("sync.log", "elvis-connect-sync (:8082)"),
            "api" => ("api.log", "elvis-control-api (:18088)"),
            "tester" => ("tester.log", "elvis-tester (:8085)"),
            _ => ("mysql.log", "서비스"),
        };

        let log_dir = "D:\\elvis-lite\\logs";
        let full_log_path = format!("{}\\{}", log_dir, log_file_name);
        let win_title = format!("ELVIS - {} 실시간 콘솔 로그", display_name);

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
        use std::process::Command;
        let script_path = std::path::PathBuf::from("D:\\project\\lselink\\ocpp-lite\\git\\h2y-ocpp\\scripts\\check-status.bat");
        let target_bat = if script_path.exists() {
            script_path
        } else {
            std::env::current_dir().unwrap_or_default().join("../scripts/check-status.bat")
        };

        if !target_bat.exists() {
            return Err("check-status.bat 스크립트 파일을 찾을 수 없습니다.".to_string());
        }

        let target_arg = target.unwrap_or_else(|| "all".to_string());
        let cwd = target_bat.parent().unwrap();
        Command::new("cmd")
            .args(["/c", "start", "ELVIS - 프로세스 및 포트 점유 실시간 진단", target_bat.to_str().unwrap(), &target_arg])
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

            let mysql_i = MenuItem::with_id(app, "status_mysql", "[OFF] ○ MySQL 9.71 (:13306)", true, None::<&str>)?;
            let kafka_i = MenuItem::with_id(app, "status_kafka", "[OFF] ○ Apache Kafka (:19092)", true, None::<&str>)?;
            let ws_i = MenuItem::with_id(app, "status_ws", "[OFF] ○ elvis-connect-ws (:18080)", true, None::<&str>)?;
            let sync_i = MenuItem::with_id(app, "status_sync", "[OFF] ○ elvis-connect-sync (:8082)", true, None::<&str>)?;
            let api_i = MenuItem::with_id(app, "status_api", "[OFF] ○ elvis-control-api (:18088)", true, None::<&str>)?;

            let sep2 = PredefinedMenuItem::separator(app)?;
            let start_all_i = MenuItem::with_id(app, "start_all", "⚡ 전체 서비스 시작", true, None::<&str>)?;
            let stop_all_i = MenuItem::with_id(app, "stop_all", "🛑 전체 서비스 종료", true, None::<&str>)?;
            let restart_all_i = MenuItem::with_id(app, "restart_all", "🔄 전체 서비스 재기동", true, None::<&str>)?;

            let sep3 = PredefinedMenuItem::separator(app)?;
            let show_i = MenuItem::with_id(app, "show", "🚀 런처 제어판 열기", true, None::<&str>)?;
            let check_proc_i = MenuItem::with_id(app, "check_proc", "🔍 프로세스 & 포트 진단 터미널", true, None::<&str>)?;
            let open_ui_i = MenuItem::with_id(app, "open_control_ui", "📊 CSMS 관제 UI (:1420)", true, None::<&str>)?;

            let sep4 = PredefinedMenuItem::separator(app)?;
            let quit_i = MenuItem::with_id(app, "quit", "🚪 런처 완전히 종료", true, None::<&str>)?;

            let menu = Menu::with_items(
                app,
                &[
                    &summary_i,
                    &sep1,
                    &mysql_i,
                    &kafka_i,
                    &ws_i,
                    &sync_i,
                    &api_i,
                    &sep2,
                    &start_all_i,
                    &stop_all_i,
                    &restart_all_i,
                    &sep3,
                    &show_i,
                    &check_proc_i,
                    &open_ui_i,
                    &sep4,
                    &quit_i,
                ],
            )?;

            // 시스템 트레이 아이콘 등록
            if let Some(icon) = app.default_window_icon() {
                let tray = TrayIconBuilder::new()
                    .icon(icon.clone())
                    .tooltip("ELVIS Launcher [상태 점검 중...]")
                    .menu(&menu)
                    .on_menu_event(|app, event| match event.id.as_ref() {
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
                                let _ = stop_service_processes("mysql");
                            } else {
                                let _ = execute_bat_script("start-mysql.bat", "mysql");
                            }
                        }
                        "status_kafka" => {
                            let (_, kafka, ..) = check_all_service_ports();
                            if kafka {
                                let _ = stop_service_processes("kafka");
                            } else {
                                let _ = execute_bat_script("start-kafka-kraft.bat", "kafka");
                            }
                        }
                        "status_ws" => {
                            let (_, _, ws, ..) = check_all_service_ports();
                            if ws {
                                let _ = stop_service_processes("ws");
                            } else {
                                let _ = execute_bat_script("start-elvis-connect-ws.bat", "ws");
                            }
                        }
                        "status_sync" => {
                            let (_, _, _, sync, ..) = check_all_service_ports();
                            if sync {
                                let _ = stop_service_processes("sync");
                            } else {
                                let _ = execute_bat_script("start-elvis-connect-sync.bat", "sync");
                            }
                        }
                        "status_api" => {
                            let (_, _, _, _, api, ..) = check_all_service_ports();
                            if api {
                                let _ = stop_service_processes("api");
                            } else {
                                let _ = execute_bat_script("start-elvis-control-api.bat", "api");
                            }
                        }
                        "start_all" => {
                            let _ = execute_bat_script("start-all-middleware.bat", "all");
                        }
                        "stop_all" => {
                            let _ = stop_service_processes("all");
                        }
                        "restart_all" => {
                            let _ = stop_service_processes("all");
                            std::thread::sleep(std::time::Duration::from_millis(1500));
                            let _ = execute_bat_script("start-all-middleware.bat", "all");
                        }
                        "open_control_ui" => {
                            #[cfg(target_os = "windows")]
                            {
                                use std::process::Command;
                                let _ = Command::new("cmd").args(["/c", "start", "http://localhost:1420"]).spawn();
                            }
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
                );

                // 백그라운드 3초 주기 자동 감시 스레드 기동
                let t_tray = tray.clone();
                let t_summary = summary_i.clone();
                let t_mysql = mysql_i.clone();
                let t_kafka = kafka_i.clone();
                let t_ws = ws_i.clone();
                let t_sync = sync_i.clone();
                let t_api = api_i.clone();

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
