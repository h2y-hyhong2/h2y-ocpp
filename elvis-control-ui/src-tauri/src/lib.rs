use tauri::{
    menu::{Menu, MenuItem},
    tray::{MouseButton, MouseButtonState, TrayIconBuilder, TrayIconEvent},
    Manager, WindowEvent,
};

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

    let p_mysql = mysql_port.unwrap_or(13306);
    let p_kafka = kafka_port.unwrap_or(19092);
    let p_api = api_port.unwrap_or(18081);

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

        let title = format!("ELVIS-{}", service);
        Command::new("cmd")
            .args(["/c", "start", &title, target_bat.to_str().unwrap()])
            .current_dir(cwd)
            .creation_flags(DETACHED_PROCESS | CREATE_NEW_PROCESS_GROUP)
            .spawn()
            .map_err(|e| format!("프로세스 기동 실패: {}", e))?;

        Ok(format!("{} 서비스가 백그라운드 독립 프로세스로 기동되었습니다.", service))
    }

    #[cfg(not(target_os = "windows"))]
    {
        Err("Windows OS에서만 지원됩니다.".to_string())
    }
}

#[tauri::command]
fn stop_middleware_detached(service: String) -> Result<String, String> {
    #[cfg(target_os = "windows")]
    {
        use std::process::Command;
        let cmd = match service.as_str() {
            "kafka" => "Get-NetTCPConnection -LocalPort 9092 -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /fi \"WINDOWTITLE eq ELVIS - Apache Kafka*\" 2>$null",
            "mysql" => "taskkill /f /im mysqld.exe & powershell -Command \"Get-NetTCPConnection -LocalPort 3306 -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }\"",
            _ => return Err("지원되지 않는 중지 서비스입니다.".to_string()),
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

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_shell::init())
        // 창 닫기(X) 요청 시 완전 종료 대신 시스템 트레이로 숨기기
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
