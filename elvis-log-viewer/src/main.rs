#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

use std::fs::{self, File};
use std::io::{Read, Seek, SeekFrom};
use std::path::PathBuf;
use std::process::Command;
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::{Arc, Mutex};
use std::thread;
use std::time::{Duration, Instant, SystemTime};

use eframe::egui;

#[derive(Clone, Copy, PartialEq, Eq)]
enum LogGroup {
    All,
    Core,
    Infra,
    Single(usize),
    Process,
}

impl LogGroup {
    fn from_str(s: &str) -> Self {
        match s.to_ascii_lowercase().as_str() {
            "core" => Self::Core,
            "infra" => Self::Infra,
            "ws" => Self::Single(ServiceIdx::Ws as usize),
            "sync" => Self::Single(ServiceIdx::Sync as usize),
            "api" => Self::Single(ServiceIdx::Api as usize),
            "mysql" => Self::Single(ServiceIdx::Mysql as usize),
            "kafka" => Self::Single(ServiceIdx::Kafka as usize),
            "process" | "proc" => Self::Process,
            _ => Self::All,
        }
    }
}

#[derive(Clone, Copy, PartialEq, Eq)]
enum ProcessScope {
    All,
    Infra,
    Core,
}

impl ProcessScope {
    fn from_str(s: &str) -> Self {
        match s.to_ascii_lowercase().as_str() {
            "infra" => Self::Infra,
            "core" => Self::Core,
            _ => Self::All,
        }
    }

    fn label(self) -> &'static str {
        match self {
            Self::All => "전체 시스템",
            Self::Infra => "미들웨어 (MySQL, Kafka)",
            Self::Core => "코어 엔진 (WS, Sync, API)",
        }
    }

    fn matches_group(self, group: &str) -> bool {
        match self {
            Self::All => true,
            Self::Infra => group == "infra",
            Self::Core => group == "core",
        }
    }
}

#[derive(Clone)]
struct PortRow {
    category: String,
    name: String,
    group: String,
    service_key: String,
    port: u16,
    running: bool,
    pid: Option<u32>,
    proc_name: String,
    mem_mb: f64,
}

#[derive(Clone)]
struct ProcRow {
    group: String,
    category: String,
    name: String,
    pid: u32,
    proc_name: String,
    port_str: String,
    mem_mb: f64,
    service_key: String,
}

#[derive(Default, Clone)]
struct ProcessSnapshot {
    checked_at: String,
    ports: Vec<PortRow>,
    procs: Vec<ProcRow>,
    error: Option<String>,
    loaded: bool,
}

#[derive(Clone, Copy, PartialEq, Eq)]
enum ServiceIdx {
    Ws = 0,
    Sync = 1,
    Api = 2,
    Mysql = 3,
    Kafka = 4,
}

struct PaneState {
    key: &'static str,
    slot: &'static str,
    title: &'static str,
    port: u16,
    accent: egui::Color32,
    badge_bg: egui::Color32,
    raw_lines: Vec<String>,
    exists: bool,
    last_len: u64,
    last_mtime: Option<SystemTime>,
    local_filter: String,
    auto_follow: bool,
    copied_until: Option<Instant>,
}

impl PaneState {
    fn new(
        key: &'static str,
        slot: &'static str,
        title: &'static str,
        port: u16,
        accent: egui::Color32,
        badge_bg: egui::Color32,
    ) -> Self {
        Self {
            key,
            slot,
            title,
            port,
            accent,
            badge_bg,
            raw_lines: Vec::new(),
            exists: false,
            last_len: 0,
            last_mtime: None,
            local_filter: String::new(),
            auto_follow: true,
            copied_until: None,
        }
    }

    fn refresh(&mut self, log_dir: &PathBuf, error_only: bool, force: bool) {
        let base_path = log_dir.join(format!("{}.log", self.key));
        let err_path = log_dir.join(format!("{}-err.log", self.key));
        let use_dedicated_err = error_only && err_path.exists();
        let target = if use_dedicated_err { err_path } else { base_path };

        let Ok(meta) = fs::metadata(&target) else {
            self.exists = false;
            self.raw_lines.clear();
            self.last_len = 0;
            self.last_mtime = None;
            return;
        };

        let len = meta.len();
        let mtime = meta.modified().ok();
        if !force && self.exists && len == self.last_len && mtime == self.last_mtime {
            return;
        }

        self.exists = true;
        self.last_len = len;
        self.last_mtime = mtime;

        let Ok(mut file) = File::open(&target) else {
            return;
        };

        const MAX_READ_BYTES: u64 = 262_144; // 마지막 256KB 읽기
        let start = len.saturating_sub(MAX_READ_BYTES);
        if file.seek(SeekFrom::Start(start)).is_err() {
            return;
        }

        let mut buf = Vec::with_capacity((len - start) as usize);
        if file.read_to_end(&mut buf).is_err() {
            return;
        }

        let text = String::from_utf8_lossy(&buf);
        let mut lines: Vec<String> = text.lines().map(strip_ansi_and_tabs).collect();

        if start > 0 && !lines.is_empty() {
            lines.remove(0);
        }

        if error_only && !use_dedicated_err {
            lines.retain(|l| is_error_or_warn_line(l));
        }

        const MAX_LINES: usize = 600;
        if lines.len() > MAX_LINES {
            lines.drain(0..(lines.len() - MAX_LINES));
        }

        self.raw_lines = lines;
    }

    fn clear_file(&mut self, log_dir: &PathBuf) {
        let path = log_dir.join(format!("{}.log", self.key));
        if path.exists() {
            let _ = fs::write(&path, "");
        }
        self.raw_lines.clear();
        self.last_len = 0;
        self.last_mtime = None;
    }
}

struct LogGuiApp {
    log_dir: PathBuf,
    ports: [u16; 5],
    group: LogGroup,
    process_scope: ProcessScope,
    error_only: bool,
    word_wrap: bool,
    is_live: bool,
    font_size: f32,
    global_filter: String,
    zoomed: Option<usize>,
    panes: [PaneState; 5],
    // 4축 마우스 드래그 분할 비율 (0.0 ~ 1.0)
    top_ratio: f32,
    top_left_ratio: f32,
    ws_ratio: f32,
    bottom_left_ratio: f32,
    last_refresh: Instant,
    // 프로세스/포트 진단 상태 (비동기 수집)
    process_live: bool,
    process_snapshot: Arc<Mutex<ProcessSnapshot>>,
    process_in_flight: Arc<AtomicBool>,
    last_process_refresh: Instant,
    kill_port_input: String,
    confirm_kill_pid: Option<u32>,
    process_feedback: Arc<Mutex<Option<(String, bool, Instant)>>>,
}

impl LogGuiApp {
    fn new(
        cc: &eframe::CreationContext<'_>,
        log_dir: PathBuf,
        group: LogGroup,
        process_scope: ProcessScope,
        error_only: bool,
        word_wrap: bool,
        ports: [u16; 5], // [mysql, kafka, ws, sync, api]
    ) -> Self {
        configure_light_theme_and_fonts(&cc.egui_ctx);

        let panes = [
            PaneState::new(
                "ws",
                "1",
                "elvis-connect-ws",
                ports[2],
                egui::Color32::from_rgb(2, 132, 199),   // sky-600
                egui::Color32::from_rgb(240, 249, 255), // sky-50
            ),
            PaneState::new(
                "sync",
                "3",
                "elvis-connect-sync",
                ports[3],
                egui::Color32::from_rgb(5, 150, 105),   // emerald-600
                egui::Color32::from_rgb(236, 253, 245), // emerald-50
            ),
            PaneState::new(
                "api",
                "2+4",
                "elvis-control-api",
                ports[4],
                egui::Color32::from_rgb(79, 70, 229),   // indigo-600
                egui::Color32::from_rgb(238, 242, 255), // indigo-50
            ),
            PaneState::new(
                "mysql",
                "5",
                "MySQL 9.71",
                ports[0],
                egui::Color32::from_rgb(217, 119, 6),   // amber-600
                egui::Color32::from_rgb(255, 251, 235), // amber-50
            ),
            PaneState::new(
                "kafka",
                "6",
                "Apache Kafka",
                ports[1],
                egui::Color32::from_rgb(124, 58, 237),  // violet-600
                egui::Color32::from_rgb(245, 243, 255), // violet-50
            ),
        ];

        let mut app = Self {
            log_dir,
            ports,
            group,
            process_scope,
            error_only,
            word_wrap,
            is_live: true,
            font_size: 12.5,
            global_filter: String::new(),
            zoomed: None,
            panes,
            top_ratio: 0.667,
            top_left_ratio: 0.5,
            ws_ratio: 0.5,
            bottom_left_ratio: 0.5,
            last_refresh: Instant::now() - Duration::from_secs(10),
            process_live: true,
            process_snapshot: Arc::new(Mutex::new(ProcessSnapshot::default())),
            process_in_flight: Arc::new(AtomicBool::new(false)),
            last_process_refresh: Instant::now() - Duration::from_secs(10),
            kill_port_input: String::new(),
            confirm_kill_pid: None,
            process_feedback: Arc::new(Mutex::new(None)),
        };
        if matches!(app.group, LogGroup::Process) {
            app.trigger_process_refresh(&cc.egui_ctx);
        } else {
            app.refresh_visible(true);
        }
        app
    }

    fn visible_indices(&self) -> Vec<usize> {
        match self.group {
            LogGroup::All => vec![
                ServiceIdx::Ws as usize,
                ServiceIdx::Sync as usize,
                ServiceIdx::Api as usize,
                ServiceIdx::Mysql as usize,
                ServiceIdx::Kafka as usize,
            ],
            LogGroup::Core => vec![
                ServiceIdx::Ws as usize,
                ServiceIdx::Sync as usize,
                ServiceIdx::Api as usize,
            ],
            LogGroup::Infra => vec![ServiceIdx::Mysql as usize, ServiceIdx::Kafka as usize],
            LogGroup::Single(idx) => vec![idx],
            LogGroup::Process => Vec::new(),
        }
    }

    fn refresh_visible(&mut self, force: bool) {
        let indices = self.visible_indices();
        for idx in indices {
            self.panes[idx].refresh(&self.log_dir, self.error_only, force);
        }
        self.last_refresh = Instant::now();
    }

    fn reset_ratios(&mut self) {
        self.top_ratio = 0.667;
        self.top_left_ratio = 0.5;
        self.ws_ratio = 0.5;
        self.bottom_left_ratio = 0.5;
    }

