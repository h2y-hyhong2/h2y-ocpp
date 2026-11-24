use std::path::PathBuf;
use tauri::{
    menu::{Menu, MenuItem},
    tray::{MouseButton, MouseButtonState, TrayIconBuilder, TrayIconEvent},
    Manager, WindowEvent,
};

struct MiddlewarePorts {
    mysql: u16,
    kafka: u16,
    api: u16,
}

fn find_relative_dir(subpath: &str) -> Option<PathBuf> {
    let mut base_dirs: Vec<PathBuf> = Vec::new();
    if let Ok(cwd) = std::env::current_dir() {
        base_dirs.push(cwd.clone());
        if let Some(p1) = cwd.parent() {
            base_dirs.push(p1.to_path_buf());
            if let Some(p2) = p1.parent() {
                base_dirs.push(p2.to_path_buf());
            }
        }
    }
    if let Ok(exe) = std::env::current_exe() {
        if let Some(exe_dir) = exe.parent() {
            base_dirs.push(exe_dir.to_path_buf());
            if let Some(p1) = exe_dir.parent() {
                base_dirs.push(p1.to_path_buf());
                if let Some(p2) = p1.parent() {
                    base_dirs.push(p2.to_path_buf());
                }
            }
        }
    }
    if let Ok(ws) = std::env::var("ELVIS_WORKSPACE_DIR") {
        let p = PathBuf::from(ws);
        if p.exists() {
            base_dirs.push(p);
        }
    }
    // D:\elvis-control-ui.exe 단독 실행 시에도 빌드 시점 워크스페이스(config/, scripts/) 자동 탐색
    let manifest_dir = PathBuf::from(env!("CARGO_MANIFEST_DIR"));
    if let Some(ws_root) = manifest_dir.parent().and_then(|p| p.parent()) {
        base_dirs.push(ws_root.to_path_buf());
    }

    base_dirs
        .into_iter()
        .map(|d| d.join(subpath))
        .find(|p| p.exists())
}

fn read_ports_from_env() -> MiddlewarePorts {
    let mut ports = MiddlewarePorts {
        mysql: 13306,
        kafka: 19092,
        api: 9082,
    };

    if let Some(env_path) = find_relative_dir("config/paths.env") {
        if let Ok(content) = std::fs::read_to_string(env_path) {
            for line in content.lines() {
                let trimmed = line.trim();
                if let Some(val) = trimmed.strip_prefix("ELVIS_MYSQL_PORT=") {
                    if let Ok(p) = val.trim().parse::<u16>() {
                        ports.mysql = p;
                    }
                } else if let Some(val) = trimmed.strip_prefix("ELVIS_KAFKA_PORT=") {
                    if let Ok(p) = val.trim().parse::<u16>() {
                        ports.kafka = p;
                    }
                } else if let Some(val) = trimmed.strip_prefix("ELVIS_API_PORT=") {
                    if let Ok(p) = val.trim().parse::<u16>() {
                        ports.api = p;
                    }
                }
            }
        }
    }

    ports
}

#[tauri::command]
fn check_middleware_status(mysql_port: Option<u16>, kafka_port: Option<u16>, api_port: Option<u16>) -> serde_json::Value {
    let check_port = |port: u16| -> bool {
        let addr = format!("127.0.0.1:{}", port);
        if let Ok(socket_addr) = addr.parse() {
            std::net::TcpStream::connect_timeout(&socket_addr, std::time::Duration::from_millis(300)).is_ok()
        } else {
            false
        }
    };

    let defaults = read_ports_from_env();
    let p_mysql = mysql_port.unwrap_or(defaults.mysql);
    let p_kafka = kafka_port.unwrap_or(defaults.kafka);
    let p_api = api_port.unwrap_or(defaults.api);

    let kafka = check_port(p_kafka);
    let mysql = check_port(p_mysql);
    let api = check_port(p_api);

    serde_json::json!({
        "kafka": kafka,
        "mysql": mysql,
        "api": api,
        "ports": {
            "mysql": p_mysql,
            "kafka": p_kafka,
            "api": p_api
        }
    })
}

