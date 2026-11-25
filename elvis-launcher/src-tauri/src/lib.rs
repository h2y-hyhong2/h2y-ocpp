use std::path::PathBuf;
use std::process::Command;
use std::sync::atomic::{AtomicU64, AtomicU8, Ordering};
use std::time::{SystemTime, UNIX_EPOCH};
use tauri::{
    menu::{Menu, MenuItem, PredefinedMenuItem},
    tray::{MouseButton, MouseButtonState, TrayIconBuilder, TrayIconEvent},
    Manager, WindowEvent,
};

// 서비스별 비동기 작업 진행 상태 추적
// 0: Idle, 1: Starting, 2: Stopping, 3: RestartStopping, 4: RestartStarting
static PENDING_MYSQL: AtomicU8 = AtomicU8::new(0);
static PENDING_KAFKA: AtomicU8 = AtomicU8::new(0);
static PENDING_WS: AtomicU8 = AtomicU8::new(0);
static PENDING_SYNC: AtomicU8 = AtomicU8::new(0);
static PENDING_API: AtomicU8 = AtomicU8::new(0);

// 그룹 작업 진행 상태 추적
// 0: Idle
// 1: StartingAll, 2: StoppingAll
// 3: RestartingAll_Stopping, 4: RestartingBackend_Stopping, 5: RestartingInfra_Stopping
// 6: RestartingAll_Starting, 7: RestartingBackend_Starting, 8: RestartingInfra_Starting
static PENDING_GROUP: AtomicU8 = AtomicU8::new(0);
static PENDING_STARTED_AT: AtomicU64 = AtomicU64::new(0);

fn now_epoch_secs() -> u64 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|d| d.as_secs())
        .unwrap_or(0)
}

fn mark_pending_started() {
    PENDING_STARTED_AT.store(now_epoch_secs(), Ordering::Relaxed);
}

#[tauri::command]
fn reset_tray_pending() {
    PENDING_GROUP.store(0, Ordering::Relaxed);
    PENDING_MYSQL.store(0, Ordering::Relaxed);
    PENDING_KAFKA.store(0, Ordering::Relaxed);
    PENDING_WS.store(0, Ordering::Relaxed);
    PENDING_SYNC.store(0, Ordering::Relaxed);
    PENDING_API.store(0, Ordering::Relaxed);
    PENDING_STARTED_AT.store(0, Ordering::Relaxed);
}

const RELEASE_PRODUCT_ROOT: &str = r"D:\elvis-lite";

fn default_product_root() -> String {
    #[cfg(debug_assertions)]
    {
        // 개발 환경(debug/dev): 워크스페이스 루트(elvis-launcher/src-tauri 의 조부모) 자동 사용
        let manifest_dir = PathBuf::from(env!("CARGO_MANIFEST_DIR"));
        if let Some(ws_root) = manifest_dir.parent().and_then(|p| p.parent()) {
            if ws_root.is_dir() {
                return ws_root.to_string_lossy().to_string();
            }
        }
    }
    RELEASE_PRODUCT_ROOT.to_string()
}

#[derive(serde::Serialize, serde::Deserialize, Default)]
struct LauncherSettings {
    #[serde(rename = "productRoot")]
    product_root: Option<String>,
}

fn normalize_product_root(raw: &str) -> String {
    let cleaned = raw.trim().trim_matches('"').replace('/', "\\");
    if cleaned.is_empty() {
        return default_product_root();
    }
    // 드라이브 루트(예: D:\)가 아닌 경우 끝의 역슬래시 제거
    let bytes = cleaned.as_bytes();
    if bytes.len() == 3 && bytes[1] == b':' && bytes[2] == b'\\' {
        return cleaned;
    }
    let trimmed = cleaned.trim_end_matches('\\');
    if trimmed.len() == 2 && trimmed.as_bytes()[1] == b':' {
        format!("{}\\", trimmed)
    } else if trimmed.is_empty() {
        default_product_root()
    } else {
        trimmed.to_string()
    }
}

fn settings_file_path() -> PathBuf {
    let file_name = if cfg!(debug_assertions) {
        "settings-dev.json"
    } else {
        "settings.json"
    };
    if let Ok(appdata) = std::env::var("APPDATA") {
        let trimmed = appdata.trim();
        if !trimmed.is_empty() {
            return PathBuf::from(trimmed)
                .join("com.lselink.elvis.launcher")
                .join(file_name);
        }
    }
    if let Ok(exe) = std::env::current_exe() {
        if let Some(dir) = exe.parent() {
            return dir.join(file_name);
        }
    }
    PathBuf::from(default_product_root()).join(file_name)
}

fn load_product_root() -> PathBuf {
    let settings_path = settings_file_path();
    if let Ok(content) = std::fs::read_to_string(&settings_path) {
        if let Ok(parsed) = serde_json::from_str::<LauncherSettings>(&content) {
            if let Some(root) = parsed.product_root {
                let norm = normalize_product_root(&root);
                if !norm.is_empty() {
                    return PathBuf::from(norm);
                }
            }
        }
    }
    if let Ok(env_home) = std::env::var("ELVIS_HOME") {
        let norm = normalize_product_root(&env_home);
        if !norm.is_empty() {
            return PathBuf::from(norm);
        }
    }
    PathBuf::from(default_product_root())
}

fn save_product_root(raw: &str) -> Result<String, String> {
    let normalized = normalize_product_root(raw);
    let settings_path = settings_file_path();
    if let Some(parent) = settings_path.parent() {
        std::fs::create_dir_all(parent)
            .map_err(|e| format!("설정 디렉터리 생성 실패: {}", e))?;
    }
    let settings = LauncherSettings {
        product_root: Some(normalized.clone()),
    };
    let json = serde_json::to_string_pretty(&settings)
        .map_err(|e| format!("설정 직렬화 실패: {}", e))?;
    std::fs::write(&settings_path, json)
        .map_err(|e| format!("설정 파일 저장 실패: {}", e))?;
    Ok(normalized)
}

struct InstallSnapshot {
    root: PathBuf,
    root_exists: bool,
    mysql: bool,
    kafka: bool,
    ws: bool,
    sync: bool,
    api: bool,
    missing_mysql: Vec<String>,
    missing_kafka: Vec<String>,
    missing_ws: Vec<String>,
    missing_sync: Vec<String>,
    missing_api: Vec<String>,
}

fn find_backend_jar(root: &std::path::Path, module_name: &str) -> Option<PathBuf> {
    let apps_dir = root.join("apps");
    let exact = apps_dir.join(format!("{}.jar", module_name));
    if exact.is_file() {
        return Some(exact);
    }
    let prefix = format!("{}-", module_name);
    if let Ok(entries) = std::fs::read_dir(&apps_dir) {
        for entry in entries.flatten() {
            let path = entry.path();
            if !path.is_file() {
                continue;
            }
            if let Some(name) = path.file_name().and_then(|n| n.to_str()) {
                let lower = name.to_ascii_lowercase();
                if lower.starts_with(&prefix)
                    && lower.ends_with(".jar")
                    && !lower.ends_with("-plain.jar")
                {
                    return Some(path);
                }
            }
        }
    }
    None
}

fn has_backend_runtime(root: &std::path::Path, module_name: &str) -> bool {
    if find_backend_jar(root, module_name).is_some() {
        return true;
    }
    // 개발 환경(debug): Gradle 워크스페이스 bootRun 폴백 지원
    #[cfg(debug_assertions)]
    {
        if root.join("build.gradle").is_file()
            && root.join(module_name).join("build.gradle").is_file()
        {
            return true;
        }
    }
    false
}

fn check_install_snapshot(root: &std::path::Path) -> InstallSnapshot {
    let root_exists = root.is_dir();
    let has_daemon_ps1 = root.join("scripts").join("run-daemon.ps1").is_file();
    let has_daemon_vbs = root.join("scripts").join("run-daemon.vbs").is_file();

    let push_daemon_missing = |missing: &mut Vec<String>| {
        if !has_daemon_ps1 {
            missing.push("scripts\\run-daemon.ps1".to_string());
        }
        if !has_daemon_vbs {
            missing.push("scripts\\run-daemon.vbs".to_string());
        }
    };

    let mut missing_mysql = Vec::new();
    if !root
        .join("binaries")
        .join("mysql-9.7.1-winx64")
        .join("bin")
        .join("mysqld.exe")
        .is_file()
    {
        missing_mysql.push("binaries\\mysql-9.7.1-winx64\\bin\\mysqld.exe".to_string());
    }
    if !root.join("scripts").join("start-mysql.bat").is_file() {
        missing_mysql.push("scripts\\start-mysql.bat".to_string());
    }
    push_daemon_missing(&mut missing_mysql);

    let mut missing_kafka = Vec::new();
    if !root
        .join("binaries")
        .join("kafka_2.13-4.3.1")
        .join("bin")
        .join("windows")
        .join("kafka-server-start.bat")
        .is_file()
    {
        missing_kafka.push(
            "binaries\\kafka_2.13-4.3.1\\bin\\windows\\kafka-server-start.bat".to_string(),
        );
    }
    if !root.join("scripts").join("start-kafka-kraft.bat").is_file() {
        missing_kafka.push("scripts\\start-kafka-kraft.bat".to_string());
    }
    push_daemon_missing(&mut missing_kafka);

    let mut missing_ws = Vec::new();
    if !has_backend_runtime(root, "elvis-connect-ws") {
        missing_ws.push("apps\\elvis-connect-ws.jar".to_string());
    }
    if !root
        .join("scripts")
        .join("start-elvis-connect-ws.bat")
        .is_file()
    {
        missing_ws.push("scripts\\start-elvis-connect-ws.bat".to_string());
    }
    push_daemon_missing(&mut missing_ws);

    let mut missing_sync = Vec::new();
    if !has_backend_runtime(root, "elvis-connect-sync") {
        missing_sync.push("apps\\elvis-connect-sync.jar".to_string());
    }
    if !root
        .join("scripts")
        .join("start-elvis-connect-sync.bat")
        .is_file()
    {
        missing_sync.push("scripts\\start-elvis-connect-sync.bat".to_string());
    }
    push_daemon_missing(&mut missing_sync);

    let mut missing_api = Vec::new();
    if !has_backend_runtime(root, "elvis-control-api") {
        missing_api.push("apps\\elvis-control-api.jar".to_string());
    }
    if !root
        .join("scripts")
        .join("start-elvis-control-api.bat")
        .is_file()
    {
        missing_api.push("scripts\\start-elvis-control-api.bat".to_string());
    }
    push_daemon_missing(&mut missing_api);

    InstallSnapshot {
        root: root.to_path_buf(),
        root_exists,
        mysql: missing_mysql.is_empty(),
        kafka: missing_kafka.is_empty(),
        ws: missing_ws.is_empty(),
        sync: missing_sync.is_empty(),
        api: missing_api.is_empty(),
        missing_mysql,
        missing_kafka,
        missing_ws,
        missing_sync,
        missing_api,
    }
}