    fn render_pane_in_rect(&mut self, ui: &mut egui::Ui, idx: usize, rect: egui::Rect) {
        let is_single_mode = matches!(self.group, LogGroup::Single(_));
        let is_zoomed = self.zoomed == Some(idx);
        let word_wrap = self.word_wrap;
        let font_size = self.font_size;
        let error_only = self.error_only;
        let global_q = self.global_filter.trim().to_lowercase();
        let log_dir = self.log_dir.clone();

        let mut toggle_zoom = false;

        let child_ui_builder = egui::UiBuilder::new().max_rect(rect);
        ui.scope_builder(child_ui_builder, |ui| {
            ui.set_clip_rect(rect);
            let frame = egui::Frame::NONE
                .fill(egui::Color32::WHITE)
                .stroke(egui::Stroke::new(1.0_f32, egui::Color32::from_rgb(203, 213, 225)))
                .corner_radius(egui::CornerRadius::same(8));

            frame.show(ui, |ui| {
                ui.set_min_size(rect.size() - egui::vec2(2.0, 2.0));
                ui.set_max_size(rect.size() - egui::vec2(2.0, 2.0));

                let pane = &mut self.panes[idx];
                let local_q = pane.local_filter.trim().to_lowercase();

                let filtered_indices: Vec<usize> = pane
                    .raw_lines
                    .iter()
                    .enumerate()
                    .filter(|(_, line)| {
                        if global_q.is_empty() && local_q.is_empty() {
                            return true;
                        }
                        let lower = line.to_lowercase();
                        if !global_q.is_empty() && !lower.contains(&global_q) {
                            return false;
                        }
                        if !local_q.is_empty() && !lower.contains(&local_q) {
                            return false;
                        }
                        true
                    })
                    .map(|(i, _)| i)
                    .collect();

                // 1) 패널 헤더 바 (라이트 슬레이트)
                let header_frame = egui::Frame::NONE
                    .fill(egui::Color32::from_rgb(248, 250, 252))
                    .inner_margin(egui::Margin::symmetric(8, 5));

                header_frame.show(ui, |ui| {
                    ui.horizontal(|ui| {
                        egui::Frame::NONE
                            .fill(pane.badge_bg)
                            .stroke(egui::Stroke::new(1.0_f32, pane.accent))
                            .corner_radius(egui::CornerRadius::same(4))
                            .inner_margin(egui::Margin::symmetric(5, 1))
                            .show(ui, |ui| {
                                ui.label(
                                    egui::RichText::new(pane.slot)
                                        .monospace()
                                        .strong()
                                        .size(11.0)
                                        .color(pane.accent),
                                );
                            });

                        ui.label(
                            egui::RichText::new(pane.title)
                                .strong()
                                .size(13.0)
                                .color(egui::Color32::from_rgb(15, 23, 42)),
                        );

                        ui.label(
                            egui::RichText::new(format!(":{}", pane.port))
                                .monospace()
                                .size(11.5)
                                .color(egui::Color32::from_rgb(100, 116, 139)),
                        );

                        ui.label(
                            egui::RichText::new(format!("{}줄", filtered_indices.len()))
                                .monospace()
                                .size(11.0)
                                .color(egui::Color32::from_rgb(148, 163, 184)),
                        );

                        ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                            if !is_single_mode {
                                let zoom_label = if is_zoomed { "복귀" } else { "확대" };
                                let zoom_btn = egui::Button::new(
                                    egui::RichText::new(zoom_label).size(11.5).color(if is_zoomed {
                                        egui::Color32::WHITE
                                    } else {
                                        egui::Color32::from_rgb(51, 65, 85)
                                    }),
                                )
                                .fill(if is_zoomed {
                                    egui::Color32::from_rgb(2, 132, 199)
                                } else {
                                    egui::Color32::WHITE
                                });
                                if ui
                                    .add(zoom_btn)
                                    .on_hover_text("단일 패널 전체 확대/복귀 (ESC)")
                                    .clicked()
                                {
                                    toggle_zoom = true;
                                }
                            }

                            if ui
                                .add(
                                    egui::Button::new(
                                        egui::RichText::new("비우기")
                                            .size(11.5)
                                            .color(egui::Color32::from_rgb(225, 29, 72)),
                                    )
                                    .fill(egui::Color32::WHITE),
                                )
                                .on_hover_text("로그 파일 내용 비우기")
                                .clicked()
                            {
                                pane.clear_file(&log_dir);
                            }

                            let is_copied = pane
                                .copied_until
                                .map(|t| Instant::now() < t)
                                .unwrap_or(false);
                            if ui
                                .add(
                                    egui::Button::new(
                                        egui::RichText::new(if is_copied { "복사됨" } else { "복사" })
                                            .size(11.5)
                                            .color(if is_copied {
                                                egui::Color32::from_rgb(5, 150, 105)
                                            } else {
                                                egui::Color32::from_rgb(51, 65, 85)
                                            }),
                                    )
                                    .fill(egui::Color32::WHITE),
                                )
                                .on_hover_text("표시된 로그 클립보드 복사")
                                .clicked()
                            {
                                let joined: String = filtered_indices
                                    .iter()
                                    .map(|&i| pane.raw_lines[i].as_str())
                                    .collect::<Vec<_>>()
                                    .join("\n");
                                ui.ctx().copy_text(joined);
                                pane.copied_until =
                                    Some(Instant::now() + Duration::from_millis(1300));
                            }

                            let follow_btn = egui::Button::new(
                                egui::RichText::new(if pane.auto_follow { "최신↓" } else { "고정" })
                                    .size(11.5)
                                    .color(if pane.auto_follow {
                                        egui::Color32::from_rgb(2, 132, 199)
                                    } else {
                                        egui::Color32::from_rgb(100, 116, 139)
                                    }),
                            )
                            .fill(if pane.auto_follow {
                                egui::Color32::from_rgb(240, 249, 255)
                            } else {
                                egui::Color32::WHITE
                            });
                            if ui
                                .add(follow_btn)
                                .on_hover_text("최신 로그 하단 자동 추적 ON/OFF")
                                .clicked()
                            {
                                pane.auto_follow = !pane.auto_follow;
                            }

                            if rect.width() > 380.0 {
                                ui.add(
                                    egui::TextEdit::singleline(&mut pane.local_filter)
                                        .desired_width(96.0)
                                        .hint_text("필터...")
                                        .font(egui::FontId::proportional(11.5)),
                                );
                            }
                        });
                    });
                });

                // 헤더 구분선
                let sep_rect = ui.available_rect_before_wrap();
                ui.painter().hline(
                    sep_rect.x_range(),
                    sep_rect.top(),
                    egui::Stroke::new(1.0_f32, egui::Color32::from_rgb(226, 232, 240)),
                );

                // 2) 로그 본문 영역 (Word Wrap OFF = 네이티브 가로+세로 스크롤바)
                let body_margin = egui::Frame::NONE
                    .fill(egui::Color32::WHITE)
                    .inner_margin(egui::Margin::symmetric(6, 4));

                body_margin.show(ui, |ui| {
                    let avail = ui.available_size();
                    ui.set_min_size(avail);

                    if !pane.exists {
                        ui.centered_and_justified(|ui| {
                            ui.label(
                                egui::RichText::new(format!(
                                    "[대기] {}.log 파일 생성 대기 중...",
                                    pane.key
                                ))
                                .size(12.5)
                                .color(egui::Color32::from_rgb(148, 163, 184)),
                            );
                        });
                        return;
                    }

                    if filtered_indices.is_empty() {
                        ui.centered_and_justified(|ui| {
                            ui.label(
                                egui::RichText::new(if error_only {
                                    "감지된 에러/경고 로그가 없습니다."
                                } else {
                                    "표시할 로그가 없습니다."
                                })
                                .size(12.5)
                                .color(egui::Color32::from_rgb(148, 163, 184)),
                            );
                        });
                        return;
                    }

                    let scroll = if word_wrap {
                        egui::ScrollArea::vertical()
                    } else {
                        egui::ScrollArea::both()
                    }
                    .id_salt(format!("log-scroll-{}-{}", pane.key, word_wrap))
                    .auto_shrink([false, false])
                    .stick_to_bottom(pane.auto_follow);

                    scroll.show(ui, |ui| {
                        ui.style_mut().wrap_mode = Some(if word_wrap {
                            egui::TextWrapMode::Wrap
                        } else {
                            egui::TextWrapMode::Extend
                        });
                        ui.spacing_mut().item_spacing.y = 2.0;

                        for &line_idx in &filtered_indices {
                            let line = &pane.raw_lines[line_idx];
                            let (fg, bg_opt, bold) = classify_line_colors(line);
                            let mut rt = egui::RichText::new(line)
                                .monospace()
                                .size(font_size)
                                .color(fg);
                            if bold {
                                rt = rt.strong();
                            }
                            if let Some(bg) = bg_opt {
                                rt = rt.background_color(bg);
                            }
                            ui.add(egui::Label::new(rt).selectable(true));
                        }
                    });
                });
            });
        });

        if toggle_zoom {
            self.zoomed = if is_zoomed { None } else { Some(idx) };
        }
    }

    fn trigger_process_refresh(&mut self, ctx: &egui::Context) {
        if self
            .process_in_flight
            .compare_exchange(false, true, Ordering::SeqCst, Ordering::SeqCst)
            .is_err()
        {
            return;
        }
        self.last_process_refresh = Instant::now();
        let ports = self.ports;
        let snapshot_arc = Arc::clone(&self.process_snapshot);
        let in_flight = Arc::clone(&self.process_in_flight);
        let ctx_clone = ctx.clone();

        thread::spawn(move || {
            let snap = collect_process_snapshot(ports);
            if let Ok(mut guard) = snapshot_arc.lock() {
                *guard = snap;
            }
            in_flight.store(false, Ordering::SeqCst);
            ctx_clone.request_repaint();
        });
    }

    fn trigger_kill_pid(&mut self, ctx: &egui::Context, pid: u32, label: String) {
        let feedback_arc = Arc::clone(&self.process_feedback);
        let snapshot_arc = Arc::clone(&self.process_snapshot);
        let in_flight = Arc::clone(&self.process_in_flight);
        let ports = self.ports;
        let ctx_clone = ctx.clone();
        self.confirm_kill_pid = None;

        thread::spawn(move || {
            let result = kill_process_by_pid(pid);
            if let Ok(mut fb) = feedback_arc.lock() {
                match result {
                    Ok(()) => {
                        *fb = Some((
                            format!("PID {} ({}) 강제 종료를 완료했습니다.", pid, label),
                            true,
                            Instant::now(),
                        ));
                    }
                    Err(e) => {
                        *fb = Some((
                            format!("PID {} 종료 실패: {}", pid, e),
                            false,
                            Instant::now(),
                        ));
                    }
                }
            }
            thread::sleep(Duration::from_millis(350));
            if in_flight
                .compare_exchange(false, true, Ordering::SeqCst, Ordering::SeqCst)
                .is_ok()
            {
                let snap = collect_process_snapshot(ports);
                if let Ok(mut guard) = snapshot_arc.lock() {
                    *guard = snap;
                }
                in_flight.store(false, Ordering::SeqCst);
            }
            ctx_clone.request_repaint();
        });
    }

    fn trigger_kill_port(&mut self, ctx: &egui::Context, port: u16) {
        let feedback_arc = Arc::clone(&self.process_feedback);
        let snapshot_arc = Arc::clone(&self.process_snapshot);
        let in_flight = Arc::clone(&self.process_in_flight);
        let ports = self.ports;
        let ctx_clone = ctx.clone();

        thread::spawn(move || {
            let result = kill_process_by_port(port);
            if let Ok(mut fb) = feedback_arc.lock() {
                match result {
                    Ok(msg) => *fb = Some((msg, true, Instant::now())),
                    Err(msg) => *fb = Some((msg, false, Instant::now())),
                }
            }
            thread::sleep(Duration::from_millis(350));
            if in_flight
                .compare_exchange(false, true, Ordering::SeqCst, Ordering::SeqCst)
                .is_ok()
            {
                let snap = collect_process_snapshot(ports);
                if let Ok(mut guard) = snapshot_arc.lock() {
                    *guard = snap;
                }
                in_flight.store(false, Ordering::SeqCst);
            }
            ctx_clone.request_repaint();
        });
    }

    fn render_process_monitor(&mut self, ui: &mut egui::Ui, ctx: &egui::Context) {
        let snapshot = self
            .process_snapshot
            .lock()
            .map(|g| g.clone())
            .unwrap_or_default();
        let is_loading = self.process_in_flight.load(Ordering::SeqCst);

        // 6초 이상 지난 피드백 자동 만료
        let feedback = self.process_feedback.lock().ok().and_then(|mut g| {
            if let Some((_, _, at)) = g.as_ref() {
                if at.elapsed() > Duration::from_secs(6) {
                    *g = None;
                }
            }
            g.clone()
        });

        let filtered_ports: Vec<&PortRow> = snapshot
            .ports
            .iter()
            .filter(|r| self.process_scope.matches_group(&r.group))
            .collect();
        let filtered_procs: Vec<&ProcRow> = snapshot
            .procs
            .iter()
            .filter(|r| self.process_scope.matches_group(&r.group))
            .collect();

        let active_port_count = filtered_ports.iter().filter(|r| r.running).count();
        let total_port_count = filtered_ports.len();
        let total_mem_mb: f64 = filtered_procs.iter().map(|p| p.mem_mb).sum();

        let mut pending_jump_service: Option<usize> = None;
        let mut pending_kill_pid: Option<(u32, String)> = None;
        let mut pending_kill_port: Option<u16> = None;
        let mut clear_feedback = false;

        egui::ScrollArea::both()
            .id_salt("process_monitor_scroll")
            .auto_shrink([false, false])
            .show(ui, |ui| {
                // 1) 상단 요약 & 특정 포트 강제 종료 카드
                egui::Frame::NONE
                    .fill(egui::Color32::WHITE)
                    .stroke(egui::Stroke::new(
                        1.0_f32,
                        egui::Color32::from_rgb(203, 213, 225),
                    ))
                    .corner_radius(egui::CornerRadius::same(8))
                    .inner_margin(egui::Margin::symmetric(12, 10))
                    .show(ui, |ui| {
                        ui.horizontal_wrapped(|ui| {
                            ui.label(
                                egui::RichText::new(format!(
                                    "프로세스 및 포트 점유 진단 · {}",
                                    self.process_scope.label()
                                ))
                                .strong()
                                .size(14.0)
                                .color(egui::Color32::from_rgb(15, 23, 42)),
                            );

                            // 활성 포트 배지
                            let (badge_fg, badge_bg) = if active_port_count > 0 {
                                (
                                    egui::Color32::from_rgb(4, 120, 87),
                                    egui::Color32::from_rgb(236, 253, 245),
                                )
                            } else {
                                (
                                    egui::Color32::from_rgb(100, 116, 139),
                                    egui::Color32::from_rgb(241, 245, 249),
                                )
                            };
                            egui::Frame::NONE
                                .fill(badge_bg)
                                .stroke(egui::Stroke::new(1.0_f32, badge_fg))
                                .corner_radius(egui::CornerRadius::same(10))
                                .inner_margin(egui::Margin::symmetric(8, 2))
                                .show(ui, |ui| {
                                    ui.label(
                                        egui::RichText::new(format!(
                                            "{}/{} LISTEN",
                                            active_port_count, total_port_count
                                        ))
                                        .monospace()
                                        .strong()
                                        .size(11.5)
                                        .color(badge_fg),
                                    );
                                });

                            egui::Frame::NONE
                                .fill(egui::Color32::from_rgb(248, 250, 252))
                                .stroke(egui::Stroke::new(
                                    1.0_f32,
                                    egui::Color32::from_rgb(226, 232, 240),
                                ))
                                .corner_radius(egui::CornerRadius::same(10))
                                .inner_margin(egui::Margin::symmetric(8, 2))
                                .show(ui, |ui| {
                                    ui.label(
                                        egui::RichText::new(format!(
                                            "감지 프로세스 {}개 · {:.1} MB",
                                            filtered_procs.len(),
                                            total_mem_mb
                                        ))
                                        .monospace()
                                        .size(11.5)
                                        .color(egui::Color32::from_rgb(71, 85, 105)),
                                    );
                                });

                            let time_text = if is_loading && !snapshot.loaded {
                                "조회 중...".to_string()
                            } else if snapshot.checked_at.is_empty() {
                                "-".to_string()
                            } else if is_loading {
                                format!("점검 {} (갱신 중)", snapshot.checked_at)
                            } else {
                                format!("점검 {}", snapshot.checked_at)
                            };
                            ui.label(
                                egui::RichText::new(time_text)
                                    .monospace()
                                    .size(11.5)
                                    .color(egui::Color32::from_rgb(100, 116, 139)),
                            );

                            ui.with_layout(
                                egui::Layout::right_to_left(egui::Align::Center),
                                |ui| {
                                    let kill_btn = egui::Button::new(
                                        egui::RichText::new("포트 종료")
                                            .size(12.0)
                                            .strong()
                                            .color(egui::Color32::from_rgb(225, 29, 72)),
                                    )
                                    .fill(egui::Color32::from_rgb(255, 241, 242));
                                    if ui
                                        .add(kill_btn)
                                        .on_hover_text("입력한 포트를 점유 중인 프로세스를 강제 종료합니다")
                                        .clicked()
                                    {
                                        if let Ok(port_num) =
                                            self.kill_port_input.trim().parse::<u16>()
                                        {
                                            if port_num > 0 {
                                                pending_kill_port = Some(port_num);
                                            }
                                        }
                                    }

                                    let resp = ui.add(
                                        egui::TextEdit::singleline(&mut self.kill_port_input)
                                            .desired_width(92.0)
                                            .hint_text("포트 (예: 9082)")
                                            .font(egui::FontId::monospace(12.0)),
                                    );
                                    if resp.lost_focus()
                                        && ui.input(|i| i.key_pressed(egui::Key::Enter))
                                    {
                                        if let Ok(port_num) =
                                            self.kill_port_input.trim().parse::<u16>()
                                        {
                                            if port_num > 0 {
                                                pending_kill_port = Some(port_num);
                                            }
                                        }
                                    }
                                },
                            );
                        });
                    });

                // 액션 피드백 또는 오류 배너
                if let Some((msg, ok, _)) = feedback {
                    ui.add_space(6.0);
                    let (fg, bg, border) = if ok {
                        (
                            egui::Color32::from_rgb(4, 120, 87),
                            egui::Color32::from_rgb(236, 253, 245),
                            egui::Color32::from_rgb(167, 243, 208),
                        )
                    } else {
                        (
                            egui::Color32::from_rgb(190, 18, 60),
                            egui::Color32::from_rgb(255, 241, 242),
                            egui::Color32::from_rgb(254, 205, 211),
                        )
                    };
                    egui::Frame::NONE
                        .fill(bg)
                        .stroke(egui::Stroke::new(1.0_f32, border))
                        .corner_radius(egui::CornerRadius::same(6))
                        .inner_margin(egui::Margin::symmetric(10, 6))
                        .show(ui, |ui| {
                            ui.horizontal(|ui| {
                                ui.label(egui::RichText::new(msg).strong().size(12.5).color(fg));
                                ui.with_layout(
                                    egui::Layout::right_to_left(egui::Align::Center),
                                    |ui| {
                                        if ui.small_button("닫기").clicked() {
                                            clear_feedback = true;
                                        }
                                    },
                                );
                            });
                        });
                } else if let Some(ref err) = snapshot.error {
                    ui.add_space(6.0);
                    egui::Frame::NONE
                        .fill(egui::Color32::from_rgb(255, 241, 242))
                        .stroke(egui::Stroke::new(
                            1.0_f32,
                            egui::Color32::from_rgb(254, 205, 211),
                        ))
                        .corner_radius(egui::CornerRadius::same(6))
                        .inner_margin(egui::Margin::symmetric(10, 6))
                        .show(ui, |ui| {
                            ui.label(
                                egui::RichText::new(format!("진단 수집 오류: {}", err))
                                    .size(12.0)
                                    .color(egui::Color32::from_rgb(190, 18, 60)),
                            );
                        });
                }

                ui.add_space(8.0);

                // 2) 포트 점유 현황 카드 (LISTEN)
                egui::Frame::NONE
                    .fill(egui::Color32::WHITE)
                    .stroke(egui::Stroke::new(
                        1.0_f32,
                        egui::Color32::from_rgb(203, 213, 225),
                    ))
                    .corner_radius(egui::CornerRadius::same(8))
                    .inner_margin(egui::Margin::same(12))
                    .show(ui, |ui| {
                        ui.horizontal(|ui| {
                            ui.label(
                                egui::RichText::new("1. 서비스 포트 점유 현황 (TCP LISTEN)")
                                    .strong()
                                    .size(13.5)
                                    .color(egui::Color32::from_rgb(15, 23, 42)),
                            );
                        });
                        ui.add_space(6.0);

                        if !snapshot.loaded && is_loading {
                            ui.label(
                                egui::RichText::new("포트 및 프로세스 정보를 불러오는 중입니다...")
                                    .size(12.5)
                                    .color(egui::Color32::from_rgb(100, 116, 139)),
                            );
                        } else {
                            egui::Grid::new("port_status_grid")
                                .num_columns(8)
                                .striped(true)
                                .spacing([14.0, 6.0])
                                .min_col_width(56.0)
                                .show(ui, |ui| {
                                    for h in [
                                        "구분",
                                        "서비스명",
                                        "포트",
                                        "상태",
                                        "PID",
                                        "프로세스명",
                                        "메모리",
                                        "작업",
                                    ] {
                                        ui.label(
                                            egui::RichText::new(h)
                                                .strong()
                                                .size(12.0)
                                                .color(egui::Color32::from_rgb(71, 85, 105)),
                                        );
                                    }
                                    ui.end_row();

                                    for row in &filtered_ports {
                                        ui.label(
                                            egui::RichText::new(&row.category)
                                                .size(12.0)
                                                .color(egui::Color32::from_rgb(71, 85, 105)),
                                        );
                                        ui.label(
                                            egui::RichText::new(&row.name)
                                                .strong()
                                                .size(self.font_size)
                                                .color(egui::Color32::from_rgb(15, 23, 42)),
                                        );
                                        ui.label(
                                            egui::RichText::new(format!(":{}", row.port))
                                                .monospace()
                                                .size(self.font_size)
                                                .color(egui::Color32::from_rgb(2, 132, 199)),
                                        );

                                        if row.running {
                                            egui::Frame::NONE
                                                .fill(egui::Color32::from_rgb(236, 253, 245))
                                                .corner_radius(egui::CornerRadius::same(4))
                                                .inner_margin(egui::Margin::symmetric(6, 1))
                                                .show(ui, |ui| {
                                                    ui.label(
                                                        egui::RichText::new("RUNNING")
                                                            .monospace()
                                                            .strong()
                                                            .size(11.0)
                                                            .color(egui::Color32::from_rgb(
                                                                4, 120, 87,
                                                            )),
                                                    );
                                                });
                                        } else {
                                            egui::Frame::NONE
                                                .fill(egui::Color32::from_rgb(241, 245, 249))
                                                .corner_radius(egui::CornerRadius::same(4))
                                                .inner_margin(egui::Margin::symmetric(6, 1))
                                                .show(ui, |ui| {
                                                    ui.label(
                                                        egui::RichText::new("STOPPED")
                                                            .monospace()
                                                            .size(11.0)
                                                            .color(egui::Color32::from_rgb(
                                                                148, 163, 184,
                                                            )),
                                                    );
                                                });
                                        }

                                        let pid_str = row
                                            .pid
                                            .map(|p| p.to_string())
                                            .unwrap_or_else(|| "-".to_string());
                                        ui.label(
                                            egui::RichText::new(pid_str)
                                                .monospace()
                                                .size(self.font_size)
                                                .color(egui::Color32::from_rgb(30, 41, 59)),
                                        );
                                        ui.label(
                                            egui::RichText::new(if row.proc_name.is_empty() {
                                                "-"
                                            } else {
                                                &row.proc_name
                                            })
                                            .monospace()
                                            .size(self.font_size)
                                            .color(egui::Color32::from_rgb(71, 85, 105)),
                                        );
                                        let mem_str = if row.running {
                                            format!("{:.1} MB", row.mem_mb)
                                        } else {
                                            "-".to_string()
                                        };
                                        ui.label(
                                            egui::RichText::new(mem_str)
                                                .monospace()
                                                .size(self.font_size)
                                                .color(egui::Color32::from_rgb(51, 65, 85)),
                                        );

                                        ui.horizontal(|ui| {
                                            if let Some(svc_idx) =
                                                service_key_to_idx(&row.service_key)
                                            {
                                                if ui
                                                    .add(
                                                        egui::Button::new(
                                                            egui::RichText::new("로그")
                                                                .size(11.5)
                                                                .color(egui::Color32::from_rgb(
                                                                    2, 132, 199,
                                                                )),
                                                        )
                                                        .fill(egui::Color32::from_rgb(
                                                            240, 249, 255,
                                                        )),
                                                    )
                                                    .on_hover_text("해당 서비스 실시간 로그 보기")
                                                    .clicked()
                                                {
                                                    pending_jump_service = Some(svc_idx);
                                                }
                                            }

                                            if let Some(pid) = row.pid {
                                                if self.confirm_kill_pid == Some(pid) {
                                                    if ui
                                                        .add(
                                                            egui::Button::new(
                                                                egui::RichText::new("확인")
                                                                    .size(11.5)
                                                                    .strong()
                                                                    .color(egui::Color32::WHITE),
                                                            )
                                                            .fill(egui::Color32::from_rgb(
                                                                225, 29, 72,
                                                            )),
                                                        )
                                                        .clicked()
                                                    {
                                                        pending_kill_pid = Some((
                                                            pid,
                                                            format!(
                                                                "{} :{}",
                                                                row.name, row.port
                                                            ),
                                                        ));
                                                    }
                                                    if ui
                                                        .add(
                                                            egui::Button::new(
                                                                egui::RichText::new("취소")
                                                                    .size(11.5)
                                                                    .color(
                                                                        egui::Color32::from_rgb(
                                                                            71, 85, 105,
                                                                        ),
                                                                    ),
                                                            )
                                                            .fill(egui::Color32::WHITE),
                                                        )
                                                        .clicked()
                                                    {
                                                        self.confirm_kill_pid = None;
                                                    }
                                                } else if ui
                                                    .add(
                                                        egui::Button::new(
                                                            egui::RichText::new("종료")
                                                                .size(11.5)
                                                                .color(egui::Color32::from_rgb(
                                                                    225, 29, 72,
                                                                )),
                                                        )
                                                        .fill(egui::Color32::from_rgb(
                                                            255, 241, 242,
                                                        )),
                                                    )
                                                    .on_hover_text(format!(
                                                        "PID {} 프로세스 강제 종료",
                                                        pid
                                                    ))
                                                    .clicked()
                                                {
                                                    self.confirm_kill_pid = Some(pid);
                                                }
                                            }
                                        });
                                        ui.end_row();
                                    }
                                });
                        }
                    });

                ui.add_space(8.0);

                // 3) 감지된 세부 서비스 프로세스 카드 (mysqld / java / node)
                egui::Frame::NONE
                    .fill(egui::Color32::WHITE)
                    .stroke(egui::Stroke::new(
                        1.0_f32,
                        egui::Color32::from_rgb(203, 213, 225),
                    ))
                    .corner_radius(egui::CornerRadius::same(8))
                    .inner_margin(egui::Margin::same(12))
                    .show(ui, |ui| {
                        ui.label(
                            egui::RichText::new(
                                "2. 실행 중인 서비스 세부 PID 현황 (mysqld / java / node)",
                            )
                            .strong()
                            .size(13.5)
                            .color(egui::Color32::from_rgb(15, 23, 42)),
                        );
                        ui.add_space(6.0);

                        if filtered_procs.is_empty() {
                            ui.label(
                                egui::RichText::new(if is_loading && !snapshot.loaded {
                                    "프로세스 목록을 스캔하는 중입니다..."
                                } else {
                                    "감지된 서비스 프로세스가 없습니다."
                                })
                                .size(12.5)
                                .color(egui::Color32::from_rgb(148, 163, 184)),
                            );
                        } else {
                            egui::Grid::new("proc_detail_grid")
                                .num_columns(7)
                                .striped(true)
                                .spacing([14.0, 6.0])
                                .min_col_width(56.0)
                                .show(ui, |ui| {
                                    for h in [
                                        "구분",
                                        "서비스 / 프로세스",
                                        "포트",
                                        "PID",
                                        "실행 파일",
                                        "메모리",
                                        "작업",
                                    ] {
                                        ui.label(
                                            egui::RichText::new(h)
                                                .strong()
                                                .size(12.0)
                                                .color(egui::Color32::from_rgb(71, 85, 105)),
                                        );
                                    }
                                    ui.end_row();

                                    for p in &filtered_procs {
                                        ui.label(
                                            egui::RichText::new(&p.category)
                                                .size(12.0)
                                                .color(egui::Color32::from_rgb(71, 85, 105)),
                                        );
                                        ui.label(
                                            egui::RichText::new(&p.name)
                                                .strong()
                                                .size(self.font_size)
                                                .color(egui::Color32::from_rgb(15, 23, 42)),
                                        );
                                        ui.label(
                                            egui::RichText::new(&p.port_str)
                                                .monospace()
                                                .size(self.font_size)
                                                .color(egui::Color32::from_rgb(2, 132, 199)),
                                        );
                                        ui.label(
                                            egui::RichText::new(p.pid.to_string())
                                                .monospace()
                                                .size(self.font_size)
                                                .color(egui::Color32::from_rgb(30, 41, 59)),
                                        );
                                        ui.label(
                                            egui::RichText::new(&p.proc_name)
                                                .monospace()
                                                .size(self.font_size)
                                                .color(egui::Color32::from_rgb(71, 85, 105)),
                                        );
                                        ui.label(
                                            egui::RichText::new(format!("{:.1} MB", p.mem_mb))
                                                .monospace()
                                                .size(self.font_size)
                                                .color(egui::Color32::from_rgb(51, 65, 85)),
                                        );

                                        ui.horizontal(|ui| {
                                            if let Some(svc_idx) =
                                                service_key_to_idx(&p.service_key)
                                            {
                                                if ui
                                                    .add(
                                                        egui::Button::new(
                                                            egui::RichText::new("로그")
                                                                .size(11.5)
                                                                .color(egui::Color32::from_rgb(
                                                                    2, 132, 199,
                                                                )),
                                                        )
                                                        .fill(egui::Color32::from_rgb(
                                                            240, 249, 255,
                                                        )),
                                                    )
                                                    .clicked()
                                                {
                                                    pending_jump_service = Some(svc_idx);
                                                }
                                            }

                                            if self.confirm_kill_pid == Some(p.pid) {
                                                if ui
                                                    .add(
                                                        egui::Button::new(
                                                            egui::RichText::new("확인")
                                                                .size(11.5)
                                                                .strong()
                                                                .color(egui::Color32::WHITE),
                                                        )
                                                        .fill(egui::Color32::from_rgb(
                                                            225, 29, 72,
                                                        )),
                                                    )
                                                    .clicked()
                                                {
                                                    pending_kill_pid =
                                                        Some((p.pid, p.name.clone()));
                                                }
                                                if ui
                                                    .add(
                                                        egui::Button::new(
                                                            egui::RichText::new("취소")
                                                                .size(11.5)
                                                                .color(egui::Color32::from_rgb(
                                                                    71, 85, 105,
                                                                )),
                                                        )
                                                        .fill(egui::Color32::WHITE),
                                                    )
                                                    .clicked()
                                                {
                                                    self.confirm_kill_pid = None;
                                                }
                                            } else if ui
                                                .add(
                                                    egui::Button::new(
                                                        egui::RichText::new("종료")
                                                            .size(11.5)
                                                            .color(egui::Color32::from_rgb(
                                                                225, 29, 72,
                                                            )),
                                                    )
                                                    .fill(egui::Color32::from_rgb(255, 241, 242)),
                                                )
                                                .clicked()
                                            {
                                                self.confirm_kill_pid = Some(p.pid);
                                            }
                                        });
                                        ui.end_row();
                                    }
                                });
                        }
                    });
            });

        if clear_feedback {
            if let Ok(mut fb) = self.process_feedback.lock() {
                *fb = None;
            }
        }
        if let Some(svc_idx) = pending_jump_service {
            self.group = LogGroup::Single(svc_idx);
            self.zoomed = None;
            self.refresh_visible(true);
        }
        if let Some((pid, label)) = pending_kill_pid {
            self.trigger_kill_pid(ctx, pid, label);
        }
        if let Some(port) = pending_kill_port {
            self.kill_port_input.clear();
            self.trigger_kill_port(ctx, port);
        }
    }
}

