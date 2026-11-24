#!/usr/bin/env node
/**
 * ELVIS 데스크톱 실행파일(.exe) D:\ 배포 복사 유틸리티
 * - 중간 빌드 산출물(target/, dist/)은 모듈 내부에 유지하고 최종 실행파일만 D:\에 복사합니다.
 * - 사용법: node scripts/copy-exe.mjs <elvis-launcher|elvis-control-ui|elvis-log-viewer>
 */
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const rootDir = path.resolve(__dirname, '..')

const OUT_DIR = process.env.ELVIS_EXE_OUT_DIR || 'D:\\'

const TARGETS = {
  'elvis-launcher': {
    src: path.join(rootDir, 'elvis-launcher', 'src-tauri', 'target', 'release', 'elvis-launcher.exe'),
    dest: path.join(OUT_DIR, 'elvis-launcher.exe')
  },
  'elvis-control-ui': {
    src: path.join(rootDir, 'elvis-control-ui', 'src-tauri', 'target', 'release', 'elvis-control-ui.exe'),
    dest: path.join(OUT_DIR, 'elvis-control-ui.exe')
  },
  'elvis-log-viewer': {
    src: path.join(rootDir, 'elvis-log-viewer', 'target', 'release', 'elvis-log-viewer.exe'),
    dest: path.join(OUT_DIR, 'elvis-log-viewer.exe')
  }
}

const name = process.argv[2]
if (!name || !(name in TARGETS)) {
  console.error('[copy-exe] 지원 모듈: elvis-launcher | elvis-control-ui | elvis-log-viewer')
  process.exit(1)
}

const { src, dest } = TARGETS[name]
if (!fs.existsSync(src)) {
  console.error(`[copy-exe] 빌드 산출물을 찾을 수 없습니다: ${src}`)
  process.exit(1)
}

try {
  fs.mkdirSync(path.dirname(dest), { recursive: true })
  fs.copyFileSync(src, dest)
  const sizeMb = (fs.statSync(dest).size / (1024 * 1024)).toFixed(2)
  console.log(`[copy-exe] 배포 완료: ${dest} (${sizeMb} MB)`)
} catch (err) {
  console.error(`[copy-exe] 복사 실패 (${dest}): ${err?.message || err}`)
  console.error('[copy-exe] 대상 실행파일이 현재 실행 중이면 종료 후 다시 시도해 주세요.')
  process.exit(1)
}