impl InstallSnapshot {
    fn installed_count(&self) -> usize {
        [self.mysql, self.kafka, self.ws, self.sync, self.api]
            .into_iter()
            .filter(|v| *v)
            .count()
    }

    fn infra_installed_count(&self) -> usize {
        (if self.mysql { 1 } else { 0 }) + (if self.kafka { 1 } else { 0 })
    }

    fn core_installed_count(&self) -> usize {
        (if self.ws { 1 } else { 0 })
            + (if self.sync { 1 } else { 0 })
            + (if self.api { 1 } else { 0 })
    }

    fn installed_json(&self) -> serde_json::Value {
        serde_json::json!({
            "mysql": self.mysql,
            "kafka": self.kafka,
            "connectWs": self.ws,
            "connectSync": self.sync,
            "controlApi": self.api
        })
    }

    fn missing_json(&self) -> serde_json::Value {
        serde_json::json!({
            "mysql": self.missing_mysql,
            "kafka": self.missing_kafka,
            "connectWs": self.missing_ws,
            "connectSync": self.missing_sync,
            "controlApi": self.missing_api
        })
    }
}

struct EnvConfig {
    product_root: PathBuf,
    log_dir: String,
    mysql_port: u16,
    kafka_port: u16,
    ws_port: u16,
    sync_port: u16,
    api_port: u16,
    tester_port: u16,
}

impl EnvConfig {
    fn for_root(root: PathBuf) -> Self {
        let log_dir = root.join("logs").to_string_lossy().to_string();
        Self {
            product_root: root,
            log_dir,
            mysql_port: 13306,
            kafka_port: 19092,
            ws_port: 9080,
            sync_port: 9081,
            api_port: 9082,
            tester_port: 9085,
        }
    }
}

fn find_paths_env_file(root: &std::path::Path) -> Option<PathBuf> {
    let candidate = root.join("config").join("paths.env");
    if candidate.is_file() {
        return Some(candidate);
    }
    None
}

fn load_env_config() -> EnvConfig {
    let root = load_product_root();
    let root_str = root.to_string_lossy().to_string();
    let mut cfg = EnvConfig::for_root(root.clone());
    if let Some(env_path) = find_paths_env_file(&root) {
        if let Ok(content) = std::fs::read_to_string(env_path) {
            for line in content.lines() {
                let trimmed = line.trim();
                if trimmed.is_empty() || trimmed.starts_with('#') {
                    continue;
                }
                if let Some((k, v)) = trimmed.split_once('=') {
                    let key = k.trim();
                    let val = v.trim();
                    if val.is_empty() {
                        continue;
                    }
                    match key {
                        "ELVIS_LOG_DIR" => {
                            cfg.log_dir = val.replace("%ELVIS_HOME%", &root_str);
                        }
                        "ELVIS_MYSQL_PORT" => {
                            if let Ok(p) = val.parse::<u16>() {
                                cfg.mysql_port = p;
                            }
                        }
                        "ELVIS_KAFKA_PORT" => {
                            if let Ok(p) = val.parse::<u16>() {
                                cfg.kafka_port = p;
                            }
                        }
                        "ELVIS_WS_PORT" => {
                            if let Ok(p) = val.parse::<u16>() {
                                cfg.ws_port = p;
                            }
                        }
                        "ELVIS_SYNC_PORT" => {
                            if let Ok(p) = val.parse::<u16>() {
                                cfg.sync_port = p;
                            }
                        }
                        "ELVIS_API_PORT" => {
                            if let Ok(p) = val.parse::<u16>() {
                                cfg.api_port = p;
                            }
                        }
                        "ELVIS_TESTER_PORT" => {
                            if let Ok(p) = val.parse::<u16>() {
                                cfg.tester_port = p;
                            }
                        }
                        _ => {}
                    }
                }
            }
        }
    }
    cfg
}

fn escape_ps_single_quoted(value: &str) -> String {
    value.replace('\'', "''")
}

fn sanitize_cmd_title(value: &str) -> String {
    value
        .chars()
        .map(|c| match c {
            '"' | '&' | '|' | '<' | '>' | '^' | '%' | '\r' | '\n' => ' ',
            _ => c,
        })
        .collect::<String>()
        .trim()
        .to_string()
}

fn encode_ps_base64(script: &str) -> String {
    const TABLE: &[u8; 64] = b"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    let mut bytes = Vec::with_capacity(script.len() * 2);
    for unit in script.encode_utf16() {
        bytes.extend_from_slice(&unit.to_le_bytes());
    }
    let mut out = String::with_capacity((bytes.len() + 2) / 3 * 4);
    for chunk in bytes.chunks(3) {
        let b0 = chunk[0] as u32;
        let b1 = if chunk.len() > 1 { chunk[1] as u32 } else { 0 };
        let b2 = if chunk.len() > 2 { chunk[2] as u32 } else { 0 };
        let n = (b0 << 16) | (b1 << 8) | b2;
        out.push(TABLE[((n >> 18) & 0x3F) as usize] as char);
        out.push(TABLE[((n >> 12) & 0x3F) as usize] as char);
        out.push(if chunk.len() > 1 {
            TABLE[((n >> 6) & 0x3F) as usize] as char
        } else {
            '='
        });
        out.push(if chunk.len() > 2 {
            TABLE[(n & 0x3F) as usize] as char
        } else {
            '='
        });
    }
    out
}

fn check_single_port(port: u16) -> bool {
    let timeout = std::time::Duration::from_millis(250);
    [format!("127.0.0.1:{}", port), format!("[::1]:{}", port)]
        .into_iter()
        .any(|addr| {
            addr.parse::<std::net::SocketAddr>()
                .map(|socket_addr| std::net::TcpStream::connect_timeout(&socket_addr, timeout).is_ok())
                .unwrap_or(false)
        })
}

fn check_all_service_ports(env: &EnvConfig) -> (bool, bool, bool, bool, bool, usize) {
    let mysql = check_single_port(env.mysql_port);
    let kafka = check_single_port(env.kafka_port);
    let ws = check_single_port(env.ws_port);
    let sync = check_single_port(env.sync_port);
    let api = check_single_port(env.api_port);
    let mut count = 0;
    if mysql { count += 1; }
    if kafka { count += 1; }
    if ws { count += 1; }
    if sync { count += 1; }
    if api { count += 1; }
    (mysql, kafka, ws, sync, api, count)
}