impl eframe::App for LogGuiApp {
    fn update(&mut self, ctx: &egui::Context, _frame: &mut eframe::Frame) {
        if ctx.input(|i| i.key_pressed(egui::Key::Escape)) {
            if self.zoomed.is_some() {
                self.zoomed = None;
            }
            if self.confirm_kill_pid.is_some() {
                self.confirm_kill_pid = None;
            }
        }

        let is_process_mode = matches!(self.group, LogGroup::Process);
        if is_process_mode {
            if self.process_live
                && self.last_process_refresh.elapsed() >= Duration::from_millis(2500)
            {
                self.trigger_process_refresh(ctx);
            }
            if self.process_live || self.process_in_flight.load(Ordering::SeqCst) {
                ctx.request_repaint_after(Duration::from_millis(500));
            }
        } else {
            if self.is_live && self.last_refresh.elapsed() >= Duration::from_millis(300) {
                self.refresh_visible(false);
            }
            if self.is_live {
                ctx.request_repaint_after(Duration::from_millis(300));
            }
        }

        // 1. 상단 통합 제어 툴바 (라이트 테마)
        egui::TopBottomPanel::top("top_toolbar")
            .frame(
                egui::Frame::NONE
                    .fill(egui::Color32::WHITE)
                    .inner_margin(egui::Margin::symmetric(10, 6))
                    .stroke(egui::Stroke::new(1.0_f32, egui::Color32::from_rgb(226, 232, 240))),
            )
            .show(ctx, |ui| {
                ui.horizontal_wrapped(|ui| {
                    ui.label(
                        egui::RichText::new("ELVIS Log Viewer")
                            .strong()
                            .size(14.5)
                            .color(egui::Color32::from_rgb(15, 23, 42)),
                    );

                    ui.separator();

                    let groups = [
                        (LogGroup::All, "전체 5분할"),
                        (LogGroup::Core, "코어 3분할"),
                        (LogGroup::Infra, "미들웨어 2분할"),
                    ];
                    for (grp, label) in groups {
                        let active = self.group == grp;
                        let btn = egui::Button::new(
                            egui::RichText::new(label)
                                .size(12.0)
                                .strong()
                                .color(if active {
                                    egui::Color32::from_rgb(2, 132, 199)
                                } else {
                                    egui::Color32::from_rgb(71, 85, 105)
                                }),
                        )
                        .fill(if active {
                            egui::Color32::from_rgb(240, 249, 255)
                        } else {
                            egui::Color32::from_rgb(241, 245, 249)
                        });
                        if ui.add(btn).clicked() && !active {
                            self.group = grp;
                            self.zoomed = None;
                            self.refresh_visible(true);
                        }
                    }

                    ui.separator();

                    // 개별 프로세스 단일 보기 전환 버튼
                    let singles = [
                        (ServiceIdx::Ws as usize, "WS"),
                        (ServiceIdx::Sync as usize, "SYNC"),
                        (ServiceIdx::Api as usize, "API"),
                        (ServiceIdx::Mysql as usize, "MySQL"),
                        (ServiceIdx::Kafka as usize, "Kafka"),
                    ];
                    for (idx, label) in singles {
                        let target = LogGroup::Single(idx);
                        let active = self.group == target;
                        let accent = self.panes[idx].accent;
                        let bg = self.panes[idx].badge_bg;
                        let btn = egui::Button::new(
                            egui::RichText::new(label)
                                .size(11.5)
                                .strong()
                                .color(if active {
                                    accent
                                } else {
                                    egui::Color32::from_rgb(71, 85, 105)
                                }),
                        )
                        .fill(if active {
                            bg
                        } else {
                            egui::Color32::from_rgb(248, 250, 252)
                        });
                        if ui.add(btn).clicked() && !active {
                            self.group = target;
                            self.zoomed = None;
                            self.refresh_visible(true);
                        }
                    }

                    ui.separator();

                    // 프로세스/포트 실시간 진단 탭
                    let proc_active = matches!(self.group, LogGroup::Process);
                    let proc_btn = egui::Button::new(
                        egui::RichText::new("프로세스")
                            .size(12.0)
                            .strong()
                            .color(if proc_active {
                                egui::Color32::WHITE
                            } else {
                                egui::Color32::from_rgb(4, 120, 87)
                            }),
                    )
                    .fill(if proc_active {
                        egui::Color32::from_rgb(5, 150, 105)
                    } else {
                        egui::Color32::from_rgb(236, 253, 245)
                    });
                    if ui
                        .add(proc_btn)
                        .on_hover_text("포트 점유 및 실행 프로세스(PID/메모리) 실시간 진단")
                        .clicked()
                        && !proc_active
                    {
                        self.group = LogGroup::Process;
                        self.zoomed = None;
                        self.trigger_process_refresh(ctx);
                    }

                    ui.separator();

                    if matches!(self.group, LogGroup::Process) {
                        // 프로세스 모드 전용 범위/갱신 컨트롤
                        let scopes = [
                            (ProcessScope::All, "전체 (8)"),
                            (ProcessScope::Infra, "미들웨어 (2)"),
                            (ProcessScope::Core, "코어 엔진 (3)"),
                        ];
                        for (sc, label) in scopes {
                            let active = self.process_scope == sc;
                            let btn = egui::Button::new(
                                egui::RichText::new(label)
                                    .size(12.0)
                                    .strong()
                                    .color(if active {
                                        egui::Color32::from_rgb(2, 132, 199)
                                    } else {
                                        egui::Color32::from_rgb(71, 85, 105)
                                    }),
                            )
                            .fill(if active {
                                egui::Color32::from_rgb(240, 249, 255)
                            } else {
                                egui::Color32::from_rgb(241, 245, 249)
                            });
                            if ui.add(btn).clicked() {
                                self.process_scope = sc;
                            }
                        }

                        ui.separator();

                        if ui.button("A-").clicked() {
                            self.font_size = (self.font_size - 1.0).max(10.0);
                        }
                        ui.label(
                            egui::RichText::new(format!("{:.1}", self.font_size))
                                .monospace()
                                .size(11.5)
                                .color(egui::Color32::from_rgb(100, 116, 139)),
                        );
                        if ui.button("A+").clicked() {
                            self.font_size = (self.font_size + 1.0).min(18.0);
                        }

                        ui.separator();

                        let live_btn = egui::Button::new(
                            egui::RichText::new(if self.process_live {
                                "● LIVE (2.5s)"
                            } else {
                                "❚❚ 정지"
                            })
                            .size(12.0)
                            .strong()
                            .color(if self.process_live {
                                egui::Color32::from_rgb(5, 150, 105)
                            } else {
                                egui::Color32::from_rgb(217, 119, 6)
                            }),
                        )
                        .fill(if self.process_live {
                            egui::Color32::from_rgb(236, 253, 245)
                        } else {
                            egui::Color32::from_rgb(255, 251, 235)
                        });
                        if ui.add(live_btn).clicked() {
                            self.process_live = !self.process_live;
                            if self.process_live {
                                self.trigger_process_refresh(ctx);
                            }
                        }

                        if ui.button("새로고침").clicked() {
                            self.trigger_process_refresh(ctx);
                        }
                    } else {
                        let err_active = self.error_only;
                        if ui
                            .add(
                                egui::Button::new(
                                    egui::RichText::new("전체 로그")
                                        .size(12.0)
                                        .strong()
                                        .color(if !err_active {
                                            egui::Color32::from_rgb(15, 23, 42)
                                        } else {
                                            egui::Color32::from_rgb(100, 116, 139)
                                        }),
                                )
                                .fill(if !err_active {
                                    egui::Color32::WHITE
                                } else {
                                    egui::Color32::from_rgb(241, 245, 249)
                                }),
                            )
                            .clicked()
                            && err_active
                        {
                            self.error_only = false;
                            self.refresh_visible(true);
                        }

                        if ui
                            .add(
                                egui::Button::new(
                                    egui::RichText::new("에러만")
                                        .size(12.0)
                                        .strong()
                                        .color(if err_active {
                                            egui::Color32::WHITE
                                        } else {
                                            egui::Color32::from_rgb(225, 29, 72)
                                        }),
                                )
                                .fill(if err_active {
                                    egui::Color32::from_rgb(225, 29, 72)
                                } else {
                                    egui::Color32::from_rgb(255, 241, 242)
                                }),
                            )
                            .clicked()
                            && !err_active
                        {
                            self.error_only = true;
                            self.refresh_visible(true);
                        }

                        ui.separator();

                        let wrap_btn = egui::Button::new(
                            egui::RichText::new(if self.word_wrap {
                                "줄바꿈 ON"
                            } else {
                                "줄바꿈 OFF (가로스크롤)"
                            })
                            .size(12.0)
                            .strong()
                            .color(if !self.word_wrap {
                                egui::Color32::from_rgb(2, 132, 199)
                            } else {
                                egui::Color32::from_rgb(71, 85, 105)
                            }),
                        )
                        .fill(if !self.word_wrap {
                            egui::Color32::from_rgb(240, 249, 255)
                        } else {
                            egui::Color32::WHITE
                        });
                        if ui.add(wrap_btn).clicked() {
                            self.word_wrap = !self.word_wrap;
                        }

                        if ui.button("A-").clicked() {
                            self.font_size = (self.font_size - 1.0).max(10.0);
                        }
                        ui.label(
                            egui::RichText::new(format!("{:.1}", self.font_size))
                                .monospace()
                                .size(11.5)
                                .color(egui::Color32::from_rgb(100, 116, 139)),
                        );
                        if ui.button("A+").clicked() {
                            self.font_size = (self.font_size + 1.0).min(18.0);
                        }

                        ui.separator();

                        ui.add(
                            egui::TextEdit::singleline(&mut self.global_filter)
                                .desired_width(150.0)
                                .hint_text("전체 로그 검색...")
                                .font(egui::FontId::proportional(12.0)),
                        );
                        if !self.global_filter.is_empty() && ui.button("✕").clicked() {
                            self.global_filter.clear();
                        }

                        let live_btn = egui::Button::new(
                            egui::RichText::new(if self.is_live {
                                "● LIVE"
                            } else {
                                "❚❚ 정지"
                            })
                            .size(12.0)
                            .strong()
                            .color(if self.is_live {
                                egui::Color32::from_rgb(5, 150, 105)
                            } else {
                                egui::Color32::from_rgb(217, 119, 6)
                            }),
                        )
                        .fill(if self.is_live {
                            egui::Color32::from_rgb(236, 253, 245)
                        } else {
                            egui::Color32::from_rgb(255, 251, 235)
                        });
                        if ui.add(live_btn).clicked() {
                            self.is_live = !self.is_live;
                            if self.is_live {
                                self.refresh_visible(false);
                            }
                        }

                        if ui.button("새로고침").clicked() {
                            self.refresh_visible(true);
                        }

                        if !matches!(self.group, LogGroup::Single(_))
                            && ui.button("비율 초기화").clicked()
                        {
                            self.reset_ratios();
                        }
                    }
                });
            });

        // 2. 메인 5분할 / 3분할 / 2분할 / 단일 / 프로세스 진단 영역
        egui::CentralPanel::default()
            .frame(
                egui::Frame::NONE
                    .fill(egui::Color32::from_rgb(241, 245, 249))
                    .inner_margin(egui::Margin::same(6)),
            )
            .show(ctx, |ui| {
                let area = ui.available_rect_before_wrap();
                if area.width() < 20.0 || area.height() < 20.0 {
                    return;
                }

                if matches!(self.group, LogGroup::Process) {
                    self.render_process_monitor(ui, ctx);
                    return;
                }

                if let Some(z_idx) = self.zoomed {
                    self.render_pane_in_rect(ui, z_idx, area);
                    return;
                }

                let gap = 6.0_f32;

                match self.group {
                    LogGroup::Process => {}
                    LogGroup::Single(idx) => {
                        self.render_pane_in_rect(ui, idx, area);
                    }
                    LogGroup::Infra => {
                        let (left_rect, splitter_rect, right_rect) =
                            split_horizontal(area, self.bottom_left_ratio, gap);
                        self.render_pane_in_rect(ui, ServiceIdx::Mysql as usize, left_rect);
                        self.render_pane_in_rect(ui, ServiceIdx::Kafka as usize, right_rect);
                        handle_v_splitter(
                            ui,
                            "split_infra_x",
                            splitter_rect,
                            area,
                            &mut self.bottom_left_ratio,
                        );
                    }
                    LogGroup::Core => {
                        let (left_col, v_split, right_col) =
                            split_horizontal(area, self.top_left_ratio, gap);
                        let (ws_rect, h_split, sync_rect) =
                            split_vertical(left_col, self.ws_ratio, gap);

                        self.render_pane_in_rect(ui, ServiceIdx::Ws as usize, ws_rect);
                        self.render_pane_in_rect(ui, ServiceIdx::Sync as usize, sync_rect);
                        self.render_pane_in_rect(ui, ServiceIdx::Api as usize, right_col);

                        handle_v_splitter(
                            ui,
                            "split_core_x",
                            v_split,
                            area,
                            &mut self.top_left_ratio,
                        );
                        handle_h_splitter(
                            ui,
                            "split_ws_sync_y",
                            h_split,
                            left_col,
                            &mut self.ws_ratio,
                        );
                    }
                    LogGroup::All => {
                        // 3행 2열 격자: 상단(1:WS, 3:SYNC | 2+4:API) + 하단(5:MYSQL | 6:KAFKA)
                        let (top_area, main_h_split, bottom_area) =
                            split_vertical(area, self.top_ratio, gap);
                        let (top_left, top_v_split, api_rect) =
                            split_horizontal(top_area, self.top_left_ratio, gap);
                        let (ws_rect, ws_h_split, sync_rect) =
                            split_vertical(top_left, self.ws_ratio, gap);
                        let (mysql_rect, bot_v_split, kafka_rect) =
                            split_horizontal(bottom_area, self.bottom_left_ratio, gap);

                        self.render_pane_in_rect(ui, ServiceIdx::Ws as usize, ws_rect);
                        self.render_pane_in_rect(ui, ServiceIdx::Sync as usize, sync_rect);
                        self.render_pane_in_rect(ui, ServiceIdx::Api as usize, api_rect);
                        self.render_pane_in_rect(ui, ServiceIdx::Mysql as usize, mysql_rect);
                        self.render_pane_in_rect(ui, ServiceIdx::Kafka as usize, kafka_rect);

                        handle_h_splitter(
                            ui,
                            "split_all_main_y",
                            main_h_split,
                            area,
                            &mut self.top_ratio,
                        );
                        handle_v_splitter(
                            ui,
                            "split_all_top_x",
                            top_v_split,
                            top_area,
                            &mut self.top_left_ratio,
                        );
                        handle_h_splitter(
                            ui,
                            "split_all_ws_sync_y",
                            ws_h_split,
                            top_left,
                            &mut self.ws_ratio,
                        );
                        handle_v_splitter(
                            ui,
                            "split_all_bottom_x",
                            bot_v_split,
                            bottom_area,
                            &mut self.bottom_left_ratio,
                        );
                    }
                }
            });
    }
}