#[tauri::command]
fn start_middleware_detached(service: String) -> Result<String, String> {
    #[cfg(target_os = "windows")]
    {
        use std::os::windows::process::CommandExt;
        use std::process::Command;

        const DETACHED_PROCESS: u32 = 0x00000008;
        const CREATE_NEW_PROCESS_GROUP: u32 = 0x00000200;

        let script_file = match service.as_str() {
            "kafka" => "start-kafka-kraft.bat",
            "mysql" => "start-mysql.bat",
            "all" => "start-all-middleware.bat",
            "topics" => "create-kafka-topics.bat",
            _ => return Err("알 수 없는 서비스 이름입니다.".to_string()),
        };

        let scripts_dir = std::env::var("ELVIS_SCRIPTS_DIR")
            .ok()
            .map(PathBuf::from)
            .filter(|p| p.exists())
            .or_else(|| find_relative_dir("scripts"))
            .ok_or_else(|| format!("scripts 디렉터리를 찾을 수 없습니다: {}", script_file))?;

        let target_bat = scripts_dir.join(script_file);
        if !target_bat.exists() {
            return Err(format!("스크립트 파일을 찾을 수 없습니다: {}", script_file));
        }

        let cwd = target_bat.parent().unwrap_or(&scripts_dir);
        let title = format!("ELVIS-{}", service);
        Command::new("cmd")
            .args(["/c", "start", &title, target_bat.to_string_lossy().as_ref()])
            .current_dir(cwd)
            .creation_flags(DETACHED_PROCESS | CREATE_NEW_PROCESS_GROUP)
            .spawn()
            .map_err(|e| format!("프로세스 기동 실패: {}", e))?;

        Ok(format!("{} 서비스가 백그라운드 독립 프로세스로 기동되었습니다.", service))
    }

    #[cfg(not(target_os = "windows"))]
    {
        let _ = service;
        Err("Windows OS에서만 지원됩니다.".to_string())
    }
}

#[tauri::command]
fn stop_middleware_detached(service: String) -> Result<String, String> {
    #[cfg(target_os = "windows")]
    {
        use std::process::Command;
        let ports = read_ports_from_env();
        let cmd = match service.as_str() {
            "kafka" => format!(
                "Get-NetTCPConnection -LocalPort {} -ErrorAction SilentlyContinue | ForEach-Object {{ Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }}; taskkill /f /fi \"WINDOWTITLE eq ELVIS - Apache Kafka*\" 2>$null",
                ports.kafka
            ),
            "mysql" => format!(
                "taskkill /f /im mysqld.exe 2>$null; Get-NetTCPConnection -LocalPort {} -ErrorAction SilentlyContinue | ForEach-Object {{ Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }}",
                ports.mysql
            ),
            _ => return Err("지원되지 않는 중지 서비스입니다.".to_string()),
        };

        let _ = Command::new("powershell")
            .args(["-Command", &cmd])
            .spawn()
            .map_err(|e| format!("중지 실패: {}", e))?;

        Ok(format!("{} 서비스가 중지되었습니다.", service))
    }

    #[cfg(not(target_os = "windows"))]
    {
        let _ = service;
        Err("Windows OS에서만 지원됩니다.".to_string())
    }
}

fn should_minimize_to_tray() -> bool {
    matches!(
        std::env::var("ELVIS_MINIMIZE_TO_TRAY").as_deref(),
        Ok("1") | Ok("true") | Ok("TRUE")
    )
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_shell::init())
        .on_window_event(|window, event| {
            if let WindowEvent::CloseRequested { api, .. } = event {
                if should_minimize_to_tray() {
                    api.prevent_close();
                    let _ = window.hide();
                }
            }
        })
        .setup(|app| {
            // 시스템 트레이 메뉴 생성
            let quit_i = MenuItem::with_id(app, "quit", "관제 센터 완전히 종료", true, None::<&str>)?;
            let show_i = MenuItem::with_id(app, "show", "관제 화면 열기", true, None::<&str>)?;
            let menu = Menu::with_items(app, &[&show_i, &quit_i])?;

            // 트레이 아이콘 설정
            if let Some(icon) = app.default_window_icon() {
                let _tray = TrayIconBuilder::new()
                    .icon(icon.clone())
                    .tooltip("ELVIS-CSMS 관제 센터 (백그라운드 실행 중)")
                    .menu(&menu)
                    .on_menu_event(|app, event| match event.id.as_ref() {
                        "quit" => {
                            app.exit(0);
                        }
                        "show" => {
                            if let Some(window) = app.get_webview_window("main") {
                                let _ = window.show();
                                let _ = window.set_focus();
                            }
                        }
                        _ => {}
                    })
                    .on_tray_icon_event(|tray, event| {
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
            }

            Ok(())
        })
        .invoke_handler(tauri::generate_handler![
            check_middleware_status,
            start_middleware_detached,
            stop_middleware_detached
        ])
        .run(tauri::generate_context!())
        .expect("error while running ELVIS-CSMS desktop application");
}