fn update_service_menu_label(
    item: &MenuItem<tauri::Wry>,
    pending: &AtomicU8,
    is_up: bool,
    is_installed: bool,
    label_prefix: &str,
) {
    if !is_installed && !is_up {
        pending.store(0, Ordering::Relaxed);
        let _ = item.set_text(format!("○ {} [미설치]", label_prefix));
        let _ = item.set_enabled(false);
        return;
    }
    let _ = item.set_enabled(true);

    let state = pending.load(Ordering::Relaxed);
    match state {
        1 => {
            if is_up {
                pending.store(0, Ordering::Relaxed);
                let _ = item.set_text(format!("● {} [ON]", label_prefix));
            } else {
                let _ = item.set_text(format!("[진행] {} [기동 중...]", label_prefix));
            }
        }
        2 => {
            if !is_up {
                pending.store(0, Ordering::Relaxed);
                let _ = item.set_text(format!("○ {} [OFF]", label_prefix));
            } else {
                let _ = item.set_text(format!("[진행] {} [종료 중...]", label_prefix));
            }
        }
        3 => {
            if !is_up {
                pending.store(4, Ordering::Relaxed);
                let _ = item.set_text(format!("[진행] {} [재기동: 기동 대기]", label_prefix));
            } else {
                let _ = item.set_text(format!("[진행] {} [재기동: 종료 중]", label_prefix));
            }
        }
        4 => {
            if is_up {
                pending.store(0, Ordering::Relaxed);
                let _ = item.set_text(format!("● {} [ON]", label_prefix));
            } else {
                let _ = item.set_text(format!("[진행] {} [재기동: 기동 중]", label_prefix));
            }
        }
        _ => {
            if is_up {
                let _ = item.set_text(format!("● {} [ON]", label_prefix));
            } else {
                let _ = item.set_text(format!("○ {} [OFF]", label_prefix));
            }
        }
    }
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
    let env = load_env_config();
    let install = check_install_snapshot(&env.product_root);
    let installed_total = install.installed_count();
    let infra_installed = install.infra_installed_count();
    let core_installed = install.core_installed_count();
    let uninstalled_total = 5usize.saturating_sub(installed_total);

    let (mysql, kafka, ws, sync, api, count) = check_all_service_ports(&env);
    let infra_count = (if mysql { 1 } else { 0 }) + (if kafka { 1 } else { 0 });
    let core_count = (if ws { 1 } else { 0 }) + (if sync { 1 } else { 0 }) + (if api { 1 } else { 0 });

    let started_at = PENDING_STARTED_AT.load(Ordering::Relaxed);
    let elapsed = if started_at > 0 {
        now_epoch_secs().saturating_sub(started_at)
    } else {
        0
    };

    // 장시간(90초 초과) 미해결 상태가 고착되는 것을 방지하는 안전 타임아웃
    if started_at > 0 && elapsed > 90 {
        reset_tray_pending();
    }

    let mut p_group = PENDING_GROUP.load(Ordering::Relaxed);

    // 2단계 재기동 상태 전이: 종료 완료 확인(또는 12초 경과) 시 기동 단계로 전이
    if p_group == 3 && (count == 0 || elapsed >= 12) {
        PENDING_GROUP.store(6, Ordering::Relaxed);
        p_group = 6;
    } else if p_group == 4 && (core_count == 0 || elapsed >= 12) {
        PENDING_GROUP.store(7, Ordering::Relaxed);
        p_group = 7;
    } else if p_group == 5 && (infra_count == 0 || elapsed >= 12) {
        PENDING_GROUP.store(8, Ordering::Relaxed);
        p_group = 8;
    }

    // 1) 전체 요약 텍스트
    let target_total = if installed_total > 0 { installed_total } else { 5 };
    let summary_text = if installed_total == 0 && count == 0 {
        PENDING_GROUP.store(0, Ordering::Relaxed);
        format!("○ [ STATUS: 제품 미설치 ({}) ]", install.root.to_string_lossy())
    } else {
        match p_group {
            1 => {
                if count >= target_total {
                    PENDING_GROUP.store(0, Ordering::Relaxed);
                    if uninstalled_total > 0 {
                        format!("● [ STATUS: {}/{} 가동 중 · {}개 미설치 ]", count, installed_total, uninstalled_total)
                    } else {
                        "● [ STATUS: 5/5 전체 정상 가동 중 ]".to_string()
                    }
                } else {
                    format!("[진행] [ STATUS: 순차 기동 중... ({}/{}) ]", count, target_total)
                }
            }
            2 => {
                if count == 0 {
                    PENDING_GROUP.store(0, Ordering::Relaxed);
                    format!("○ [ STATUS: 0/{} 모든 서비스 정지됨 ]", target_total)
                } else {
                    format!("[진행] [ STATUS: 순차 종료 중... ({}/{} 잔여) ]", count, target_total)
                }
            }
            3 => format!("[진행] [ STATUS: 전체 재기동(종료 중)... ({}/{} 잔여) ]", count, target_total),
            6 => {
                if count >= target_total {
                    PENDING_GROUP.store(0, Ordering::Relaxed);
                    format!("● [ STATUS: {}/{} 정상 가동 중 ]", count, target_total)
                } else {
                    format!("[진행] [ STATUS: 전체 재기동(기동 중)... ({}/{}) ]", count, target_total)
                }
            }
            4 | 7 | 5 | 8 => format!("[진행] [ STATUS: 선택 그룹 재기동 중... ({}/{}) ]", count, target_total),
            _ => {
                if uninstalled_total > 0 {
                    format!("◐ [ STATUS: {}/{} 가동 중 · {}개 미설치 ]", count, installed_total, uninstalled_total)
                } else if count == 5 {
                    "● [ STATUS: 5/5 전체 정상 가동 중 ]".to_string()
                } else if count > 0 {
                    format!("◐ [ STATUS: {}/5 서비스 실행 중 ]", count)
                } else {
                    "○ [ STATUS: 0/5 모든 서비스 정지됨 ]".to_string()
                }
            }
        }
    };
    let _ = summary_item.set_text(summary_text);

    // 2~6) 개별 서비스 메뉴 텍스트 갱신 (동적 포트 + 설치 여부 반영)
    update_service_menu_label(
        mysql_item,
        &PENDING_MYSQL,
        mysql,
        install.mysql,
        &format!("[인프라] MySQL 9.71 (:{})", env.mysql_port),
    );
    update_service_menu_label(
        kafka_item,
        &PENDING_KAFKA,
        kafka,
        install.kafka,
        &format!("[인프라] Apache Kafka (:{})", env.kafka_port),
    );
    update_service_menu_label(
        ws_item,
        &PENDING_WS,
        ws,
        install.ws,
        &format!("[코어] elvis-connect-ws (:{})", env.ws_port),
    );
    update_service_menu_label(
        sync_item,
        &PENDING_SYNC,
        sync,
        install.sync,
        &format!("[코어] elvis-connect-sync (:{})", env.sync_port),
    );
    update_service_menu_label(
        api_item,
        &PENDING_API,
        api,
        install.api,
        &format!("[코어] elvis-control-api (:{})", env.api_port),
    );

    // 7) 그룹 메뉴 버튼 상태 및 텍스트 갱신
    p_group = PENDING_GROUP.load(Ordering::Relaxed);
    match p_group {
        1 => {
            if count >= target_total {
                PENDING_GROUP.store(0, Ordering::Relaxed);
                let _ = start_all_item.set_text("▶ 전체 서비스 시작");
                let _ = start_all_item.set_enabled(false);
                let _ = stop_all_item.set_enabled(true);
            } else {
                let _ = start_all_item.set_text(format!("[진행] 전체 서비스 시작 중... ({}/{})", count, target_total));
                let _ = start_all_item.set_enabled(false);
                let _ = stop_all_item.set_enabled(true);
            }
        }
        2 => {
            if count == 0 {
                PENDING_GROUP.store(0, Ordering::Relaxed);
                let _ = stop_all_item.set_text("■ 전체 서비스 종료");
                let _ = stop_all_item.set_enabled(false);
                let _ = start_all_item.set_enabled(installed_total > 0);
            } else {
                let _ = stop_all_item.set_text(format!("[진행] 전체 서비스 종료 중... ({}/{})", target_total.saturating_sub(count), target_total));
                let _ = stop_all_item.set_enabled(false);
                let _ = start_all_item.set_enabled(installed_total > 0);
            }
        }
        3 => {
            let _ = restart_all_item.set_text(format!("[진행] 전체 재기동(종료)... ({}/{} 잔여)", count, target_total));
            let _ = restart_all_item.set_enabled(false);
        }
        6 => {
            if count >= target_total {
                PENDING_GROUP.store(0, Ordering::Relaxed);
                let _ = restart_all_item.set_text("전체 재기동");
                let _ = restart_all_item.set_enabled(installed_total > 0);
            } else {
                let _ = restart_all_item.set_text(format!("[진행] 전체 재기동(기동)... ({}/{})", count, target_total));
                let _ = restart_all_item.set_enabled(false);
            }
        }
        4 => {
            let _ = restart_backend_item.set_text(format!("[진행] 백엔드 재기동(종료)... ({}/{} 잔여)", core_count, core_installed.max(1)));
            let _ = restart_backend_item.set_enabled(false);
        }
        7 => {
            if core_count >= core_installed {
                PENDING_GROUP.store(0, Ordering::Relaxed);
                let _ = restart_backend_item.set_text("백엔드만 재기동 (WS/Sync/API)");
                let _ = restart_backend_item.set_enabled(core_installed > 0);
            } else {
                let _ = restart_backend_item.set_text(format!("[진행] 백엔드 재기동(기동)... ({}/{})", core_count, core_installed));
                let _ = restart_backend_item.set_enabled(false);
            }
        }
        5 => {
            let _ = restart_infra_item.set_text(format!("[진행] 인프라 재기동(종료)... ({}/{} 잔여)", infra_count, infra_installed.max(1)));
            let _ = restart_infra_item.set_enabled(false);
        }
        8 => {
            if infra_count >= infra_installed {
                PENDING_GROUP.store(0, Ordering::Relaxed);
                let _ = restart_infra_item.set_text("인프라만 재기동 (MySQL/Kafka)");
                let _ = restart_infra_item.set_enabled(infra_installed > 0);
            } else {
                let _ = restart_infra_item.set_text(format!("[진행] 인프라 재기동(기동)... ({}/{})", infra_count, infra_installed));
                let _ = restart_infra_item.set_enabled(false);
            }
        }
        _ => {
            let _ = start_all_item.set_text("▶ 전체 서비스 시작");
            let _ = stop_all_item.set_text("■ 전체 서비스 종료");
            let _ = restart_all_item.set_text("전체 재기동");
            let _ = restart_backend_item.set_text("백엔드만 재기동 (WS/Sync/API)");
            let _ = restart_infra_item.set_text("인프라만 재기동 (MySQL/Kafka)");

            let _ = start_all_item.set_enabled(installed_total > 0 && count < installed_total);
            let _ = stop_all_item.set_enabled(count > 0);
            let _ = restart_all_item.set_enabled(installed_total > 0 && count > 0);
            let _ = restart_backend_item.set_enabled(core_installed > 0 && core_count > 0);
            let _ = restart_infra_item.set_enabled(infra_installed > 0 && infra_count > 0);
        }
    }

    let tooltip = if installed_total == 0 {
        format!("ELVIS Launcher [제품 미설치: {}]", install.root.to_string_lossy())
    } else if uninstalled_total > 0 {
        format!("ELVIS Launcher [설치 {}/5 · {}개 가동 중]", installed_total, count)
    } else {
        format!("ELVIS Launcher [5개 중 {}개 가동 중]", count)
    };
    let _ = tray.set_tooltip(Some(tooltip));
}

#[tauri::command]
fn get_product_root() -> serde_json::Value {
    let root = load_product_root();
    let install = check_install_snapshot(&root);
    serde_json::json!({
        "productRoot": root.to_string_lossy(),
        "defaultRoot": default_product_root(),
        "exists": install.root_exists,
        "installed": install.installed_json(),
        "missing": install.missing_json()
    })
}

#[tauri::command]
fn set_product_root(path: String) -> Result<serde_json::Value, String> {
    let saved = save_product_root(&path)?;
    let root = PathBuf::from(&saved);
    let install = check_install_snapshot(&root);
    let env = load_env_config();
    Ok(serde_json::json!({
        "productRoot": saved,
        "defaultRoot": default_product_root(),
        "exists": install.root_exists,
        "installed": install.installed_json(),
        "missing": install.missing_json(),
        "ports": {
            "mysql": env.mysql_port,
            "kafka": env.kafka_port,
            "api": env.api_port,
            "ws": env.ws_port,
            "sync": env.sync_port,
            "tester": env.tester_port
        }
    }))
}

#[tauri::command]
fn browse_product_root() -> Result<Option<String>, String> {
    #[cfg(target_os = "windows")]
    {
        use std::os::windows::process::CommandExt;
        const CREATE_NO_WINDOW: u32 = 0x0800_0000;

        let current = load_product_root();
        let initial = escape_ps_single_quoted(&current.to_string_lossy());
        let ps_script = format!(
            "[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; \
             Add-Type -AssemblyName System.Windows.Forms; \
             $dlg = New-Object System.Windows.Forms.FolderBrowserDialog; \
             $dlg.Description = 'ELVIS Lite 제품 메인 경로를 선택하세요 (예: D:\\elvis-lite)'; \
             $dlg.ShowNewFolderButton = $true; \
             if (Test-Path -LiteralPath '{init}') {{ $dlg.SelectedPath = '{init}' }}; \
             $owner = New-Object System.Windows.Forms.Form; \
             $owner.TopMost = $true; \
             $owner.StartPosition = 'CenterScreen'; \
             $owner.Width = 1; $owner.Height = 1; $owner.Opacity = 0; \
             $owner.Show(); $owner.Activate(); \
             $res = $dlg.ShowDialog($owner); \
             $owner.Dispose(); \
             if ($res -eq [System.Windows.Forms.DialogResult]::OK) {{ [Console]::Out.Write($dlg.SelectedPath) }}",
            init = initial
        );

        let output = Command::new("powershell")
            .args([
                "-NoProfile",
                "-STA",
                "-WindowStyle",
                "Hidden",
                "-EncodedCommand",
                &encode_ps_base64(&ps_script),
            ])
            .creation_flags(CREATE_NO_WINDOW)
            .output()
            .map_err(|e| format!("폴더 선택 창 호출 실패: {}", e))?;

        let selected = String::from_utf8_lossy(&output.stdout).trim().to_string();
        if selected.is_empty() {
            Ok(None)
        } else {
            Ok(Some(normalize_product_root(&selected)))
        }
    }

    #[cfg(not(target_os = "windows"))]
    {
        Err("Windows OS에서만 지원됩니다.".to_string())
    }
}