fn split_horizontal(rect: egui::Rect, ratio: f32, gap: f32) -> (egui::Rect, egui::Rect, egui::Rect) {
    let usable = (rect.width() - gap).max(10.0);
    let left_w = (usable * ratio.clamp(0.15, 0.85)).max(40.0).min(usable - 40.0);
    let left = egui::Rect::from_min_size(rect.min, egui::vec2(left_w, rect.height()));
    let splitter = egui::Rect::from_min_size(
        egui::pos2(rect.left() + left_w, rect.top()),
        egui::vec2(gap, rect.height()),
    );
    let right = egui::Rect::from_min_max(
        egui::pos2(rect.left() + left_w + gap, rect.top()),
        rect.max,
    );
    (left, splitter, right)
}

fn split_vertical(rect: egui::Rect, ratio: f32, gap: f32) -> (egui::Rect, egui::Rect, egui::Rect) {
    let usable = (rect.height() - gap).max(10.0);
    let top_h = (usable * ratio.clamp(0.15, 0.85)).max(40.0).min(usable - 40.0);
    let top = egui::Rect::from_min_size(rect.min, egui::vec2(rect.width(), top_h));
    let splitter = egui::Rect::from_min_size(
        egui::pos2(rect.left(), rect.top() + top_h),
        egui::vec2(rect.width(), gap),
    );
    let bottom = egui::Rect::from_min_max(
        egui::pos2(rect.left(), rect.top() + top_h + gap),
        rect.max,
    );
    (top, splitter, bottom)
}

