use std::env;
use std::fs;
use std::path::{Path, PathBuf};
use std::process::Command;

fn main() {
    println!("cargo:rerun-if-changed=icons/icon.ico");
    println!("cargo:rerun-if-changed=icons/icon_64.rgba");

    if env::var("CARGO_CFG_TARGET_OS").as_deref() != Ok("windows") {
        return;
    }

    let manifest_dir = PathBuf::from(env::var("CARGO_MANIFEST_DIR").unwrap());
    let icon_path = manifest_dir.join("icons").join("icon.ico");
    if !icon_path.exists() {
        println!("cargo:warning=icons/icon.ico not found; skipping Windows icon embedding");
        return;
    }

    let out_dir = PathBuf::from(env::var("OUT_DIR").unwrap());
    let rc_path = out_dir.join("elvis_log_viewer.rc");
    let res_path = out_dir.join("elvis_log_viewer.res");

    let icon_escaped = icon_path.to_string_lossy().replace('\\', "\\\\");
    let rc_content = format!("1 ICON \"{}\"\r\n", icon_escaped);
    if let Err(e) = fs::write(&rc_path, rc_content) {
        println!("cargo:warning=failed to write .rc file: {}", e);
        return;
    }

    let Some(rc_exe) = find_rc_exe() else {
        println!("cargo:warning=rc.exe not found; skipping Windows icon embedding");
        return;
    };

    let status = Command::new(&rc_exe)
        .args(["/nologo", "/fo"])
        .arg(&res_path)
        .arg(&rc_path)
        .status();

    match status {
        Ok(s) if s.success() && res_path.exists() => {
            println!("cargo:rustc-link-arg-bins={}", res_path.display());
        }
        Ok(s) => {
            println!("cargo:warning=rc.exe exited with status {}", s);
        }
        Err(e) => {
            println!("cargo:warning=failed to execute rc.exe: {}", e);
        }
    }
}

fn find_rc_exe() -> Option<PathBuf> {
    if let Ok(status) = Command::new("rc.exe").arg("/?").output() {
        if !status.stdout.is_empty() || !status.stderr.is_empty() {
            return Some(PathBuf::from("rc.exe"));
        }
    }

    let kits_bin = Path::new(r"C:\Program Files (x86)\Windows Kits\10\bin");
    if let Ok(entries) = fs::read_dir(kits_bin) {
        let mut versions: Vec<PathBuf> = entries
            .filter_map(|e| e.ok().map(|d| d.path()))
            .filter(|p| p.is_dir())
            .collect();
        versions.sort();
        versions.reverse();
        for ver_dir in versions {
            let candidate = ver_dir.join("x64").join("rc.exe");
            if candidate.exists() {
                return Some(candidate);
            }
        }
    }
    None
}