#[tauri::command]
fn check_middleware_status(
    mysql_port: Option<u16>,
    kafka_port: Option<u16>,
    api_port: Option<u16>,
    ws_port: Option<u16>,
    sync_port: Option<u16>,
    tester_port: Option<u16>,
) -> serde_json::Value {
    let env = load_env_config();
    let install = check_install_snapshot(&env.product_root);
    let p_mysql = mysql_port.unwrap_or(env.mysql_port);
    let p_kafka = kafka_port.unwrap_or(env.kafka_port);
    let p_api = api_port.unwrap_or(env.api_port);
    let p_ws = ws_port.unwrap_or(env.ws_port);
    let p_sync = sync_port.unwrap_or(env.sync_port);
    let p_tester = tester_port.unwrap_or(env.tester_port);

    let kafka = check_single_port(p_kafka);
    let mysql = check_single_port(p_mysql);
    let api = check_single_port(p_api);
    let ws = check_single_port(p_ws);
    let sync = check_single_port(p_sync);
    let tester = check_single_port(p_tester) || check_single_port(1422);

    serde_json::json!({
        "productRoot": env.product_root.to_string_lossy(),
        "defaultRoot": default_product_root(),
        "productRootExists": install.root_exists,
        "kafka": kafka,
        "mysql": mysql,
        "controlApi": api,
        "connectWs": ws,
        "connectSync": sync,
        "tester": tester,
        "installed": install.installed_json(),
        "missing": install.missing_json(),
        "ports": {
            "mysql": p_mysql,
            "kafka": p_kafka,
            "api": p_api,
            "ws": p_ws,
            "sync": p_sync,
            "tester": p_tester
        }
    })
}

fn find_script_file(script_file: &str) -> Result<std::path::PathBuf, String> {
    let root = load_product_root();
    let candidate = root.join("scripts").join(script_file);
    if candidate.is_file() {
        return Ok(candidate);
    }

    Err(format!(
        "스크립트 파일을 찾을 수 없습니다 ({}): {}",
        root.to_string_lossy(),
        script_file
    ))
}

fn execute_bat_script(script_file: &str, service_name: &str) -> Result<String, String> {
    #[cfg(target_os = "windows")]
    {
        use std::os::windows::process::CommandExt;
        use std::process::Command;
        // CREATE_NO_WINDOW: GUI 앱(Tauri)에서 powershell 기동 시 콘솔 창 깜빡임 방지
        const CREATE_NO_WINDOW: u32 = 0x0800_0000;

        let product_root = load_product_root();
        let product_root_str = product_root.to_string_lossy().to_string();
        let daemon_script = find_script_file("run-daemon.ps1")?;
        let _ = find_script_file(script_file)?;
        let cwd = daemon_script.parent().unwrap();

        // run-daemon.ps1 (WMI Win32_Process.Create)를 통해 런처(Tauri) 프로세스 트리와 완전히 분리된 무창 독립 데몬으로 기동
        // 런처 창을 닫거나 런처 앱이 종료되어도 MySQL, Kafka 및 코어 프로세스가 종료되지 않고 유지됨
        Command::new("powershell")
            .args([
                "-NoProfile",
                "-WindowStyle",
                "Hidden",
                "-ExecutionPolicy",
                "Bypass",
                "-File",
                daemon_script.to_str().unwrap(),
                "-BatName",
                script_file,
                "-ServiceName",
                service_name,
                "-ElvisHome",
                &product_root_str,
            ])
            .current_dir(cwd)
            .env("NO_PAUSE", "1")
            .env("ELVIS_HOME", &product_root_str)
            .creation_flags(CREATE_NO_WINDOW)
            .spawn()
            .map_err(|e| format!("프로세스 기동 실패: {}", e))?;

        Ok(format!("{} 서비스가 백그라운드 독립 프로세스로 기동되었습니다.", service_name))
    }

    #[cfg(not(target_os = "windows"))]
    {
        Err("Windows OS에서만 지원됩니다.".to_string())
    }
}

fn start_all_services_sequence(
    run_kafka: bool,
    run_mysql: bool,
    run_ws: bool,
    run_sync: bool,
    run_api: bool,
) {
    std::thread::spawn(move || {
        if run_kafka {
            let _ = execute_bat_script("start-kafka-kraft.bat", "kafka");
            if run_mysql || run_ws || run_sync || run_api {
                std::thread::sleep(std::time::Duration::from_millis(2500));
            }
        }
        if run_mysql {
            let _ = execute_bat_script("start-mysql.bat", "mysql");
            if run_ws || run_sync || run_api {
                std::thread::sleep(std::time::Duration::from_millis(3000));
            }
        }
        if run_ws {
            let _ = execute_bat_script("start-elvis-connect-ws.bat", "ws");
        }
        if run_sync {
            let _ = execute_bat_script("start-elvis-connect-sync.bat", "sync");
        }
        if run_api {
            if run_ws || run_sync {
                std::thread::sleep(std::time::Duration::from_millis(2500));
            }
            let _ = execute_bat_script("start-elvis-control-api.bat", "api");
        }
    });
}