fn handle_v_splitter(
    ui: &mut egui::Ui,
    id: &str,
    splitter_rect: egui::Rect,
    parent_rect: egui::Rect,
    ratio: &mut f32,
) {
    let resp = ui.interact(splitter_rect, egui::Id::new(id), egui::Sense::drag());
    if resp.hovered() || resp.dragged() {
        ui.ctx().set_cursor_icon(egui::CursorIcon::ResizeHorizontal);
    }
    if resp.dragged() {
        if let Some(pos) = resp.interact_pointer_pos() {
            let new_ratio = (pos.x - parent_rect.left()) / parent_rect.width().max(1.0);
            *ratio = new_ratio.clamp(0.18, 0.82);
        }
    }

    let color = if resp.dragged() || resp.hovered() {
        egui::Color32::from_rgb(2, 132, 199)
    } else {
        egui::Color32::from_rgb(203, 213, 225)
    };
    let x = splitter_rect.center().x;
    let half_len = if resp.dragged() || resp.hovered() {
        splitter_rect.height() * 0.48
    } else {
        18.0_f32.min(splitter_rect.height() * 0.3)
    };
    let cy = splitter_rect.center().y;
    ui.painter().line_segment(
        [egui::pos2(x, cy - half_len), egui::pos2(x, cy + half_len)],
        egui::Stroke::new(2.0_f32, color),
    );
}

fn handle_h_splitter(
    ui: &mut egui::Ui,
    id: &str,
    splitter_rect: egui::Rect,
    parent_rect: egui::Rect,
    ratio: &mut f32,
) {
    let resp = ui.interact(splitter_rect, egui::Id::new(id), egui::Sense::drag());
    if resp.hovered() || resp.dragged() {
        ui.ctx().set_cursor_icon(egui::CursorIcon::ResizeVertical);
    }
    if resp.dragged() {
        if let Some(pos) = resp.interact_pointer_pos() {
            let new_ratio = (pos.y - parent_rect.top()) / parent_rect.height().max(1.0);
            *ratio = new_ratio.clamp(0.18, 0.82);
        }
    }

    let color = if resp.dragged() || resp.hovered() {
        egui::Color32::from_rgb(2, 132, 199)
    } else {
        egui::Color32::from_rgb(203, 213, 225)
    };
    let y = splitter_rect.center().y;
    let half_len = if resp.dragged() || resp.hovered() {
        splitter_rect.width() * 0.48
    } else {
        24.0_f32.min(splitter_rect.width() * 0.3)
    };
    let cx = splitter_rect.center().x;
    ui.painter().line_segment(
        [egui::pos2(cx - half_len, y), egui::pos2(cx + half_len, y)],
        egui::Stroke::new(2.0_f32, color),
    );
}

fn find_workspace_root() -> Option<PathBuf> {
    let mut roots: Vec<PathBuf> = Vec::new();
    if let Ok(exe) = std::env::current_exe() {
        let mut cur = exe.parent().map(|p| p.to_path_buf());
        for _ in 0..6 {
            if let Some(dir) = cur {
                roots.push(dir.clone());
                cur = dir.parent().map(|p| p.to_path_buf());
            }
        }
    }
    if let Ok(cwd) = std::env::current_dir() {
        let mut cur = Some(cwd);
        for _ in 0..4 {
            if let Some(dir) = cur {
                roots.push(dir.clone());
                cur = dir.parent().map(|p| p.to_path_buf());
            }
        }
    }
    roots
        .into_iter()
        .find(|d| d.join("config").join("paths.env").exists())
}