fn stop_service_processes(service: &str) -> Result<String, String> {
    #[cfg(target_os = "windows")]
    {
        use std::os::windows::process::CommandExt;
        const CREATE_NO_WINDOW: u32 = 0x0800_0000;
        let env = load_env_config();

        let ps_script = match service {
            "kafka" => format!(
                "Get-NetTCPConnection -LocalPort {},9092 -State Listen -ErrorAction SilentlyContinue | ForEach-Object {{ Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }}; \
                 Get-Process -ErrorAction SilentlyContinue | Where-Object {{ $_.MainWindowTitle -like 'ELVIS - Apache Kafka*' }} | Stop-Process -Force -ErrorAction SilentlyContinue; \
                 Get-CimInstance Win32_Process -ErrorAction SilentlyContinue | Where-Object {{ $_.CommandLine -match 'kafka\\.Kafka' }} | ForEach-Object {{ Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }}",
                env.kafka_port
            ),
            "mysql" => format!(
                "Stop-Process -Name 'mysqld' -Force -ErrorAction SilentlyContinue; \
                 Get-NetTCPConnection -LocalPort {},3306 -State Listen -ErrorAction SilentlyContinue | ForEach-Object {{ Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }}; \
                 Get-Process -ErrorAction SilentlyContinue | Where-Object {{ $_.MainWindowTitle -like 'ELVIS - MySQL*' }} | Stop-Process -Force -ErrorAction SilentlyContinue",
                env.mysql_port
            ),
            "ws" => format!(
                "Get-NetTCPConnection -LocalPort {} -State Listen -ErrorAction SilentlyContinue | ForEach-Object {{ Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }}; \
                 Get-Process -ErrorAction SilentlyContinue | Where-Object {{ $_.MainWindowTitle -like 'ELVIS - Connect WS*' }} | Stop-Process -Force -ErrorAction SilentlyContinue",
                env.ws_port
            ),
            "sync" => format!(
                "Get-NetTCPConnection -LocalPort {} -State Listen -ErrorAction SilentlyContinue | ForEach-Object {{ Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }}; \
                 Get-Process -ErrorAction SilentlyContinue | Where-Object {{ $_.MainWindowTitle -like 'ELVIS - Connect Sync*' }} | Stop-Process -Force -ErrorAction SilentlyContinue",
                env.sync_port
            ),
            "api" => format!(
                "Get-NetTCPConnection -LocalPort {} -State Listen -ErrorAction SilentlyContinue | ForEach-Object {{ Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }}; \
                 Get-Process -ErrorAction SilentlyContinue | Where-Object {{ $_.MainWindowTitle -like 'ELVIS - Control API*' }} | Stop-Process -Force -ErrorAction SilentlyContinue",
                env.api_port
            ),
            "tester" => format!(
                "Get-NetTCPConnection -LocalPort {},1422 -State Listen -ErrorAction SilentlyContinue | ForEach-Object {{ Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }}; \
                 Get-Process -ErrorAction SilentlyContinue | Where-Object {{ $_.MainWindowTitle -like 'ELVIS - Tester Harness*' }} | Stop-Process -Force -ErrorAction SilentlyContinue",
                env.tester_port
            ),
            "all-services" => format!(
                "Get-NetTCPConnection -LocalPort {},{},{} -State Listen -ErrorAction SilentlyContinue | ForEach-Object {{ Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }}; \
                 Get-Process -ErrorAction SilentlyContinue | Where-Object {{ $_.MainWindowTitle -like 'ELVIS - Connect*' -or $_.MainWindowTitle -like 'ELVIS - Control*' }} | Stop-Process -Force -ErrorAction SilentlyContinue",
                env.ws_port, env.sync_port, env.api_port
            ),
            "all-infra" => format!(
                "Stop-Process -Name 'mysqld' -Force -ErrorAction SilentlyContinue; \
                 Get-NetTCPConnection -LocalPort {},{},3306,9092 -State Listen -ErrorAction SilentlyContinue | ForEach-Object {{ Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }}; \
                 Get-Process -ErrorAction SilentlyContinue | Where-Object {{ $_.MainWindowTitle -like 'ELVIS - Apache Kafka*' -or $_.MainWindowTitle -like 'ELVIS - MySQL*' }} | Stop-Process -Force -ErrorAction SilentlyContinue; \
                 Get-CimInstance Win32_Process -ErrorAction SilentlyContinue | Where-Object {{ $_.CommandLine -match 'kafka\\.Kafka' }} | ForEach-Object {{ Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }}",
                env.mysql_port, env.kafka_port
            ),
            "all" => format!(
                "Stop-Process -Name 'mysqld' -Force -ErrorAction SilentlyContinue; \
                 Get-NetTCPConnection -LocalPort {},{},{},{},{},3306,9092 -State Listen -ErrorAction SilentlyContinue | ForEach-Object {{ Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }}; \
                 Get-Process -ErrorAction SilentlyContinue | Where-Object {{ $_.MainWindowTitle -like 'ELVIS - Connect*' -or $_.MainWindowTitle -like 'ELVIS - Control*' -or $_.MainWindowTitle -like 'ELVIS - Apache*' -or $_.MainWindowTitle -like 'ELVIS - MySQL*' }} | Stop-Process -Force -ErrorAction SilentlyContinue; \
                 Get-CimInstance Win32_Process -ErrorAction SilentlyContinue | Where-Object {{ $_.CommandLine -match 'kafka\\.Kafka' }} | ForEach-Object {{ Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }}",
                env.mysql_port, env.kafka_port, env.ws_port, env.sync_port, env.api_port
            ),
            _ => return Err("지원되지 않는 서비스입니다.".to_string()),
        };

        let encoded = encode_ps_base64(&ps_script);
        let _ = Command::new("powershell")
            .args(["-NoProfile", "-WindowStyle", "Hidden", "-EncodedCommand", &encoded])
            .creation_flags(CREATE_NO_WINDOW)
            .output()
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
    let root = load_product_root();
    let install = check_install_snapshot(&root);
    let root_label = root.to_string_lossy().to_string();

    match service.as_str() {
        "all" => {
            if install.installed_count() == 0 {
                return Err(format!("설치된 서비스가 없습니다. (제품 경로: {})", root_label));
            }
            start_all_services_sequence(
                install.kafka,
                install.mysql,
                install.ws,
                install.sync,
                install.api,
            );
            Ok(format!(
                "설치된 {}개 서비스가 순차적으로 백그라운드 기동됩니다.",
                install.installed_count()
            ))
        }
        "all-infra" => {
            if install.infra_installed_count() == 0 {
                return Err(format!("설치된 미들웨어가 없습니다. (제품 경로: {})", root_label));
            }
            let run_kafka = install.kafka;
            let run_mysql = install.mysql;
            std::thread::spawn(move || {
                if run_kafka {
                    let _ = execute_bat_script("start-kafka-kraft.bat", "kafka");
                    if run_mysql {
                        std::thread::sleep(std::time::Duration::from_millis(2500));
                    }
                }
                if run_mysql {
                    let _ = execute_bat_script("start-mysql.bat", "mysql");
                }
            });
            Ok("설치된 미들웨어가 순차적으로 백그라운드 기동됩니다.".to_string())
        }
        "all-services" => {
            if install.core_installed_count() == 0 {
                return Err(format!("설치된 코어 엔진이 없습니다. (제품 경로: {})", root_label));
            }
            let run_ws = install.ws;
            let run_sync = install.sync;
            let run_api = install.api;
            std::thread::spawn(move || {
                if run_ws {
                    let _ = execute_bat_script("start-elvis-connect-ws.bat", "ws");
                }
                if run_sync {
                    let _ = execute_bat_script("start-elvis-connect-sync.bat", "sync");
                }
                if run_api {
                    if run_ws || run_sync {
                        std::thread::sleep(std::time::Duration::from_millis(2500));
                    }
                    let _ = execute_bat_script("start-elvis-control-api.bat", "api");
                }
            });
            Ok("설치된 코어 엔진이 순차적으로 백그라운드 기동됩니다.".to_string())
        }
        "kafka" => {
            if !install.kafka {
                return Err(format!("Apache Kafka가 설치되어 있지 않습니다. (경로: {})", root_label));
            }
            execute_bat_script("start-kafka-kraft.bat", "kafka")
        }
        "mysql" => {
            if !install.mysql {
                return Err(format!("MySQL 9.71이 설치되어 있지 않습니다. (경로: {})", root_label));
            }
            execute_bat_script("start-mysql.bat", "mysql")
        }
        "ws" => {
            if !install.ws {
                return Err(format!("elvis-connect-ws가 설치되어 있지 않습니다. (경로: {})", root_label));
            }
            execute_bat_script("start-elvis-connect-ws.bat", "ws")
        }
        "sync" => {
            if !install.sync {
                return Err(format!("elvis-connect-sync가 설치되어 있지 않습니다. (경로: {})", root_label));
            }
            execute_bat_script("start-elvis-connect-sync.bat", "sync")
        }
        "api" => {
            if !install.api {
                return Err(format!("elvis-control-api가 설치되어 있지 않습니다. (경로: {})", root_label));
            }
            execute_bat_script("start-elvis-control-api.bat", "api")
        }
        "tester" => execute_bat_script("start-all-tester.bat", "tester"),
        "topics" => execute_bat_script("create-kafka-topics.bat", "topics"),
        _ => Err("알 수 없는 서비스 이름입니다.".to_string()),
    }
}

#[tauri::command]
fn stop_middleware_detached(service: String) -> Result<String, String> {
    stop_service_processes(&service)
}

#[cfg(target_os = "windows")]
const PS_NOWRAP_INIT: &str = "[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; \
chcp 65001 >$null; \
function Out-NoWrap { \
    process { \
        $line = ([string]$_).Replace([string][char]9, '  ').Replace([string][char]13, ''); \
        $line = [regex]::Replace($line, ([string][char]27) + '\\[[0-9;]*[A-Za-z]', ''); \
        $max = 78; \
        try { \
            $w1 = [Console]::WindowWidth; \
            $w2 = $Host.UI.RawUI.WindowSize.Width; \
            $winW = if ($w1 -gt 5 -and $w2 -gt 5) { [Math]::Min($w1, $w2) } elseif ($w1 -gt 5) { $w1 } elseif ($w2 -gt 5) { $w2 } else { 80 }; \
            $max = [Math]::Max(8, $winW - 2) \
        } catch {}; \
        $w = 0; $sb = [System.Text.StringBuilder]::new(); \
        foreach ($ch in $line.ToCharArray()) { \
            $cw = if ([int]$ch -gt 127) { 2 } else { 1 }; \
            if ($w + $cw -gt $max) { break }; \
            [void]$sb.Append($ch); $w += $cw \
        }; \
        Write-Host $sb.ToString() \
    } \
};";

#[cfg(target_os = "windows")]
const PS_WAIT_WIDTH_STABLE: &str = "$prevW = -1; $stable = 0; \
for ($i = 0; $i -lt 8; $i++) { \
    Start-Sleep -Milliseconds 150; \
    $curW = 0; \
    try { $curW = [Console]::WindowWidth } catch {}; \
    if ($curW -gt 5 -and $curW -eq $prevW) { \
        $stable++; \
        if ($stable -ge 3) { break } \
    } else { \
        $stable = 0; $prevW = $curW \
    } \
}; \
Clear-Host;";

#[tauri::command]
fn open_log_window(
    service: Option<String>,
    log_type: Option<String>,
    word_wrap: Option<bool>,
) -> Result<String, String> {
    #[cfg(target_os = "windows")]
    {
        let svc = service.unwrap_or_else(|| "mysql".to_string());
        if !matches!(svc.as_str(), "mysql" | "kafka" | "ws" | "sync" | "api" | "tester") {
            return Err(format!("지원되지 않는 로그 서비스입니다: {}", svc));
        }

        let env = load_env_config();
        let is_error_mode = log_type.as_deref() == Some("error");
        let is_word_wrap = word_wrap.unwrap_or(false);
        let wrap_label = if is_word_wrap { "줄바꿈 ON" } else { "줄바꿈 OFF" };

        let (log_file_name, display_name) = match svc.as_str() {
            "mysql" => ("mysql.log", format!("MySQL 9.71 (:{})", env.mysql_port)),
            "kafka" => ("kafka.log", format!("Apache Kafka (:{})", env.kafka_port)),
            "ws" => ("ws.log", format!("elvis-connect-ws (:{})", env.ws_port)),
            "sync" => ("sync.log", format!("elvis-connect-sync (:{})", env.sync_port)),
            "api" => ("api.log", format!("elvis-control-api (:{})", env.api_port)),
            "tester" => ("tester.log", format!("elvis-tester (:{})", env.tester_port)),
            _ => unreachable!(),
        };

        // 0) 독립 Rust 네이티브 GUI 모듈(elvis-log-viewer.exe) 우선 실행 (5대 서비스 단일 프로세스 모드)
        if matches!(svc.as_str(), "mysql" | "kafka" | "ws" | "sync" | "api") {
            if let Some(viewer_exe) = find_log_viewer_binary() {
                let ports_arg = format!(
                    "{},{},{},{},{}",
                    env.mysql_port, env.kafka_port, env.ws_port, env.sync_port, env.api_port
                );
                let type_arg = if is_error_mode { "error" } else { "all" };
                let wrap_arg = if is_word_wrap { "1" } else { "0" };

                Command::new(&viewer_exe)
                    .args([
                        "--log-dir",
                        &env.log_dir,
                        "--service",
                        &svc,
                        "--type",
                        type_arg,
                        "--wrap",
                        wrap_arg,
                        "--ports",
                        &ports_arg,
                    ])
                    .spawn()
                    .map_err(|e| format!("elvis-log-viewer 기동 실패: {}", e))?;

                return Ok(format!(
                    "{} {} 뷰어를 열었습니다. ({})",
                    display_name,
                    if is_error_mode { "에러 로그" } else { "실시간 로그" },
                    wrap_label
                ));
            }
        }

        let init_script = if is_word_wrap {
            "[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; chcp 65001 >$null;"
        } else {
            PS_NOWRAP_INIT
        };
        let pipe_suffix = if is_word_wrap { "" } else { " | Out-NoWrap" };

        let full_log_path = PathBuf::from(&env.log_dir)
            .join(log_file_name)
            .to_string_lossy()
            .to_string();
        let win_title = if is_error_mode {
            format!("ELVIS - {} 실시간 [에러] 콘솔 로그 [{}]", display_name, wrap_label)
        } else {
            format!("ELVIS - {} 실시간 콘솔 로그 [{}]", display_name, wrap_label)
        };

        let safe_title_cmd = sanitize_cmd_title(&win_title);
        let ps_title = escape_ps_single_quoted(&win_title);
        let ps_display = escape_ps_single_quoted(&display_name);
        let ps_path = escape_ps_single_quoted(&full_log_path);
        let ps_file_name = escape_ps_single_quoted(log_file_name);

        // 1) 기존 동일 서비스 로그창이 이미 열려 있는지 검사 -> 활성화(Focus) 성공 시 중복 생성 방지
        let check_script = format!(
            "$w = New-Object -ComObject WScript.Shell; \
             if ($w.AppActivate('{title}')) {{ exit 10 }}; \
             $p = Get-Process -ErrorAction SilentlyContinue | Where-Object {{ $_.MainWindowTitle -like '*{title}*' }}; \
             if ($p) {{ if ($w.AppActivate($p[0].Id)) {{ exit 10 }} }}; \
             exit 0",
            title = ps_title
        );

        use std::os::windows::process::CommandExt;
        const CREATE_NO_WINDOW: u32 = 0x0800_0000;

        if let Ok(st) = Command::new("powershell")
            .args([
                "-NoProfile",
                "-NonInteractive",
                "-EncodedCommand",
                &encode_ps_base64(&check_script),
            ])
            .creation_flags(CREATE_NO_WINDOW)
            .status()
        {
            if st.code() == Some(10) {
                return Ok(format!("기존 {} 실시간 콘솔 창을 활성화했습니다.", display_name));
            }
        }

        // 2) 열려 있지 않은 경우 신규 창 기동 (에러 모드 vs 일반 모드 + 줄바꿈 옵션 분기)
        let ps_cmd = if is_error_mode {
            format!(
                "{init} \
                 $host.UI.RawUI.WindowTitle = '{title}'; \
                 Clear-Host; \
                 Write-Host '========================================================================================================' -ForegroundColor DarkRed; \
                 Write-Host '  ELVIS CSMS Lite - {display_name} 실시간 [에러/예외] 필터 스트림 [{wrap_label}]' -ForegroundColor Red; \
                 Write-Host '  감시 대상: {path} (ERROR | Exception | FATAL | 실패 필터링)' -ForegroundColor DarkGray; \
                 Write-Host '  (종료: 창 닫기 또는 Ctrl+C)' -ForegroundColor DarkGray; \
                 Write-Host '========================================================================================================' -ForegroundColor DarkRed; \
                 Write-Host ''; \
                 if (Test-Path '{path}') {{ \
                     Write-Host '[실시간 필터] ERROR/Exception 발생 이벤트를 대기 및 모니터링합니다...' -ForegroundColor DarkYellow; \
                     Get-Content -Path '{path}' -Tail 200 -Wait -Encoding utf8 | Where-Object {{ $_ -match 'ERROR|Exception|FATAL|경고|실패|RuntimeException|NullPointer' }}{pipe} \
                 }} else {{ \
                     Write-Host '[안내] 원본 로그 파일({log_file_name}) 대기 중...' -ForegroundColor Yellow; \
                     while (-not (Test-Path '{path}')) {{ Start-Sleep -Seconds 2 }}; \
                     Clear-Host; \
                     Write-Host '[연결됨] 에러 필터 감시를 시작합니다.' -ForegroundColor Red; \
                     Get-Content -Path '{path}' -Tail 200 -Wait -Encoding utf8 | Where-Object {{ $_ -match 'ERROR|Exception|FATAL|경고|실패|RuntimeException|NullPointer' }}{pipe} \
                 }}",
                init = init_script,
                title = ps_title,
                display_name = ps_display,
                wrap_label = wrap_label,
                path = ps_path,
                log_file_name = ps_file_name,
                pipe = pipe_suffix
            )
        } else {
            format!(
                "{init} \
                 $host.UI.RawUI.WindowTitle = '{title}'; \
                 Clear-Host; \
                 Write-Host '========================================================================================================' -ForegroundColor DarkCyan; \
                 Write-Host '  ELVIS CSMS Lite - {display_name} 실시간 콘솔 로그 [{wrap_label}]' -ForegroundColor Cyan; \
                 Write-Host '  로그 경로: {path}' -ForegroundColor DarkGray; \
                 Write-Host '  (종료: 창 닫기 또는 Ctrl+C)' -ForegroundColor DarkGray; \
                 Write-Host '========================================================================================================' -ForegroundColor DarkCyan; \
                 Write-Host ''; \
                 if (Test-Path '{path}') {{ \
                     Get-Content -Path '{path}' -Tail 150 -Wait -Encoding utf8{pipe} \
                 }} else {{ \
                     Write-Host '[안내] 아직 {log_file_name} 파일이 생성되지 않았습니다.' -ForegroundColor Yellow; \
                     Write-Host '서비스를 기동하면 실시간 로그가 자동으로 기록됩니다.' -ForegroundColor Gray; \
                     Write-Host '파일 생성을 대기하는 중... (Ctrl+C로 종료)' -ForegroundColor DarkGray; \
                     while (-not (Test-Path '{path}')) {{ Start-Sleep -Seconds 2 }}; \
                     Clear-Host; \
                     Write-Host '[연결됨] {log_file_name} 로그 감시를 시작합니다.' -ForegroundColor Green; \
                     Get-Content -Path '{path}' -Tail 150 -Wait -Encoding utf8{pipe} \
                 }}",
                init = init_script,
                title = ps_title,
                display_name = ps_display,
                wrap_label = wrap_label,
                path = ps_path,
                log_file_name = ps_file_name,
                pipe = pipe_suffix
            )
        };

        Command::new("cmd")
            .args([
                "/c",
                "start",
                &safe_title_cmd,
                "powershell",
                "-NoExit",
                "-EncodedCommand",
                &encode_ps_base64(&ps_cmd),
            ])
            .spawn()
            .map_err(|e| format!("PowerShell 콘솔 기동 실패: {}", e))?;

        Ok(format!(
            "{} 서비스의 실시간 {} 터미널 콘솔이 열렸습니다.",
            svc,
            if is_error_mode { "에러" } else { "표준" }
        ))
    }
    #[cfg(not(target_os = "windows"))]
    {
        Err("Windows OS에서만 지원됩니다.".to_string())
    }
}

#[cfg(target_os = "windows")]
fn build_pane_log_ps_encoded(
    env: &EnvConfig,
    svc: &str,
    is_error_mode: bool,
    is_word_wrap: bool,
) -> Result<String, String> {
    let (log_file_name, display_name) = match svc {
        "mysql" => ("mysql.log", format!("MySQL 9.71 (:{})", env.mysql_port)),
        "kafka" => ("kafka.log", format!("Apache Kafka (:{})", env.kafka_port)),
        "ws" => ("ws.log", format!("elvis-connect-ws (:{})", env.ws_port)),
        "sync" => ("sync.log", format!("elvis-connect-sync (:{})", env.sync_port)),
        "api" => ("api.log", format!("elvis-control-api (:{})", env.api_port)),
        _ => return Err(format!("지원되지 않는 로그 서비스입니다: {}", svc)),
    };

    let full_log_path = PathBuf::from(&env.log_dir)
        .join(log_file_name)
        .to_string_lossy()
        .to_string();

    let ps_display = escape_ps_single_quoted(&display_name);
    let ps_path = escape_ps_single_quoted(&full_log_path);
    let ps_file_name = escape_ps_single_quoted(log_file_name);

    let wrap_label = if is_word_wrap { "줄바꿈 ON" } else { "줄바꿈 OFF" };
    let init_script = if is_word_wrap {
        "[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; chcp 65001 >$null;"
    } else {
        PS_NOWRAP_INIT
    };
    let post_clear_vt = if is_word_wrap {
        ""
    } else {
        PS_WAIT_WIDTH_STABLE
    };
    let pipe_suffix = if is_word_wrap { "" } else { " | Out-NoWrap" };

    let ps_cmd = if is_error_mode {
        format!(
            "{init} \
             Clear-Host; \
             {post_clear} \
             Write-Host '=== [에러] {display_name} [{wrap_label}] ===' -ForegroundColor Red; \
             if (Test-Path '{path}') {{ \
                 Get-Content -Path '{path}' -Tail 200 -Wait -Encoding utf8 | Where-Object {{ $_ -match 'ERROR|Exception|FATAL|경고|실패|RuntimeException|NullPointer' }}{pipe} \
             }} else {{ \
                 Write-Host '[대기] {log_file_name} 파일 생성 대기 중...' -ForegroundColor Yellow; \
                 while (-not (Test-Path '{path}')) {{ Start-Sleep -Seconds 2 }}; \
                 Get-Content -Path '{path}' -Tail 200 -Wait -Encoding utf8 | Where-Object {{ $_ -match 'ERROR|Exception|FATAL|경고|실패|RuntimeException|NullPointer' }}{pipe} \
             }}",
            init = init_script,
            post_clear = post_clear_vt,
            display_name = ps_display,
            wrap_label = wrap_label,
            path = ps_path,
            log_file_name = ps_file_name,
            pipe = pipe_suffix
        )
    } else {
        format!(
            "{init} \
             Clear-Host; \
             {post_clear} \
             Write-Host '=== {display_name} [{wrap_label}] ===' -ForegroundColor Cyan; \
             if (Test-Path '{path}') {{ \
                 Get-Content -Path '{path}' -Tail 150 -Wait -Encoding utf8{pipe} \
             }} else {{ \
                 Write-Host '[대기] {log_file_name} 파일 생성 대기 중...' -ForegroundColor Yellow; \
                 while (-not (Test-Path '{path}')) {{ Start-Sleep -Seconds 2 }}; \
                 Get-Content -Path '{path}' -Tail 150 -Wait -Encoding utf8{pipe} \
             }}",
            init = init_script,
            post_clear = post_clear_vt,
            display_name = ps_display,
            wrap_label = wrap_label,
            path = ps_path,
            log_file_name = ps_file_name,
            pipe = pipe_suffix
        )
    };

    Ok(encode_ps_base64(&ps_cmd))
}

#[cfg(target_os = "windows")]
fn find_log_viewer_binary() -> Option<PathBuf> {
    let mut candidates: Vec<PathBuf> = Vec::new();
    let product_root = load_product_root();
    // 개발 환경(debug): 워크스페이스 빌드 산출물(target/debug -> target/release) 우선 탐색
    #[cfg(debug_assertions)]
    {
        candidates.push(
            product_root
                .join("elvis-log-viewer")
                .join("target")
                .join("debug")
                .join("elvis-log-viewer.exe"),
        );
        candidates.push(
            product_root
                .join("elvis-log-viewer")
                .join("target")
                .join("release")
                .join("elvis-log-viewer.exe"),
        );
    }
    // Release(및 폴백): 설정된 제품 메인 경로 및 실행 파일과 같은 폴더의 elvis-log-viewer.exe
    candidates.push(product_root.join("elvis-log-viewer.exe"));
    if let Ok(exe) = std::env::current_exe() {
        if let Some(dir) = exe.parent() {
            candidates.push(dir.join("elvis-log-viewer.exe"));
        }
    }
    candidates.into_iter().find(|p| p.is_file())
}

/// 통합 분할 로그 창 기동:
/// 1) 독립 Rust 네이티브 GUI 모듈(elvis-log-viewer.exe) 우선 실행 (가로 스크롤 + 4축 마우스 드래그 리사이즈)
/// 2) 바이너리 미빌드 시 Windows Terminal(wt.exe) 5분할 자동 폴백
#[tauri::command]
fn open_log_group_window(
    group: Option<String>,
    log_type: Option<String>,
    word_wrap: Option<bool>,
) -> Result<String, String> {
    #[cfg(target_os = "windows")]
    {
        let grp = group.unwrap_or_else(|| "all".to_string());
        if !matches!(grp.as_str(), "all" | "infra" | "core") {
            return Err(format!("지원되지 않는 로그 그룹입니다: {}", grp));
        }

        let env = load_env_config();
        let is_error_mode = log_type.as_deref() == Some("error");
        let is_word_wrap = word_wrap.unwrap_or(false);
        let mode_suffix = if is_error_mode { "에러 로그" } else { "실시간 로그" };
        let wrap_tag = if is_word_wrap { "줄바꿈 ON" } else { "줄바꿈 OFF" };

        // 1) 독립 Rust 네이티브 GUI 모듈(elvis-log-viewer.exe) 우선 실행
        if let Some(viewer_exe) = find_log_viewer_binary() {
            let ports_arg = format!(
                "{},{},{},{},{}",
                env.mysql_port, env.kafka_port, env.ws_port, env.sync_port, env.api_port
            );
            let type_arg = if is_error_mode { "error" } else { "all" };
            let wrap_arg = if is_word_wrap { "1" } else { "0" };

            Command::new(&viewer_exe)
                .args([
                    "--log-dir",
                    &env.log_dir,
                    "--group",
                    &grp,
                    "--type",
                    type_arg,
                    "--wrap",
                    wrap_arg,
                    "--ports",
                    &ports_arg,
                ])
                .spawn()
                .map_err(|e| format!("elvis-log-viewer 기동 실패: {}", e))?;

            let msg = match grp.as_str() {
                "infra" => format!("미들웨어 2분할 {} 뷰어를 열었습니다. ({})", mode_suffix, wrap_tag),
                "core" => format!("코어 엔진 3분할 {} 뷰어를 열었습니다. ({})", mode_suffix, wrap_tag),
                _ => format!("전체 5분할 {} 뷰어를 열었습니다. ({})", mode_suffix, wrap_tag),
            };
            return Ok(msg);
        }

        let mut args: Vec<String> = vec!["-M".into(), "-w".into(), "new".into()];

        match grp.as_str() {
            "infra" => {
                let mysql_b64 = build_pane_log_ps_encoded(&env, "mysql", is_error_mode, is_word_wrap)?;
                let kafka_b64 = build_pane_log_ps_encoded(&env, "kafka", is_error_mode, is_word_wrap)?;
                let tab_title = format!("ELVIS 미들웨어 {} (2분할 · {})", mode_suffix, wrap_tag);

                args.extend([
                    "new-tab".into(), "--title".into(), tab_title, "--suppressApplicationTitle".into(),
                    "powershell".into(), "-NoProfile".into(), "-NoExit".into(), "-EncodedCommand".into(), mysql_b64,
                    ";".into(),
                    "split-pane".into(), "-V".into(), "-s".into(), "0.5".into(), "--suppressApplicationTitle".into(),
                    "powershell".into(), "-NoProfile".into(), "-NoExit".into(), "-EncodedCommand".into(), kafka_b64,
                ]);
            }
            "core" => {
                let ws_b64 = build_pane_log_ps_encoded(&env, "ws", is_error_mode, is_word_wrap)?;
                let sync_b64 = build_pane_log_ps_encoded(&env, "sync", is_error_mode, is_word_wrap)?;
                let api_b64 = build_pane_log_ps_encoded(&env, "api", is_error_mode, is_word_wrap)?;
                let tab_title = format!("ELVIS 코어 엔진 {} (3분할 · {})", mode_suffix, wrap_tag);

                args.extend([
                    "new-tab".into(), "--title".into(), tab_title, "--suppressApplicationTitle".into(),
                    "powershell".into(), "-NoProfile".into(), "-NoExit".into(), "-EncodedCommand".into(), ws_b64,
                    ";".into(),
                    "split-pane".into(), "-V".into(), "-s".into(), "0.5".into(), "--suppressApplicationTitle".into(),
                    "powershell".into(), "-NoProfile".into(), "-NoExit".into(), "-EncodedCommand".into(), api_b64,
                    ";".into(),
                    "move-focus".into(), "left".into(),
                    ";".into(),
                    "split-pane".into(), "-H".into(), "-s".into(), "0.5".into(), "--suppressApplicationTitle".into(),
                    "powershell".into(), "-NoProfile".into(), "-NoExit".into(), "-EncodedCommand".into(), sync_b64,
                ]);
            }
            _ => {
                // 3행 2열 격자: 1:WS, 3:SYNC, 2+4:API, 5:MYSQL, 6:KAFKA
                let ws_b64 = build_pane_log_ps_encoded(&env, "ws", is_error_mode, is_word_wrap)?;
                let sync_b64 = build_pane_log_ps_encoded(&env, "sync", is_error_mode, is_word_wrap)?;
                let api_b64 = build_pane_log_ps_encoded(&env, "api", is_error_mode, is_word_wrap)?;
                let mysql_b64 = build_pane_log_ps_encoded(&env, "mysql", is_error_mode, is_word_wrap)?;
                let kafka_b64 = build_pane_log_ps_encoded(&env, "kafka", is_error_mode, is_word_wrap)?;
                let tab_title = format!("ELVIS 전체 {} (5분할 · {})", mode_suffix, wrap_tag);

                args.extend([
                    "new-tab".into(), "--title".into(), tab_title, "--suppressApplicationTitle".into(),
                    "powershell".into(), "-NoProfile".into(), "-NoExit".into(), "-EncodedCommand".into(), ws_b64,
                    ";".into(),
                    "split-pane".into(), "-H".into(), "-s".into(), "0.333".into(), "--suppressApplicationTitle".into(),
                    "powershell".into(), "-NoProfile".into(), "-NoExit".into(), "-EncodedCommand".into(), mysql_b64,
                    ";".into(),
                    "split-pane".into(), "-V".into(), "-s".into(), "0.5".into(), "--suppressApplicationTitle".into(),
                    "powershell".into(), "-NoProfile".into(), "-NoExit".into(), "-EncodedCommand".into(), kafka_b64,
                    ";".into(),
                    "move-focus".into(), "up".into(),
                    ";".into(),
                    "split-pane".into(), "-V".into(), "-s".into(), "0.5".into(), "--suppressApplicationTitle".into(),
                    "powershell".into(), "-NoProfile".into(), "-NoExit".into(), "-EncodedCommand".into(), api_b64,
                    ";".into(),
                    "move-focus".into(), "left".into(),
                    ";".into(),
                    "split-pane".into(), "-H".into(), "-s".into(), "0.5".into(), "--suppressApplicationTitle".into(),
                    "powershell".into(), "-NoProfile".into(), "-NoExit".into(), "-EncodedCommand".into(), sync_b64,
                ]);
            }
        }

        Command::new("wt.exe")
            .args(&args)
            .spawn()
            .map_err(|e| format!("Windows Terminal(wt.exe) 분할 창 기동 실패: {}", e))?;

        let msg = match grp.as_str() {
            "infra" => format!("미들웨어 2분할 {} 터미널을 열었습니다. ({})", mode_suffix, wrap_tag),
            "core" => format!("코어 엔진 3분할 {} 터미널을 열었습니다. ({})", mode_suffix, wrap_tag),
            _ => format!("전체 5분할 {} 터미널을 열었습니다. ({})", mode_suffix, wrap_tag),
        };
        Ok(msg)
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
        let target_arg = target.unwrap_or_else(|| "all".to_string());
        if !matches!(target_arg.as_str(), "all" | "infra" | "core" | "extension") {
            return Err(format!("허용되지 않은 진단 타겟입니다: {}", target_arg));
        }

        // 0) 독립 Rust 네이티브 GUI 모듈(elvis-log-viewer.exe) 프로세스 진단 모드 우선 실행
        if let Some(viewer_exe) = find_log_viewer_binary() {
            let env = load_env_config();
            let ports_arg = format!(
                "{},{},{},{},{}",
                env.mysql_port, env.kafka_port, env.ws_port, env.sync_port, env.api_port
            );
            let scope_arg = if target_arg == "infra" || target_arg == "core" {
                target_arg.as_str()
            } else {
                "all"
            };
            Command::new(&viewer_exe)
                .args([
                    "--log-dir",
                    &env.log_dir,
                    "--mode",
                    "process",
                    "--target",
                    scope_arg,
                    "--ports",
                    &ports_arg,
                ])
                .spawn()
                .map_err(|e| format!("elvis-log-viewer 프로세스 진단창 실행 실패: {}", e))?;

            let scope_label = match scope_arg {
                "infra" => "미들웨어",
                "core" => "코어 엔진",
                _ => "전체 시스템",
            };
            return Ok(format!(
                "[{}] 프로세스 및 포트 진단 뷰어를 열었습니다.",
                scope_label
            ));
        }

        let win_title = "ELVIS - 프로세스 및 포트 점유 실시간 진단";
        let ps_title = escape_ps_single_quoted(win_title);

        // 기존 진단창이 열려 있으면 포커스 활성화 (중복 기동 방지)
        let check_script = format!(
            "$w = New-Object -ComObject WScript.Shell; \
             if ($w.AppActivate('{title}')) {{ exit 10 }}; \
             $p = Get-Process -ErrorAction SilentlyContinue | Where-Object {{ $_.MainWindowTitle -like '*{title}*' }}; \
             if ($p) {{ if ($w.AppActivate($p[0].Id)) {{ exit 10 }} }}; \
             exit 0",
            title = ps_title
        );

        use std::os::windows::process::CommandExt;
        const CREATE_NO_WINDOW: u32 = 0x0800_0000;

        if let Ok(st) = Command::new("powershell")
            .args([
                "-NoProfile",
                "-NonInteractive",
                "-EncodedCommand",
                &encode_ps_base64(&check_script),
            ])
            .creation_flags(CREATE_NO_WINDOW)
            .status()
        {
            if st.code() == Some(10) {
                return Ok("기존 프로세스 진단 터미널을 활성화했습니다.".to_string());
            }
        }

        let target_bat = find_script_file("check-status.bat")?;
        let cwd = target_bat.parent().unwrap();
        let bat_str = target_bat.to_str().ok_or_else(|| "잘못된 스크립트 경로입니다.".to_string())?;
        Command::new("cmd")
            .args(["/c", "start", win_title, "cmd", "/c", "call", bat_str, &target_arg])
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
            "if (window.__triggerTrayCommand) {{ window.__triggerTrayCommand('{}', '{}'); }} else if (window.__TAURI_INTERNALS__) {{ window.__TAURI_INTERNALS__.invoke('reset_tray_pending'); }}",
            action, service
        );
        if window.eval(&js).is_err() {
            reset_tray_pending();
        }
    } else {
        reset_tray_pending();
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
            // #[cfg(debug_assertions)]
            // {
            //     if let Some(window) = app.get_webview_window("main") {
            //         let _ = window.open_devtools();
            //     }
            // }

            // 시스템 트레이 메뉴 아이템 정의
            let init_env = load_env_config();
            let summary_i = MenuItem::with_id(app, "status_summary", "● [ STATUS: 점검 중... ]", false, None::<&str>)?;
            let sep1 = PredefinedMenuItem::separator(app)?;

            // 1. 인프라 미들웨어 상태 (클릭 시 단독 기동/종료 토글)
            let mysql_i = MenuItem::with_id(
                app,
                "status_mysql",
                format!("○ [인프라] MySQL 9.71 (:{}) [OFF]", init_env.mysql_port),
                true,
                None::<&str>,
            )?;
            let kafka_i = MenuItem::with_id(
                app,
                "status_kafka",
                format!("○ [인프라] Apache Kafka (:{}) [OFF]", init_env.kafka_port),
                true,
                None::<&str>,
            )?;
            let sep_infra = PredefinedMenuItem::separator(app)?;

            // 2. 코어 백엔드 엔진 상태 (클릭 시 단독 기동/종료 토글)
            let ws_i = MenuItem::with_id(
                app,
                "status_ws",
                format!("○ [코어] elvis-connect-ws (:{}) [OFF]", init_env.ws_port),
                true,
                None::<&str>,
            )?;
            let sync_i = MenuItem::with_id(
                app,
                "status_sync",
                format!("○ [코어] elvis-connect-sync (:{}) [OFF]", init_env.sync_port),
                true,
                None::<&str>,
            )?;
            let api_i = MenuItem::with_id(
                app,
                "status_api",
                format!("○ [코어] elvis-control-api (:{}) [OFF]", init_env.api_port),
                true,
                None::<&str>,
            )?;

            let sep2 = PredefinedMenuItem::separator(app)?;

            // 3. 일괄 제어 명령
            let start_all_i = MenuItem::with_id(app, "start_all", "▶ 전체 서비스 시작", true, None::<&str>)?;
            let stop_all_i = MenuItem::with_id(app, "stop_all", "■ 전체 서비스 종료", true, None::<&str>)?;
            let restart_all_i = MenuItem::with_id(app, "restart_all", "전체 재기동", true, None::<&str>)?;
            let restart_backend_i = MenuItem::with_id(app, "restart_backend", "백엔드만 재기동 (WS/Sync/API)", true, None::<&str>)?;
            let restart_infra_i = MenuItem::with_id(app, "restart_infra", "인프라만 재기동 (MySQL/Kafka)", true, None::<&str>)?;

            let sep3 = PredefinedMenuItem::separator(app)?;

            // 4. 진단 도구 및 창 제어
            let check_proc_i = MenuItem::with_id(app, "check_proc", "프로세스 & 포트 점유 진단", true, None::<&str>)?;
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
                    .on_menu_event(move |app, event| {
                        let env = load_env_config();
                        let install = check_install_snapshot(&env.product_root);
                        match event.id.as_ref() {
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
                                let (mysql, ..) = check_all_service_ports(&env);
                                if !mysql && !install.mysql {
                                    return;
                                }
                                mark_pending_started();
                                if mysql {
                                    PENDING_MYSQL.store(2, Ordering::Relaxed);
                                    let _ = m_mysql.set_text(format!("[진행] [인프라] MySQL 9.71 (:{}) [종료 중...]", env.mysql_port));
                                    trigger_ui_command(app, "stop", "mysql");
                                } else {
                                    PENDING_MYSQL.store(1, Ordering::Relaxed);
                                    let _ = m_mysql.set_text(format!("[진행] [인프라] MySQL 9.71 (:{}) [기동 중...]", env.mysql_port));
                                    trigger_ui_command(app, "start", "mysql");
                                }
                            }
                            "status_kafka" => {
                                let (_, kafka, ..) = check_all_service_ports(&env);
                                if !kafka && !install.kafka {
                                    return;
                                }
                                mark_pending_started();
                                if kafka {
                                    PENDING_KAFKA.store(2, Ordering::Relaxed);
                                    let _ = m_kafka.set_text(format!("[진행] [인프라] Apache Kafka (:{}) [종료 중...]", env.kafka_port));
                                    trigger_ui_command(app, "stop", "kafka");
                                } else {
                                    PENDING_KAFKA.store(1, Ordering::Relaxed);
                                    let _ = m_kafka.set_text(format!("[진행] [인프라] Apache Kafka (:{}) [기동 중...]", env.kafka_port));
                                    trigger_ui_command(app, "start", "kafka");
                                }
                            }
                            "status_ws" => {
                                let (_, _, ws, ..) = check_all_service_ports(&env);
                                if !ws && !install.ws {
                                    return;
                                }
                                mark_pending_started();
                                if ws {
                                    PENDING_WS.store(2, Ordering::Relaxed);
                                    let _ = m_ws.set_text(format!("[진행] [코어] elvis-connect-ws (:{}) [종료 중...]", env.ws_port));
                                    trigger_ui_command(app, "stop", "ws");
                                } else {
                                    PENDING_WS.store(1, Ordering::Relaxed);
                                    let _ = m_ws.set_text(format!("[진행] [코어] elvis-connect-ws (:{}) [기동 중...]", env.ws_port));
                                    trigger_ui_command(app, "start", "ws");
                                }
                            }
                            "status_sync" => {
                                let (_, _, _, sync, ..) = check_all_service_ports(&env);
                                if !sync && !install.sync {
                                    return;
                                }
                                mark_pending_started();
                                if sync {
                                    PENDING_SYNC.store(2, Ordering::Relaxed);
                                    let _ = m_sync.set_text(format!("[진행] [코어] elvis-connect-sync (:{}) [종료 중...]", env.sync_port));
                                    trigger_ui_command(app, "stop", "sync");
                                } else {
                                    PENDING_SYNC.store(1, Ordering::Relaxed);
                                    let _ = m_sync.set_text(format!("[진행] [코어] elvis-connect-sync (:{}) [기동 중...]", env.sync_port));
                                    trigger_ui_command(app, "start", "sync");
                                }
                            }
                            "status_api" => {
                                let (_, _, _, _, api, ..) = check_all_service_ports(&env);
                                if !api && !install.api {
                                    return;
                                }
                                mark_pending_started();
                                if api {
                                    PENDING_API.store(2, Ordering::Relaxed);
                                    let _ = m_api.set_text(format!("[진행] [코어] elvis-control-api (:{}) [종료 중...]", env.api_port));
                                    trigger_ui_command(app, "stop", "api");
                                } else {
                                    PENDING_API.store(1, Ordering::Relaxed);
                                    let _ = m_api.set_text(format!("[진행] [코어] elvis-control-api (:{}) [기동 중...]", env.api_port));
                                    trigger_ui_command(app, "start", "api");
                                }
                            }
                            "start_all" => {
                                if install.installed_count() == 0 {
                                    return;
                                }
                                mark_pending_started();
                                PENDING_GROUP.store(1, Ordering::Relaxed);
                                if install.mysql { PENDING_MYSQL.store(1, Ordering::Relaxed); }
                                if install.kafka { PENDING_KAFKA.store(1, Ordering::Relaxed); }
                                if install.ws { PENDING_WS.store(1, Ordering::Relaxed); }
                                if install.sync { PENDING_SYNC.store(1, Ordering::Relaxed); }
                                if install.api { PENDING_API.store(1, Ordering::Relaxed); }
                                let _ = m_summary.set_text("[진행] [ STATUS: 설치된 서비스 순차 기동 중... ]");
                                let _ = m_start_all.set_text("[진행] 전체 서비스 시작 중...");
                                trigger_ui_command(app, "start_all", "");
                            }
                            "stop_all" => {
                                mark_pending_started();
                                PENDING_GROUP.store(2, Ordering::Relaxed);
                                PENDING_MYSQL.store(2, Ordering::Relaxed);
                                PENDING_KAFKA.store(2, Ordering::Relaxed);
                                PENDING_WS.store(2, Ordering::Relaxed);
                                PENDING_SYNC.store(2, Ordering::Relaxed);
                                PENDING_API.store(2, Ordering::Relaxed);
                                let _ = m_summary.set_text("[진행] [ STATUS: 전체 서비스 종료 중... ]");
                                let _ = m_stop_all.set_text("[진행] 전체 서비스 종료 중...");
                                trigger_ui_command(app, "stop_all", "");
                            }
                            "restart_all" => {
                                if install.installed_count() == 0 {
                                    return;
                                }
                                mark_pending_started();
                                PENDING_GROUP.store(3, Ordering::Relaxed);
                                if install.mysql { PENDING_MYSQL.store(3, Ordering::Relaxed); }
                                if install.kafka { PENDING_KAFKA.store(3, Ordering::Relaxed); }
                                if install.ws { PENDING_WS.store(3, Ordering::Relaxed); }
                                if install.sync { PENDING_SYNC.store(3, Ordering::Relaxed); }
                                if install.api { PENDING_API.store(3, Ordering::Relaxed); }
                                let _ = m_summary.set_text("[진행] [ STATUS: 전체 서비스 재기동 중... ]");
                                let _ = m_restart_all.set_text("[진행] 전체 재기동 중...");
                                trigger_ui_command(app, "restart_all", "");
                            }
                            "restart_backend" => {
                                if install.core_installed_count() == 0 {
                                    return;
                                }
                                mark_pending_started();
                                PENDING_GROUP.store(4, Ordering::Relaxed);
                                if install.ws { PENDING_WS.store(3, Ordering::Relaxed); }
                                if install.sync { PENDING_SYNC.store(3, Ordering::Relaxed); }
                                if install.api { PENDING_API.store(3, Ordering::Relaxed); }
                                let _ = m_restart_backend.set_text("[진행] 백엔드 재기동 중...");
                                trigger_ui_command(app, "restart_backend", "");
                            }
                            "restart_infra" => {
                                if install.infra_installed_count() == 0 {
                                    return;
                                }
                                mark_pending_started();
                                PENDING_GROUP.store(5, Ordering::Relaxed);
                                if install.mysql { PENDING_MYSQL.store(3, Ordering::Relaxed); }
                                if install.kafka { PENDING_KAFKA.store(3, Ordering::Relaxed); }
                                let _ = m_restart_infra.set_text("[진행] 인프라 재기동 중...");
                                trigger_ui_command(app, "restart_infra", "");
                            }
                            "quit" => {
                                app.exit(0);
                            }
                            _ => {}
                        }
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
            get_product_root,
            set_product_root,
            browse_product_root,
            check_middleware_status,
            start_middleware_detached,
            stop_middleware_detached,
            open_log_window,
            open_log_group_window,
            open_process_status_window,
            show_main_window,
            reset_tray_pending
        ])
        .run(tauri::generate_context!())
        .expect("error while running ELVIS launcher application");
}