fn load_defaults_from_paths_env() -> (PathBuf, [u16; 5]) {
    let mut log_dir = PathBuf::from(r"D:\elvis-lite\logs");
    let mut ports = [13306u16, 19092, 9080, 9081, 9082];

    if let Some(root) = find_workspace_root() {
        let env_path = root.join("config").join("paths.env");
        if let Ok(content) = fs::read_to_string(env_path) {
            for line in content.lines() {
                let trimmed = line.trim();
                if trimmed.is_empty() || trimmed.starts_with('#') {
                    continue;
                }
                if let Some((k, v)) = trimmed.split_once('=') {
                    let key = k.trim();
                    let val = v.trim();
                    match key {
                        "ELVIS_LOG_DIR" if !val.is_empty() => log_dir = PathBuf::from(val),
                        "ELVIS_MYSQL_PORT" => {
                            if let Ok(p) = val.parse() {
                                ports[0] = p;
                            }
                        }
                        "ELVIS_KAFKA_PORT" => {
                            if let Ok(p) = val.parse() {
                                ports[1] = p;
                            }
                        }
                        "ELVIS_WS_PORT" => {
                            if let Ok(p) = val.parse() {
                                ports[2] = p;
                            }
                        }
                        "ELVIS_SYNC_PORT" => {
                            if let Ok(p) = val.parse() {
                                ports[3] = p;
                            }
                        }
                        "ELVIS_API_PORT" => {
                            if let Ok(p) = val.parse() {
                                ports[4] = p;
                            }
                        }
                        _ => {}
                    }
                }
            }
        }
    }

    (log_dir, ports)
}

fn configure_light_theme_and_fonts(ctx: &egui::Context) {
    let mut visuals = egui::Visuals::light();
    visuals.panel_fill = egui::Color32::from_rgb(248, 250, 252);
    visuals.window_fill = egui::Color32::WHITE;
    visuals.widgets.noninteractive.bg_stroke =
        egui::Stroke::new(1.0_f32, egui::Color32::from_rgb(226, 232, 240));
    ctx.set_visuals(visuals);

    let mut fonts = egui::FontDefinitions::default();
    let ws_root = find_workspace_root();

    // 한글 폰트 탐색 (프로젝트 Pretendard -> Windows 맑은 고딕 폴백)
    let mut kr_candidates: Vec<PathBuf> = Vec::new();
    if let Some(ref root) = ws_root {
        kr_candidates.push(root.join("elvis-launcher/public/fonts/PretendardVariable.ttf"));
        kr_candidates.push(root.join("elvis-control-ui/public/fonts/PretendardVariable.ttf"));
    }
    kr_candidates.push(PathBuf::from(r"C:\Windows\Fonts\malgun.ttf"));
    kr_candidates.push(PathBuf::from(r"C:\Windows\Fonts\gulim.ttc"));

    if let Some(bytes) = kr_candidates.iter().find_map(|p| fs::read(p).ok()) {
        fonts
            .font_data
            .insert("elvis_kr".to_owned(), Arc::new(egui::FontData::from_owned(bytes)));
        fonts
            .families
            .entry(egui::FontFamily::Proportional)
            .or_default()
            .insert(0, "elvis_kr".to_owned());
        fonts
            .families
            .entry(egui::FontFamily::Monospace)
            .or_default()
            .push("elvis_kr".to_owned());
    }

    // 모노스페이스 영문/숫자 폰트 (Inconsolata -> Consolas)
    let mut mono_candidates: Vec<PathBuf> = Vec::new();
    if let Some(ref root) = ws_root {
        mono_candidates
            .push(root.join("elvis-launcher/public/fonts/Inconsolata-VariableFont_wdth,wght.ttf"));
        mono_candidates.push(
            root.join("elvis-control-ui/public/fonts/Inconsolata-VariableFont_wdth,wght.ttf"),
        );
    }
    mono_candidates.push(PathBuf::from(r"C:\Windows\Fonts\consola.ttf"));

    if let Some(bytes) = mono_candidates.iter().find_map(|p| fs::read(p).ok()) {
        fonts.font_data.insert(
            "elvis_mono".to_owned(),
            Arc::new(egui::FontData::from_owned(bytes)),
        );
        fonts
            .families
            .entry(egui::FontFamily::Monospace)
            .or_default()
            .insert(0, "elvis_mono".to_owned());
    }

    ctx.set_fonts(fonts);
}

fn strip_ansi_and_tabs(input: &str) -> String {
    let mut out = String::with_capacity(input.len());
    let mut chars = input.chars().peekable();
    while let Some(ch) = chars.next() {
        if ch == '\x1b' {
            if let Some(&'[') = chars.peek() {
                chars.next();
                while let Some(c) = chars.next() {
                    if c.is_ascii_alphabetic() {
                        break;
                    }
                }
                continue;
            }
        }
        match ch {
            '\t' => out.push_str("  "),
            '\r' => {}
            _ => out.push(ch),
        }
    }
    out
}

fn is_error_or_warn_line(line: &str) -> bool {
    let lower = line.to_lowercase();
    lower.contains("error")
        || lower.contains("exception")
        || lower.contains("fatal")
        || lower.contains("warn")
        || lower.contains("실패")
        || lower.contains("경고")
        || lower.contains("nullpointer")
}

fn classify_line_colors(line: &str) -> (egui::Color32, Option<egui::Color32>, bool) {
    let lower = line.to_lowercase();
    if lower.contains("error")
        || lower.contains("exception")
        || lower.contains("fatal")
        || lower.contains("실패")
    {
        (
            egui::Color32::from_rgb(190, 18, 60),        // rose-700
            Some(egui::Color32::from_rgb(255, 241, 242)), // rose-50
            true,
        )
    } else if lower.contains("warn") || lower.contains("경고") {
        (
            egui::Color32::from_rgb(146, 64, 14),        // amber-800
            Some(egui::Color32::from_rgb(255, 251, 235)), // amber-50
            false,
        )
    } else if lower.contains("[ocpp]") || lower.contains("ready for connections") {
        (egui::Color32::from_rgb(3, 105, 161), None, false) // sky-700
    } else {
        (egui::Color32::from_rgb(30, 41, 59), None, false) // slate-800
    }
}

fn service_key_to_idx(key: &str) -> Option<usize> {
    match key {
        "ws" => Some(ServiceIdx::Ws as usize),
        "sync" => Some(ServiceIdx::Sync as usize),
        "api" => Some(ServiceIdx::Api as usize),
        "mysql" => Some(ServiceIdx::Mysql as usize),
        "kafka" => Some(ServiceIdx::Kafka as usize),
        _ => None,
    }
}

fn encode_ps_utf16le_base64(script: &str) -> String {
    const TABLE: &[u8; 64] =
        b"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    let mut bytes = Vec::with_capacity(script.len() * 2);
    for u in script.encode_utf16() {
        bytes.extend_from_slice(&u.to_le_bytes());
    }
    let mut out = String::with_capacity(bytes.len().div_ceil(3) * 4);
    let mut i = 0;
    while i < bytes.len() {
        let b0 = bytes[i];
        let b1 = if i + 1 < bytes.len() { bytes[i + 1] } else { 0 };
        let b2 = if i + 2 < bytes.len() { bytes[i + 2] } else { 0 };
        out.push(TABLE[(b0 >> 2) as usize] as char);
        out.push(TABLE[(((b0 & 0x03) << 4) | (b1 >> 4)) as usize] as char);
        if i + 1 < bytes.len() {
            out.push(TABLE[(((b1 & 0x0f) << 2) | (b2 >> 6)) as usize] as char);
        } else {
            out.push('=');
        }
        if i + 2 < bytes.len() {
            out.push(TABLE[(b2 & 0x3f) as usize] as char);
        } else {
            out.push('=');
        }
        i += 3;
    }
    out
}

fn run_hidden_powershell(script: &str) -> Result<String, String> {
    let encoded = encode_ps_utf16le_base64(script);
    let mut cmd = Command::new("powershell");
    cmd.args(["-NoProfile", "-NonInteractive", "-EncodedCommand", &encoded]);
    #[cfg(target_os = "windows")]
    {
        use std::os::windows::process::CommandExt;
        const CREATE_NO_WINDOW: u32 = 0x0800_0000;
        cmd.creation_flags(CREATE_NO_WINDOW);
    }
    let output = cmd.output().map_err(|e| format!("PowerShell 실행 실패: {}", e))?;
    if !output.status.success() {
        let err_txt = String::from_utf8_lossy(&output.stderr).trim().to_string();
        if !err_txt.is_empty() {
            return Err(err_txt);
        }
    }
    Ok(String::from_utf8_lossy(&output.stdout).to_string())
}

fn collect_process_snapshot(ports: [u16; 5]) -> ProcessSnapshot {
    let [mysql_p, kafka_p, ws_p, sync_p, api_p] = ports;
    let ps_script = format!(
        r#"[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
Write-Output ("TIME`t" + (Get-Date -Format "yyyy-MM-dd HH:mm:ss"))

$targets = @(
  @("런처UI","elvis-launcher (통합 제어)","launcher","",1420),
  @("미들웨어","MySQL 9.71","infra","mysql",{mysql_p}),
  @("미들웨어","Apache Kafka KRaft","infra","kafka",{kafka_p}),
  @("코어엔진","elvis-connect-ws","core","ws",{ws_p}),
  @("코어엔진","elvis-connect-sync","core","sync",{sync_p}),
  @("코어엔진","elvis-control-api","core","api",{api_p}),
  @("관제웹UI","elvis-control-ui","ui","",1421),
  @("독립테스터","elvis-tester-ui","tester","",1422)
)

$listenByPort = @{{}}
$conns = Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue
if ($conns) {{
  foreach ($c in $conns) {{
    $lp = [int]$c.LocalPort
    if (-not $listenByPort.ContainsKey($lp)) {{
      $listenByPort[$lp] = New-Object System.Collections.Generic.HashSet[int]
    }}
    [void]$listenByPort[$lp].Add([int]$c.OwningProcess)
  }}
}}

$procById = @{{}}
Get-Process -ErrorAction SilentlyContinue | ForEach-Object {{ $procById[[int]$_.Id] = $_ }}

foreach ($t in $targets) {{
  $cat=$t[0]; $name=$t[1]; $grp=$t[2]; $skey=$t[3]; $port=[int]$t[4]
  if ($listenByPort.ContainsKey($port) -and $listenByPort[$port].Count -gt 0) {{
    foreach ($pidVal in $listenByPort[$port]) {{
      $pObj = $procById[$pidVal]
      $pName = if ($pObj) {{ $pObj.ProcessName }} else {{ "Unknown" }}
      $mem = if ($pObj) {{ [math]::Round($pObj.WorkingSet64 / 1MB, 1) }} else {{ 0 }}
      Write-Output ("PORT`t$cat`t$name`t$grp`t$skey`t$port`tRUNNING`t$pidVal`t$pName`t$mem")
    }}
  }} else {{
    Write-Output ("PORT`t$cat`t$name`t$grp`t$skey`t$port`tSTOPPED`t`t`t0")
  }}
}}

$cimProcs = Get-CimInstance Win32_Process -Filter "Name = 'mysqld.exe' OR Name = 'java.exe' OR Name = 'node.exe'" -ErrorAction SilentlyContinue
if ($cimProcs) {{
  foreach ($cp in $cimProcs) {{
    $pName = $cp.Name
    $cmd = [string]$cp.CommandLine
    $grp = $null; $cat = ""; $label = ""; $pStr = "-"; $skey = ""
    if ($pName -ieq "mysqld.exe") {{
      $grp = "infra"; $cat = "미들웨어"; $label = "MySQL 9.71 Database Engine"; $pStr = ":{mysql_p}"; $skey = "mysql"
    }} elseif ($pName -ieq "java.exe") {{
      if ($cmd -match "kafka\.Kafka") {{
        $grp = "infra"; $cat = "미들웨어"; $label = "Apache Kafka KRaft Broker"; $pStr = ":{kafka_p}"; $skey = "kafka"
      }} elseif ($cmd -match "elvis-connect-ws") {{
        $grp = "core"; $cat = "코어엔진"; $label = "elvis-connect-ws Gateway"; $pStr = ":{ws_p}"; $skey = "ws"
      }} elseif ($cmd -match "elvis-connect-sync") {{
        $grp = "core"; $cat = "코어엔진"; $label = "elvis-connect-sync Engine"; $pStr = ":{sync_p}"; $skey = "sync"
      }} elseif ($cmd -match "elvis-control-api") {{
        $grp = "core"; $cat = "코어엔진"; $label = "elvis-control-api Server"; $pStr = ":{api_p}"; $skey = "api"
      }} elseif ($cmd -match "GradleDaemon") {{
        $grp = "build"; $cat = "빌드도구"; $label = "Gradle Build Daemon"; $pStr = "-"; $skey = ""
      }}
    }} elseif ($pName -ieq "node.exe") {{
      if ($cmd -match "elvis-launcher") {{
        $grp = "launcher"; $cat = "런처UI"; $label = "elvis-launcher (Dev Host)"; $pStr = ":1420"
      }} elseif ($cmd -match "elvis-control-ui") {{
        $grp = "ui"; $cat = "관제웹UI"; $label = "elvis-control-ui (Admin Web)"; $pStr = ":1421"
      }} elseif ($cmd -match "elvis-tester-ui") {{
        $grp = "tester"; $cat = "독립테스터"; $label = "elvis-tester-ui (Test Web)"; $pStr = ":1422"
      }}
    }}
    if ($grp) {{
      $pidVal = [int]$cp.ProcessId
      $pObj = $procById[$pidVal]
      $mem = if ($pObj) {{ [math]::Round($pObj.WorkingSet64 / 1MB, 1) }} else {{ 0 }}
      Write-Output ("PROC`t$grp`t$cat`t$label`t$pidVal`t$pName`t$pStr`t$mem`t$skey")
    }}
  }}
}}
"#
    );

    let mut snap = ProcessSnapshot {
        loaded: true,
        ..Default::default()
    };

    match run_hidden_powershell(&ps_script) {
        Ok(stdout) => {
            for raw in stdout.lines() {
                let line = raw.trim_end_matches('\r');
                if line.is_empty() {
                    continue;
                }
                let cols: Vec<&str> = line.split('\t').collect();
                match cols.first().copied() {
                    Some("TIME") if cols.len() >= 2 => {
                        snap.checked_at = cols[1].to_string();
                    }
                    Some("PORT") if cols.len() >= 10 => {
                        snap.ports.push(PortRow {
                            category: cols[1].to_string(),
                            name: cols[2].to_string(),
                            group: cols[3].to_string(),
                            service_key: cols[4].to_string(),
                            port: cols[5].parse().unwrap_or(0),
                            running: cols[6] == "RUNNING",
                            pid: cols[7].parse().ok(),
                            proc_name: cols[8].to_string(),
                            mem_mb: cols[9].parse().unwrap_or(0.0),
                        });
                    }
                    Some("PROC") if cols.len() >= 9 => {
                        snap.procs.push(ProcRow {
                            group: cols[1].to_string(),
                            category: cols[2].to_string(),
                            name: cols[3].to_string(),
                            pid: cols[4].parse().unwrap_or(0),
                            proc_name: cols[5].to_string(),
                            port_str: cols[6].to_string(),
                            mem_mb: cols[7].parse().unwrap_or(0.0),
                            service_key: cols[8].to_string(),
                        });
                    }
                    _ => {}
                }
            }
        }
        Err(e) => {
            snap.error = Some(e);
        }
    }

    snap
}

fn kill_process_by_pid(pid: u32) -> Result<(), String> {
    let mut cmd = Command::new("taskkill");
    cmd.args(["/F", "/PID", &pid.to_string()]);
    #[cfg(target_os = "windows")]
    {
        use std::os::windows::process::CommandExt;
        const CREATE_NO_WINDOW: u32 = 0x0800_0000;
        cmd.creation_flags(CREATE_NO_WINDOW);
    }
    let output = cmd.output().map_err(|e| format!("taskkill 실행 실패: {}", e))?;
    if output.status.success() {
        Ok(())
    } else {
        let stderr = String::from_utf8_lossy(&output.stderr).trim().to_string();
        let stdout = String::from_utf8_lossy(&output.stdout).trim().to_string();
        Err(if !stderr.is_empty() { stderr } else { stdout })
    }
}

fn kill_process_by_port(port: u16) -> Result<String, String> {
    let ps_script = format!(
        r#"[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$conns = Get-NetTCPConnection -LocalPort {port} -State Listen -ErrorAction SilentlyContinue
if (-not $conns) {{
  Write-Output "NONE"
  exit 0
}}
$pids = $conns | Select-Object -ExpandProperty OwningProcess -Unique
foreach ($p in $pids) {{
  Stop-Process -Id $p -Force -ErrorAction SilentlyContinue
  Write-Output ("KILLED`t" + $p)
}}
"#
    );
    let out = run_hidden_powershell(&ps_script)?;
    let mut killed: Vec<String> = Vec::new();
    for line in out.lines() {
        let trimmed = line.trim();
        if trimmed == "NONE" {
            return Err(format!("Port :{} 를 점유 중인 프로세스가 없습니다.", port));
        }
        if let Some(pid_str) = trimmed.strip_prefix("KILLED\t") {
            killed.push(pid_str.to_string());
        }
    }
    if killed.is_empty() {
        Err(format!("Port :{} 를 점유 중인 프로세스가 없습니다.", port))
    } else {
        Ok(format!(
            "Port :{} 점유 프로세스 (PID {}) 강제 종료를 완료했습니다.",
            port,
            killed.join(", ")
        ))
    }
}

fn parse_args() -> (PathBuf, LogGroup, ProcessScope, bool, bool, [u16; 5]) {
    let (mut log_dir, mut ports) = load_defaults_from_paths_env();
    let mut group = LogGroup::All;
    let mut process_scope = ProcessScope::All;
    let mut error_only = false;
    let mut word_wrap = false;

    let args: Vec<String> = std::env::args().collect();
    let mut i = 1;
    while i < args.len() {
        match args[i].as_str() {
            "--log-dir" if i + 1 < args.len() => {
                log_dir = PathBuf::from(&args[i + 1]);
                i += 2;
            }
            "--mode" if i + 1 < args.len() => {
                if args[i + 1].eq_ignore_ascii_case("process")
                    || args[i + 1].eq_ignore_ascii_case("proc")
                {
                    group = LogGroup::Process;
                }
                i += 2;
            }
            "--target" if i + 1 < args.len() => {
                process_scope = ProcessScope::from_str(&args[i + 1]);
                i += 2;
            }
            "--group" | "--service" if i + 1 < args.len() => {
                group = LogGroup::from_str(&args[i + 1]);
                i += 2;
            }
            "--type" if i + 1 < args.len() => {
                error_only = args[i + 1].eq_ignore_ascii_case("error");
                i += 2;
            }
            "--wrap" if i + 1 < args.len() => {
                word_wrap = args[i + 1] == "1" || args[i + 1].eq_ignore_ascii_case("true");
                i += 2;
            }
            "--ports" if i + 1 < args.len() => {
                let parts: Vec<u16> = args[i + 1]
                    .split(',')
                    .filter_map(|p| p.trim().parse::<u16>().ok())
                    .collect();
                if parts.len() == 5 {
                    ports = [parts[0], parts[1], parts[2], parts[3], parts[4]];
                }
                i += 2;
            }
            _ => {
                i += 1;
            }
        }
    }

    (log_dir, group, process_scope, error_only, word_wrap, ports)
}

fn main() -> eframe::Result {
    let (log_dir, group, process_scope, error_only, word_wrap, ports) = parse_args();

    let mode_label = if error_only { "에러 로그" } else { "실시간 로그" };
    let (window_title, init_size) = match group {
        LogGroup::Process => (
            format!("ELVIS 프로세스 및 포트 진단 - {}", process_scope.label()),
            [1120.0, 720.0],
        ),
        LogGroup::Core => (format!("ELVIS 코어 엔진 (3분할) - {}", mode_label), [1440.0, 860.0]),
        LogGroup::Infra => (format!("ELVIS 미들웨어 (2분할) - {}", mode_label), [1360.0, 760.0]),
        LogGroup::Single(idx) => {
            let label = match idx {
                0 => format!("elvis-connect-ws (:{})", ports[2]),
                1 => format!("elvis-connect-sync (:{})", ports[3]),
                2 => format!("elvis-control-api (:{})", ports[4]),
                3 => format!("MySQL 9.71 (:{})", ports[0]),
                4 => format!("Apache Kafka (:{})", ports[1]),
                _ => "단일 프로세스".to_string(),
            };
            (format!("ELVIS {} - {}", label, mode_label), [1080.0, 700.0])
        }
        LogGroup::All => (
            format!("ELVIS 통합 로그 뷰어 (5분할) - {}", mode_label),
            [1440.0, 860.0],
        ),
    };

    let options = eframe::NativeOptions {
        viewport: egui::ViewportBuilder::default()
            .with_title(window_title)
            .with_inner_size(init_size)
            .with_min_inner_size([640.0, 400.0]),
        ..Default::default()
    };

    eframe::run_native(
        "ELVIS-Log-Viewer",
        options,
        Box::new(move |cc| {
            Ok(Box::new(LogGuiApp::new(
                cc,
                log_dir,
                group,
                process_scope,
                error_only,
                word_wrap,
                ports,
            )))
        }),
    )
}
