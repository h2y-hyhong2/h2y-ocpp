import { defineConfig, Plugin } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import fs from 'fs'
import { exec, execFile, spawn } from 'child_process'
import { promisify } from 'util'
import net from 'net'

const execAsync = promisify(exec)
const execFileAsync = promisify(execFile)

const ALLOWED_SERVICES = new Set(['mysql', 'kafka', 'ws', 'sync', 'api', 'tester', 'all-infra', 'all-services', 'all'])
const ALLOWED_LOG_SERVICES = new Set(['mysql', 'kafka', 'ws', 'sync', 'api', 'tester'])
const ALLOWED_MONITOR_TARGETS = new Set(['all', 'infra', 'core', 'extension'])

function escapePsSingleQuoted(value: string): string {
  return value.replace(/'/g, "''")
}

function sanitizeCmdTitle(value: string): string {
  return value.replace(/["&|<>^%\r\n]/g, ' ').trim()
}

function encodePsBase64(script: string): string {
  return Buffer.from(script, 'utf16le').toString('base64')
}

// 포트 점유 여부 확인 헬퍼 함수 (IPv4 & IPv6 동시 점검)
function checkPort(port: number): Promise<boolean> {
  return new Promise((resolve) => {
    let resolved = false
    let remaining = 2
    let safetyTimer: ReturnType<typeof setTimeout> | null = null

    const finish = (result: boolean) => {
      if (resolved) return
      resolved = true
      if (safetyTimer) {
        clearTimeout(safetyTimer)
        safetyTimer = null
      }
      resolve(result)
    }

    const checkHost = (host: string) => {
      const socket = new net.Socket()
      let hostSettled = false

      const markHostFailed = () => {
        if (hostSettled) return
        hostSettled = true
        socket.destroy()
        remaining -= 1
        if (remaining <= 0) {
          finish(false)
        }
      }

      socket.setTimeout(500)
      socket.once('connect', () => {
        hostSettled = true
        socket.destroy()
        finish(true)
      })
      socket.once('timeout', markHostFailed)
      socket.once('error', markHostFailed)
      socket.connect(port, host)
    }

    safetyTimer = setTimeout(() => {
      finish(false)
    }, 600)

    checkHost('127.0.0.1')
    checkHost('::1')
  })
}

// 로컬 미들웨어 제어 및 실시간 콘솔 로깅 플러그인
function infraLauncherPlugin(): Plugin {
  return {
    name: 'infra-launcher-plugin',
    configureServer(server) {
      server.middlewares.use(async (req, res, next) => {
        if (!req.url?.startsWith('/api/infra')) {
          return next()
        }

        const url = new URL(req.url, 'http://127.0.0.1')
        const pathname = url.pathname

        res.setHeader('Content-Type', 'application/json; charset=utf-8')

        const rootDir = path.resolve(__dirname, '..')
        const scriptsDir = path.resolve(__dirname, '../scripts')
        const configDir = path.resolve(__dirname, '../config')
        const envPath = path.resolve(configDir, 'paths.env')

        // paths.env 파서
        const readEnv = () => {
          const env = {
            dataDir: 'D:\\elvis-lite\\data',
            logDir: 'D:\\elvis-lite\\logs',
            mysqlPort: 13306,
            kafkaPort: 19092,
            apiPort: 9082,
            wsPort: 9080,
            syncPort: 9081,
            testerPort: 9085
          }
          if (fs.existsSync(envPath)) {
            const lines = fs.readFileSync(envPath, 'utf-8').split(/\r?\n/)
            for (const line of lines) {
              const trimmed = line.trim()
              if (!trimmed || trimmed.startsWith('#')) continue
              const [key, val] = trimmed.split('=')
              if (key === 'ELVIS_DATA_DIR' && val) env.dataDir = val.trim()
              if (key === 'ELVIS_LOG_DIR' && val) env.logDir = val.trim()
              if (key === 'ELVIS_MYSQL_PORT' && val) env.mysqlPort = parseInt(val.trim(), 10) || env.mysqlPort
              if (key === 'ELVIS_KAFKA_PORT' && val) env.kafkaPort = parseInt(val.trim(), 10) || env.kafkaPort
              if (key === 'ELVIS_API_PORT' && val) env.apiPort = parseInt(val.trim(), 10) || env.apiPort
              if (key === 'ELVIS_WS_PORT' && val) env.wsPort = parseInt(val.trim(), 10) || env.wsPort
              if (key === 'ELVIS_SYNC_PORT' && val) env.syncPort = parseInt(val.trim(), 10) || env.syncPort
              if (key === 'ELVIS_TESTER_PORT' && val) env.testerPort = parseInt(val.trim(), 10) || env.testerPort
            }
          }
          return env
        }

        // 서비스 기동 헬퍼 함수
        const runServiceProcess = (serviceName: string, batFileName: string, winTitle: string, isBackground = true) => {
          const batPath = path.resolve(scriptsDir, batFileName)
          const currentEnv = readEnv()
          const logDir = currentEnv.logDir || 'D:\\elvis-lite\\logs'
          if (!fs.existsSync(logDir)) {
            try { fs.mkdirSync(logDir, { recursive: true }) } catch (_) {}
          }

          if (isBackground) {
            console.log(`[ELVIS-Launcher] WMI 기반 완전 독립 백그라운드 데몬 기동 -> service: ${serviceName}, bat: ${batFileName}`)
            // run-daemon.ps1 (WMI Win32_Process.Create)를 호출하여 부모 콘솔 및 런처 프로세스 트리와 100% 분리된 독립 OS 데몬으로 기동
            const daemonScript = path.resolve(scriptsDir, 'run-daemon.ps1')
            const safeDaemon = escapePsSingleQuoted(daemonScript)
            const safeBat = escapePsSingleQuoted(batFileName)
            const safeSvc = escapePsSingleQuoted(serviceName)
            execFile('powershell', ['-NoProfile', '-WindowStyle', 'Hidden', '-ExecutionPolicy', 'Bypass', '-File', safeDaemon, '-BatName', safeBat, '-ServiceName', safeSvc], {
              cwd: scriptsDir,
              windowsHide: true,
              env: { ...process.env, NO_PAUSE: '1' }
            })
          } else {
            const safeTitle = sanitizeCmdTitle(winTitle)
            const safeBatPath = batPath.replace(/"/g, '')
            console.log(`[ELVIS-Launcher] 콘솔 창 기동 -> service: ${serviceName}, bat: ${safeBatPath}`)
            exec(`cmd /c start "${safeTitle}" cmd /c "call "${safeBatPath}""`, {
              cwd: scriptsDir,
              env: { ...process.env, NO_PAUSE: '1' }
            })
          }
        }

        // 1. 미들웨어 및 전체 서비스 헬스체크 (GET /api/infra/status)
        if (pathname === '/api/infra/status') {
          const currentEnv = readEnv()
          const [isKafka, isMysql, isControlApi, isConnectWs, isConnectSync] = await Promise.all([
            checkPort(currentEnv.kafkaPort),
            checkPort(currentEnv.mysqlPort),
            checkPort(currentEnv.apiPort),
            checkPort(currentEnv.wsPort),
            checkPort(currentEnv.syncPort)
          ])

          res.end(JSON.stringify({
            kafka: isKafka,
            mysql: isMysql,
            controlApi: isControlApi,
            connectWs: isConnectWs,
            connectSync: isConnectSync,
            ports: {
              mysql: currentEnv.mysqlPort,
              kafka: currentEnv.kafkaPort,
              api: currentEnv.apiPort,
              ws: currentEnv.wsPort,
              sync: currentEnv.syncPort
            }
          }))
          return
        }

        // 2. 단일 포트 점유 여부 실시간 검사 (GET /api/infra/check-port?port=13306)
        if (pathname === '/api/infra/check-port') {
          const portStr = url.searchParams.get('port')
          const port = parseInt(portStr || '', 10)
          if (!port || isNaN(port)) {
            res.statusCode = 400
            res.end(JSON.stringify({ success: false, message: '유효한 포트 번호를 입력하세요.' }))
            return
          }
          const isOccupied = await checkPort(port)
          res.end(JSON.stringify({ success: true, port, isOccupied }))
          return
        }

        // 3. 포트 설정 조회 (GET /api/infra/ports)
        if (pathname === '/api/infra/ports' && req.method === 'GET') {
          const currentEnv = readEnv()
          res.end(JSON.stringify({
            success: true,
            ports: {
              mysql: currentEnv.mysqlPort,
              kafka: currentEnv.kafkaPort,
              api: currentEnv.apiPort,
              ws: currentEnv.wsPort
            }
          }))
          return
        }

        // 4. 포트 설정 저장 및 동기화 (POST /api/infra/ports)
        if (pathname === '/api/infra/ports' && req.method === 'POST') {
          let body = ''
          req.on('data', chunk => { body += chunk })
          req.on('end', () => {
            try {
              const parsed = JSON.parse(body)
              const mysqlPort = parseInt(parsed.mysql, 10) || 13306
              const kafkaPort = parseInt(parsed.kafka, 10) || 19092
              const apiPort = parseInt(parsed.api, 10) || 9082
              const wsPort = parseInt(parsed.ws, 10) || 9080

              const portList = [mysqlPort, kafkaPort, apiPort, wsPort]
              for (const p of portList) {
                if (p < 1024 || p > 65535) {
                  res.statusCode = 400
                  res.end(JSON.stringify({ success: false, message: `포트 범위는 1024~65535 사이여야 합니다 (입력값: ${p})` }))
                  return
                }
              }

              const uniquePorts = new Set(portList)
              if (uniquePorts.size !== portList.length) {
                res.statusCode = 400
                res.end(JSON.stringify({ success: false, message: '각 서비스의 포트는 서로 중복될 수 없습니다.' }))
                return
              }

              const currentEnv = readEnv()

              // paths.env 저장 (기존 키 ELVIS_SYNC_PORT, ELVIS_TESTER_PORT 및 커스텀 설정 보존)
              const updates: Record<string, string> = {
                ELVIS_DATA_DIR: currentEnv.dataDir,
                ELVIS_LOG_DIR: currentEnv.logDir,
                ELVIS_MYSQL_PORT: String(mysqlPort),
                ELVIS_KAFKA_PORT: String(kafkaPort),
                ELVIS_API_PORT: String(apiPort),
                ELVIS_WS_PORT: String(wsPort)
              }
              if (parsed.sync !== undefined) {
                const syncPort = parseInt(parsed.sync, 10)
                if (syncPort >= 1024 && syncPort <= 65535) updates.ELVIS_SYNC_PORT = String(syncPort)
              }
              if (parsed.tester !== undefined) {
                const testerPort = parseInt(parsed.tester, 10)
                if (testerPort >= 1024 && testerPort <= 65535) updates.ELVIS_TESTER_PORT = String(testerPort)
              }

              const seenKeys = new Set<string>()
              let lines: string[] = []
              if (fs.existsSync(envPath)) {
                lines = fs.readFileSync(envPath, 'utf-8').split(/\r?\n/).map(line => {
                  const eqIdx = line.indexOf('=')
                  const trimmed = line.trim()
                  if (!trimmed || trimmed.startsWith('#') || eqIdx <= 0) return line
                  const key = line.slice(0, eqIdx).trim()
                  if (Object.prototype.hasOwnProperty.call(updates, key)) {
                    seenKeys.add(key)
                    return `${key}=${updates[key]}`
                  }
                  return line
                })
                while (lines.length > 0 && lines[lines.length - 1] === '') {
                  lines.pop()
                }
              } else {
                lines = [
                  '# ========================================================',
                  '# ELVIS-CSMS 스토리지 및 네트워크 포트 설정 파일 (관리자 자동 반영)',
                  '# ========================================================'
                ]
              }
              for (const [k, v] of Object.entries(updates)) {
                if (!seenKeys.has(k)) {
                  lines.push(`${k}=${v}`)
                }
              }
              lines.push('')
              fs.writeFileSync(envPath, lines.join('\r\n'), 'utf-8')

              // config/mysql/my.ini 동기화
              const myIniPath = path.resolve(configDir, 'mysql/my.ini')
              if (fs.existsSync(myIniPath)) {
                let ini = fs.readFileSync(myIniPath, 'utf-8')
                ini = ini.replace(/port\s*=\s*\d+/g, `port=${mysqlPort}`)
                fs.writeFileSync(myIniPath, ini, 'utf-8')
              }

              // config/kafka/server.properties 동기화
              const kafkaConfPath = path.resolve(configDir, 'kafka/server.properties')
              if (fs.existsSync(kafkaConfPath)) {
                let conf = fs.readFileSync(kafkaConfPath, 'utf-8')
                conf = conf.replace(/listeners=PLAINTEXT:\/\/:(\d+)/g, `listeners=PLAINTEXT://:${kafkaPort}`)
                conf = conf.replace(/advertised\.listeners=PLAINTEXT:\/\/(localhost|127\.0\.0\.1):(\d+)/g, `advertised.listeners=PLAINTEXT://$1:${kafkaPort}`)
                fs.writeFileSync(kafkaConfPath, conf, 'utf-8')
              }

              res.end(JSON.stringify({
                success: true,
                message: '포트 설정이 paths.env, my.ini, server.properties에 성공적으로 반영되었습니다.',
                ports: { mysql: mysqlPort, kafka: kafkaPort, api: apiPort, ws: wsPort }
              }))
            } catch (err: any) {
              res.statusCode = 500
              res.end(JSON.stringify({ success: false, message: '포트 저장 실패: ' + err.message }))
            }
          })
          return
        }

        // 5. 스토리지 경로 조회 (GET /api/infra/paths)
        if (pathname === '/api/infra/paths' && req.method === 'GET') {
          const currentEnv = readEnv()
          res.end(JSON.stringify({
            dataDir: currentEnv.dataDir,
            logDir: currentEnv.logDir,
            dataExists: fs.existsSync(currentEnv.dataDir),
            logExists: fs.existsSync(currentEnv.logDir)
          }))
          return
        }

        // 6. 서비스 기동 (POST /api/infra/start?service=mysql|kafka|ws|sync|api|tester|all-infra|all-services|all|topics&background=true|false)
        if (pathname === '/api/infra/start') {
          const service = url.searchParams.get('service')
          const isBackground = url.searchParams.get('background') !== 'false'

          if (service === 'kafka') {
            runServiceProcess('kafka', 'start-kafka-kraft.bat', 'ELVIS - Apache Kafka KRaft', isBackground)
          } else if (service === 'mysql') {
            runServiceProcess('mysql', 'start-mysql.bat', 'ELVIS - MySQL 9.71', isBackground)
          } else if (service === 'ws') {
            runServiceProcess('ws', 'start-elvis-connect-ws.bat', 'ELVIS - Connect WS Gateway', isBackground)
          } else if (service === 'sync') {
            runServiceProcess('sync', 'start-elvis-connect-sync.bat', 'ELVIS - Connect Sync Engine', isBackground)
          } else if (service === 'api') {
            runServiceProcess('api', 'start-elvis-control-api.bat', 'ELVIS - Control API Server', isBackground)
          } else if (service === 'tester') {
            runServiceProcess('tester', 'start-all-tester.bat', 'ELVIS - Tester Harness', isBackground)
          } else if (service === 'all-infra') {
            // 인프라 전체 기동: Kafka -> MySQL
            if (isBackground) {
              runServiceProcess('kafka', 'start-kafka-kraft.bat', 'ELVIS - Apache Kafka KRaft', true)
              setTimeout(() => {
                runServiceProcess('mysql', 'start-mysql.bat', 'ELVIS - MySQL 9.71', true)
              }, 2500)
            } else {
              runServiceProcess('all', 'start-all-middleware.bat', 'ELVIS - Middleware', false)
            }
          } else if (service === 'all-services') {
            // 코어 서비스 전체 기동: WS -> Sync -> Control API
            runServiceProcess('ws', 'start-elvis-connect-ws.bat', 'ELVIS - Connect WS Gateway', isBackground)
            setTimeout(() => {
              runServiceProcess('sync', 'start-elvis-connect-sync.bat', 'ELVIS - Connect Sync Engine', isBackground)
              setTimeout(() => {
                runServiceProcess('api', 'start-elvis-control-api.bat', 'ELVIS - Control API Server', isBackground)
              }, 2500)
            }, 2500)
          } else if (service === 'all') {
            // 전체 기동: 인프라 기동 후 코어 백엔드 순차 부팅
            if (isBackground) {
              runServiceProcess('kafka', 'start-kafka-kraft.bat', 'ELVIS - Apache Kafka KRaft', true)
              setTimeout(() => {
                runServiceProcess('mysql', 'start-mysql.bat', 'ELVIS - MySQL 9.71', true)
                setTimeout(() => {
                  runServiceProcess('ws', 'start-elvis-connect-ws.bat', 'ELVIS - Connect WS Gateway', true)
                  runServiceProcess('sync', 'start-elvis-connect-sync.bat', 'ELVIS - Connect Sync Engine', true)
                  setTimeout(() => {
                    runServiceProcess('api', 'start-elvis-control-api.bat', 'ELVIS - Control API Server', true)
                  }, 2500)
                }, 4000)
              }, 2500)
            } else {
              runServiceProcess('all', 'start-all-middleware.bat', 'ELVIS - Middleware', false)
              setTimeout(() => {
                runServiceProcess('ws', 'start-elvis-connect-ws.bat', 'ELVIS - Connect WS Gateway', false)
                runServiceProcess('sync', 'start-elvis-connect-sync.bat', 'ELVIS - Connect Sync Engine', false)
                setTimeout(() => {
                  runServiceProcess('api', 'start-elvis-control-api.bat', 'ELVIS - Control API Server', false)
                }, 2500)
              }, 3000)
            }
          } else if (service === 'topics') {
            runServiceProcess('topics', 'create-kafka-topics.bat', 'ELVIS - Topics', isBackground)
          } else {
            console.warn(`[ELVIS-Launcher] 알 수 없는 서비스 요청: ${service}`)
          }

          const modeText = isBackground ? '백그라운드(무창)' : '콘솔 윈도우'
          res.end(JSON.stringify({
            success: true,
            isBackground,
            message: `${service} ${modeText} 기동 명령이 전송되었습니다.`
          }))
          return
        }

        // 7. 서비스 중지 (POST /api/infra/stop?service=mysql|kafka|ws|sync|api|tester|all-infra|all-services|all)
        const buildStopPsScript = (svc: string, currentEnv: ReturnType<typeof readEnv>): string | null => {
          const stopByPort = (ports: number[]) =>
            `Get-NetTCPConnection -LocalPort ${ports.join(', ')} -State Listen -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }`
          const killKafkaCim = `Get-CimInstance Win32_Process -ErrorAction SilentlyContinue | Where-Object { $_.CommandLine -match 'kafka\\.Kafka' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }`
          const killMysqlProc = `Stop-Process -Name 'mysqld' -Force -ErrorAction SilentlyContinue`
          const killTitle = (prefix: string) =>
            `Get-Process -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowTitle -like '${escapePsSingleQuoted(prefix)}' } | Stop-Process -Force -ErrorAction SilentlyContinue`

          switch (svc) {
            case 'kafka':
              return [stopByPort([currentEnv.kafkaPort]), killTitle('ELVIS - Apache Kafka*'), killKafkaCim].join('; ')
            case 'mysql':
              return [killMysqlProc, stopByPort([currentEnv.mysqlPort]), killTitle('ELVIS - MySQL*')].join('; ')
            case 'ws':
              return [stopByPort([currentEnv.wsPort]), killTitle('ELVIS - Connect WS*')].join('; ')
            case 'sync':
              return [stopByPort([currentEnv.syncPort]), killTitle('ELVIS - Connect Sync*')].join('; ')
            case 'api':
              return [stopByPort([currentEnv.apiPort]), killTitle('ELVIS - Control API*')].join('; ')
            case 'tester':
              return [stopByPort([currentEnv.testerPort, 1422]), killTitle('ELVIS - Tester Harness*')].join('; ')
            case 'all-infra':
              return [killMysqlProc, stopByPort([currentEnv.kafkaPort, currentEnv.mysqlPort]), killTitle('ELVIS - Apache Kafka*'), killTitle('ELVIS - MySQL*'), killKafkaCim].join('; ')
            case 'all-services':
              return [stopByPort([currentEnv.wsPort, currentEnv.syncPort, currentEnv.apiPort]), killTitle('ELVIS - Connect*'), killTitle('ELVIS - Control*')].join('; ')
            case 'all':
              return [
                killMysqlProc,
                stopByPort([currentEnv.kafkaPort, currentEnv.mysqlPort, currentEnv.wsPort, currentEnv.syncPort, currentEnv.apiPort]),
                killTitle('ELVIS - Connect*'),
                killTitle('ELVIS - Control*'),
                killTitle('ELVIS - Apache*'),
                killTitle('ELVIS - MySQL*'),
                killKafkaCim
              ].join('; ')
            default:
              return null
          }
        }

        const waitForPortsClosed = async (targetPorts: number[], timeoutMs = 10000) => {
          const startedAt = Date.now()
          while (Date.now() - startedAt < timeoutMs) {
            const statuses = await Promise.all(targetPorts.map((p) => checkPort(p)))
            if (statuses.every((up) => !up)) {
              return
            }
            await new Promise((r) => setTimeout(r, 350))
          }
        }

        const stopAndWaitClosed = async (svc: string, targetPorts: number[], currentEnv: ReturnType<typeof readEnv>) => {
          const psScript = buildStopPsScript(svc, currentEnv)
          if (psScript) {
            await execFileAsync('powershell', ['-NoProfile', '-WindowStyle', 'Hidden', '-EncodedCommand', encodePsBase64(psScript)]).catch(() => {})
          }
          await waitForPortsClosed(targetPorts)
        }

        if (pathname === '/api/infra/stop') {
          const currentEnv = readEnv()
          const service = url.searchParams.get('service') || ''
          if (!ALLOWED_SERVICES.has(service)) {
            res.statusCode = 400
            res.end(JSON.stringify({ success: false, message: `허용되지 않은 서비스 파라미터입니다: ${service}` }))
            return
          }
          const psScript = buildStopPsScript(service, currentEnv)
          if (psScript) {
            await execFileAsync('powershell', ['-NoProfile', '-WindowStyle', 'Hidden', '-EncodedCommand', encodePsBase64(psScript)]).catch(() => {})
          }
          res.end(JSON.stringify({ success: true, message: `${service} 중지 명령이 완료되었습니다.` }))
          return
        }

        // 8. 백그라운드 전환 재기동 (POST /api/infra/restart-background)
        if (pathname === '/api/infra/restart-background') {
          const currentEnv = readEnv()
          console.log(`[ELVIS-Launcher] 기존 프로세스 정리 후 백그라운드 무창 전환 재기동 요청`)

          await stopAndWaitClosed('all', [currentEnv.kafkaPort, currentEnv.mysqlPort, currentEnv.wsPort, currentEnv.syncPort, currentEnv.apiPort], currentEnv)
          runServiceProcess('kafka', 'start-kafka-kraft.bat', 'ELVIS - Apache Kafka KRaft', true)
          setTimeout(() => {
            runServiceProcess('mysql', 'start-mysql.bat', 'ELVIS - MySQL 9.71', true)
            setTimeout(() => {
              runServiceProcess('ws', 'start-elvis-connect-ws.bat', 'ELVIS - Connect WS Gateway', true)
              runServiceProcess('sync', 'start-elvis-connect-sync.bat', 'ELVIS - Connect Sync Engine', true)
              setTimeout(() => {
                runServiceProcess('api', 'start-elvis-control-api.bat', 'ELVIS - Control API Server', true)
              }, 2500)
            }, 4000)
          }, 2500)

          res.end(JSON.stringify({
            success: true,
            message: '전체 프로세스를 정리하고, 백그라운드 무창 모드로 인프라 및 코어 서비스를 순차 재기동합니다.'
          }))
          return
        }

        // 8-1. 인프라 미들웨어만 재기동 (POST /api/infra/restart-infra)
        if (pathname === '/api/infra/restart-infra') {
          const currentEnv = readEnv()
          console.log(`[ELVIS-Launcher] 인프라 미들웨어(MySQL, Kafka) 전용 재기동 요청`)

          await stopAndWaitClosed('all-infra', [currentEnv.kafkaPort, currentEnv.mysqlPort], currentEnv)
          runServiceProcess('kafka', 'start-kafka-kraft.bat', 'ELVIS - Apache Kafka KRaft', true)
          setTimeout(() => {
            runServiceProcess('mysql', 'start-mysql.bat', 'ELVIS - MySQL 9.71', true)
          }, 2500)

          res.end(JSON.stringify({
            success: true,
            message: '인프라 미들웨어(MySQL, Kafka)를 순차 재기동합니다.'
          }))
          return
        }

        // 8-2. 코어 백엔드 엔진만 재기동 (POST /api/infra/restart-services)
        if (pathname === '/api/infra/restart-services') {
          const currentEnv = readEnv()
          console.log(`[ELVIS-Launcher] 코어 백엔드 엔진(WS, Sync, API) 전용 재기동 요청`)

          await stopAndWaitClosed('all-services', [currentEnv.wsPort, currentEnv.syncPort, currentEnv.apiPort], currentEnv)
          runServiceProcess('ws', 'start-elvis-connect-ws.bat', 'ELVIS - Connect WS Gateway', true)
          runServiceProcess('sync', 'start-elvis-connect-sync.bat', 'ELVIS - Connect Sync Engine', true)
          setTimeout(() => {
            runServiceProcess('api', 'start-elvis-control-api.bat', 'ELVIS - Control API Server', true)
          }, 2500)

          res.end(JSON.stringify({
            success: true,
            message: '코어 백엔드 엔진(WS, Sync, API)을 순차 재기동합니다.'
          }))
          return
        }

        // 8-3. 개별 단일 서비스 재기동 (POST /api/infra/restart?service=mysql|kafka|ws|sync|api|tester&background=true|false)
        if (pathname === '/api/infra/restart') {
          const currentEnv = readEnv()
          const service = url.searchParams.get('service') || ''
          const isBackground = url.searchParams.get('background') !== 'false'
          console.log(`[ELVIS-Launcher] 개별 서비스 재기동 요청 -> service: ${service}, isBackground: ${isBackground}`)

          const servicePortsMap: Record<string, number[]> = {
            kafka: [currentEnv.kafkaPort],
            mysql: [currentEnv.mysqlPort],
            ws: [currentEnv.wsPort],
            sync: [currentEnv.syncPort],
            api: [currentEnv.apiPort],
            tester: [currentEnv.testerPort, 1422]
          }
          if (!Object.prototype.hasOwnProperty.call(servicePortsMap, service)) {
            res.statusCode = 400
            res.end(JSON.stringify({ success: false, message: `지원하지 않는 서비스입니다: ${service}` }))
            return
          }

          await stopAndWaitClosed(service, servicePortsMap[service], currentEnv)

          if (service === 'kafka') {
            runServiceProcess('kafka', 'start-kafka-kraft.bat', 'ELVIS - Apache Kafka KRaft', isBackground)
          } else if (service === 'mysql') {
            runServiceProcess('mysql', 'start-mysql.bat', 'ELVIS - MySQL 9.71', isBackground)
          } else if (service === 'ws') {
            runServiceProcess('ws', 'start-elvis-connect-ws.bat', 'ELVIS - Connect WS Gateway', isBackground)
          } else if (service === 'sync') {
            runServiceProcess('sync', 'start-elvis-connect-sync.bat', 'ELVIS - Connect Sync Engine', isBackground)
          } else if (service === 'api') {
            runServiceProcess('api', 'start-elvis-control-api.bat', 'ELVIS - Control API Server', isBackground)
          } else if (service === 'tester') {
            runServiceProcess('tester', 'start-all-tester.bat', 'ELVIS - Tester Harness', isBackground)
          }

          res.end(JSON.stringify({
            success: true,
            message: `${service} 재기동 명령이 전송되었습니다.`
          }))
          return
        }

        // 9. 미들웨어 콘솔 가시화/재기동 (POST /api/infra/open-console)
        if (pathname === '/api/infra/open-console') {
          const currentEnv = readEnv()
          await stopAndWaitClosed('all-infra', [currentEnv.kafkaPort, currentEnv.mysqlPort], currentEnv)

          runServiceProcess('kafka', 'start-kafka-kraft.bat', 'ELVIS - Apache Kafka KRaft', false)
          setTimeout(() => {
            runServiceProcess('mysql', 'start-mysql.bat', 'ELVIS - MySQL 9.71', false)
          }, 2000)

          res.end(JSON.stringify({
            success: true,
            message: 'MySQL 9.71 및 Apache Kafka 콘솔 윈도우 창이 화면에 성공적으로 기동되었습니다.'
          }))
          return
        }

        // 9-1. 프로세스 및 포트 점유 실시간 진단 창 기동 (POST /api/infra/open-process-monitor?target=all|infra|core|extension)
        if (pathname === '/api/infra/open-process-monitor') {
          const target = url.searchParams.get('target') || 'all'
          if (!ALLOWED_MONITOR_TARGETS.has(target)) {
            res.statusCode = 400
            res.end(JSON.stringify({ success: false, message: `허용되지 않은 진단 타겟입니다: ${target}` }))
            return
          }

          // 0) 독립 Rust 네이티브 GUI 모듈(elvis-log-viewer.exe) 프로세스 진단 모드 우선 실행
          const viewerCandidates = [
            path.resolve(__dirname, '..', 'elvis-log-viewer', 'target', 'release', 'elvis-log-viewer.exe'),
            path.resolve(__dirname, '..', 'elvis-log-viewer', 'target', 'debug', 'elvis-log-viewer.exe')
          ]
          const viewerExe = viewerCandidates.find((p) => fs.existsSync(p))
          if (viewerExe) {
            const currentEnv = readEnv()
            const logDir = currentEnv.logDir || 'D:\\elvis-lite\\logs'
            const portsArg = `${currentEnv.mysqlPort},${currentEnv.kafkaPort},${currentEnv.wsPort},${currentEnv.syncPort},${currentEnv.apiPort}`
            const scopeArg = (target === 'infra' || target === 'core') ? target : 'all'
            try {
              const child = spawn(viewerExe, [
                '--log-dir', logDir,
                '--mode', 'process',
                '--target', scopeArg,
                '--ports', portsArg
              ], { detached: true, stdio: 'ignore' })
              child.unref()
              const scopeLabel = scopeArg === 'infra' ? '미들웨어' : scopeArg === 'core' ? '코어 엔진' : '전체 시스템'
              res.end(JSON.stringify({
                success: true,
                target,
                message: `[${scopeLabel}] 프로세스 및 포트 진단 뷰어를 열었습니다.`
              }))
              return
            } catch (_) {
              // 실패 시 아래 check-status.bat 콘솔로 폴백
            }
          }

          const batPath = path.resolve(scriptsDir, 'check-status.bat')
          const safeBat = batPath.replace(/"/g, '')
          const child = spawn('cmd.exe', ['/c', 'start', 'ELVIS - 프로세스 및 포트 점유 실시간 진단', 'cmd', '/c', `call "${safeBat}" ${target}`], {
            cwd: scriptsDir,
            detached: true,
            stdio: 'ignore'
          })
          child.unref()
          res.end(JSON.stringify({
            success: true,
            target,
            message: `[${target.toUpperCase()}] 프로세스 및 포트 점유 실시간 진단 터미널이 열렸습니다.`
          }))
          return
        }

        // 10. 콘솔 로그 조회 (GET /api/infra/logs?service=mysql|kafka|ws|sync|api|tester&tail=250&type=all|error)
        if (pathname === '/api/infra/logs' && req.method === 'GET') {
          const currentEnv = readEnv()
          const logDir = currentEnv.logDir || 'D:\\elvis-lite\\logs'
          const service = url.searchParams.get('service') || 'mysql'
          if (!ALLOWED_LOG_SERVICES.has(service)) {
            res.statusCode = 400
            res.end(JSON.stringify({ success: false, message: `허용되지 않은 서비스입니다: ${service}` }))
            return
          }
          const tailLines = parseInt(url.searchParams.get('tail') || '250', 10)
          const logType = url.searchParams.get('type') === 'error' ? 'error' : 'all'

          let logFile = path.resolve(logDir, `${service}.log`)
          const errLogFile = path.resolve(logDir, `${service}-err.log`)
          let isDedicatedErrFile = false

          if (logType === 'error' && fs.existsSync(errLogFile)) {
            logFile = errLogFile
            isDedicatedErrFile = true
          }

          if (!fs.existsSync(logFile)) {
            res.end(JSON.stringify({
              success: true,
              service,
              type: logType,
              exists: false,
              message: `${path.basename(logFile)} 파일이 아직 없습니다.`,
              lines: [],
              fileSize: 0,
              lastModified: null
            }))
            return
          }

          try {
            const stats = fs.statSync(logFile)
            const maxReadBytes = 512 * 1024
            const fileSize = stats.size
            const readSize = Math.min(fileSize, maxReadBytes)
            const buffer = Buffer.alloc(readSize)
            const fd = fs.openSync(logFile, 'r')
            fs.readSync(fd, buffer, 0, readSize, Math.max(0, fileSize - readSize))
            fs.closeSync(fd)

            const rawContent = buffer.toString('utf-8')
            let allLines = rawContent.split(/\r?\n/)
            if (fileSize > maxReadBytes && allLines.length > 1) {
              allLines.shift()
            }

            if (logType === 'error' && !isDedicatedErrFile) {
              allLines = allLines.filter(line => /error|exception|fatal|fail|심각|실패/i.test(line))
            }

            const recentLines = allLines.slice(-tailLines)

            res.end(JSON.stringify({
              success: true,
              service,
              type: logType,
              exists: true,
              fileSize: stats.size,
              lastModified: stats.mtime.toISOString(),
              lines: recentLines
            }))
          } catch (err: any) {
            res.end(JSON.stringify({
              success: true,
              service,
              type: logType,
              exists: true,
              fileSize: 0,
              lastModified: null,
              lines: [`[시스템 안내] 로그 파일 접근 대기 중... (${err.message})`]
            }))
          }
          return
        }

        // 11. 콘솔 로그 비우기 (POST /api/infra/logs/clear?service=mysql|kafka)
        if (pathname === '/api/infra/logs/clear' && req.method === 'POST') {
          const currentEnv = readEnv()
          const logDir = currentEnv.logDir || 'D:\\elvis-lite\\logs'
          const service = url.searchParams.get('service') || 'mysql'
          if (!ALLOWED_LOG_SERVICES.has(service)) {
            res.statusCode = 400
            res.end(JSON.stringify({ success: false, message: `허용되지 않은 서비스입니다: ${service}` }))
            return
          }
          const logFile = path.resolve(logDir, `${service}.log`)

          try {
            if (fs.existsSync(logFile)) {
              fs.writeFileSync(logFile, '', 'utf-8')
            }
            res.end(JSON.stringify({
              success: true,
              service,
              message: `${service} 콘솔 로그를 성공적으로 비웠습니다.`
            }))
          } catch (err: any) {
            res.statusCode = 500
            res.end(JSON.stringify({ success: false, message: '로그 비우기 실패: ' + err.message }))
          }
          return
        }

        // 11-1. 실시간 콘솔 터미널 창 기동 (POST /api/infra/logs/open-terminal?service=mysql&type=all|error&wrap=0|1)
        if (pathname === '/api/infra/logs/open-terminal') {
          const currentEnv = readEnv()
          const logDir = currentEnv.logDir || 'D:\\elvis-lite\\logs'
          const service = url.searchParams.get('service') || 'mysql'
          if (!ALLOWED_LOG_SERVICES.has(service)) {
            res.statusCode = 400
            res.end(JSON.stringify({ success: false, message: `허용되지 않은 서비스입니다: ${service}` }))
            return
          }
          const logType = url.searchParams.get('type') === 'error' ? 'error' : 'all'
          const isWordWrap = url.searchParams.get('wrap') !== '0'
          const wrapLabel = isWordWrap ? '줄바꿈 ON' : '줄바꿈 OFF'
          const psInit = isWordWrap
            ? `[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; chcp 65001 >$null;`
            : `[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; chcp 65001 >$null; function Out-NoWrap { process { $line = ([string]$_).Replace([string][char]9, '  ').Replace([string][char]13, ''); $line = [regex]::Replace($line, ([string][char]27) + '\\[[0-9;]*[A-Za-z]', ''); $max = 78; try { $w1 = [Console]::WindowWidth; $w2 = $Host.UI.RawUI.WindowSize.Width; $winW = if ($w1 -gt 5 -and $w2 -gt 5) { [Math]::Min($w1, $w2) } elseif ($w1 -gt 5) { $w1 } elseif ($w2 -gt 5) { $w2 } else { 80 }; $max = [Math]::Max(8, $winW - 2) } catch {}; $w = 0; $sb = [System.Text.StringBuilder]::new(); foreach ($ch in $line.ToCharArray()) { $cw = if ([int]$ch -gt 127) { 2 } else { 1 }; if ($w + $cw -gt $max) { break }; [void]$sb.Append($ch); $w += $cw }; Write-Host $sb.ToString() } };`
          const pipeSuffix = isWordWrap ? '' : ' | Out-NoWrap'

          const titles: Record<string, string> = {
            mysql: `MySQL 9.71 (:${currentEnv.mysqlPort})`,
            kafka: `Apache Kafka (:${currentEnv.kafkaPort})`,
            ws: `elvis-connect-ws (:${currentEnv.wsPort})`,
            sync: `elvis-connect-sync (:${currentEnv.syncPort})`,
            api: `elvis-control-api (:${currentEnv.apiPort})`,
            tester: `elvis-tester (:${currentEnv.testerPort})`
          }
          const display = titles[service] || service

          const logFile = path.resolve(logDir, `${service}.log`)
          const errFile = path.resolve(logDir, `${service}-err.log`)
          const isErrorMode = logType === 'error'

          // 0) 독립 Rust 네이티브 GUI 모듈(elvis-log-viewer.exe) 우선 실행 (5대 서비스 단일 프로세스 모드)
          if (['mysql', 'kafka', 'ws', 'sync', 'api'].includes(service)) {
            const viewerCandidates = [
              path.resolve(__dirname, '..', 'elvis-log-viewer', 'target', 'release', 'elvis-log-viewer.exe'),
              path.resolve(__dirname, '..', 'elvis-log-viewer', 'target', 'debug', 'elvis-log-viewer.exe')
            ]
            const viewerExe = viewerCandidates.find((p) => fs.existsSync(p))
            if (viewerExe) {
              const portsArg = `${currentEnv.mysqlPort},${currentEnv.kafkaPort},${currentEnv.wsPort},${currentEnv.syncPort},${currentEnv.apiPort}`
              const viewerArgs = [
                '--log-dir', logDir,
                '--service', service,
                '--type', isErrorMode ? 'error' : 'all',
                '--wrap', isWordWrap ? '1' : '0',
                '--ports', portsArg
              ]
              try {
                const child = spawn(viewerExe, viewerArgs, { detached: true, stdio: 'ignore' })
                child.unref()
                res.end(JSON.stringify({
                  success: true,
                  message: `${display} ${isErrorMode ? '에러 로그' : '실시간 로그'} 뷰어를 열었습니다. (${wrapLabel})`
                }))
                return
              } catch (_) {
                // 실패 시 아래 PowerShell 콘솔로 폴백
              }
            }
          }

          let winTitle = `ELVIS - ${display} 실시간 콘솔 로그 [${wrapLabel}]`
          if (isErrorMode) {
            winTitle = `ELVIS - ${display} 실시간 [에러] 콘솔 로그 [${wrapLabel}]`
          }

          const safeTitleCmd = sanitizeCmdTitle(winTitle)
          const psWinTitle = escapePsSingleQuoted(winTitle)
          const psDisplay = escapePsSingleQuoted(display)
          const psLogFile = escapePsSingleQuoted(logFile)
          const psErrFile = escapePsSingleQuoted(errFile)
          const psErrBase = escapePsSingleQuoted(path.basename(errFile))

          // 1) 기존 동일 서비스 로그창이 이미 열려 있는지 검사 -> 활성화(Focus) 성공 시 중복 생성 방지
          const checkScript = `$w = New-Object -ComObject WScript.Shell; if ($w.AppActivate('${psWinTitle}')) { exit 10 }; $p = Get-Process -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowTitle -like '*${psWinTitle}*' }; if ($p) { if ($w.AppActivate($p[0].Id)) { exit 10 } }; exit 0`
          try {
            await execFileAsync('powershell', ['-NoProfile', '-WindowStyle', 'Hidden', '-EncodedCommand', encodePsBase64(checkScript)])
          } catch (checkErr: any) {
            if (checkErr && checkErr.code === 10) {
              res.end(JSON.stringify({ success: true, message: `기존 ${display} 콘솔 창을 활성화했습니다.` }))
              return
            }
          }

          // 2) 신규 창 기동 스크립트 작성
          let psCmd = ''
          if (isErrorMode) {
            if (fs.existsSync(errFile)) {
              psCmd = `${psInit} $host.UI.RawUI.WindowTitle = '${psWinTitle}'; Clear-Host; Write-Host '========================================================================================================' -ForegroundColor DarkRed; Write-Host '  ELVIS CSMS Lite - ${psDisplay} 실시간 [에러] 콘솔 로그 [${wrapLabel}]' -ForegroundColor Red; Write-Host '  에러 로그 경로: ${psErrFile}' -ForegroundColor DarkGray; Write-Host '  (종료: 창 닫기 또는 Ctrl+C)' -ForegroundColor DarkGray; Write-Host '========================================================================================================' -ForegroundColor DarkRed; Write-Host ''; if (Test-Path '${psErrFile}') { Write-Host '[모니터링] ${psDisplay} 전용 에러 로그 스트림을 감시합니다.' -ForegroundColor Red; Get-Content -Path '${psErrFile}' -Tail 150 -Wait -Encoding utf8${pipeSuffix} } else { Write-Host '[안내] 에러 로그 파일(${psErrBase}) 대기 중...' -ForegroundColor Yellow; while (-not (Test-Path '${psErrFile}')) { Start-Sleep -Seconds 2 }; Clear-Host; Write-Host '[연결됨] 에러 로그 감시를 시작합니다.' -ForegroundColor Red; Get-Content -Path '${psErrFile}' -Tail 150 -Wait -Encoding utf8${pipeSuffix} }`
            } else {
              psCmd = `${psInit} $host.UI.RawUI.WindowTitle = '${psWinTitle}'; Clear-Host; Write-Host '========================================================================================================' -ForegroundColor DarkRed; Write-Host '  ELVIS CSMS Lite - ${psDisplay} 실시간 [에러/예외] 필터 스트림 [${wrapLabel}]' -ForegroundColor Red; Write-Host '  감시 대상: ${psLogFile} (ERROR | Exception | FATAL | 실패 필터링)' -ForegroundColor DarkGray; Write-Host '  (종료: 창 닫기 또는 Ctrl+C)' -ForegroundColor DarkGray; Write-Host '========================================================================================================' -ForegroundColor DarkRed; Write-Host ''; if (Test-Path '${psLogFile}') { Write-Host '[실시간 필터] ERROR/Exception 발생 이벤트를 대기 및 모니터링합니다...' -ForegroundColor DarkYellow; Get-Content -Path '${psLogFile}' -Tail 200 -Wait -Encoding utf8 | Where-Object { $_ -match 'ERROR|Exception|FATAL|경고|실패|RuntimeException|NullPointer' }${pipeSuffix} } else { Write-Host '[안내] 원본 로그 파일 대기 중...' -ForegroundColor Yellow; while (-not (Test-Path '${psLogFile}')) { Start-Sleep -Seconds 2 }; Clear-Host; Write-Host '[연결됨] 에러 필터 감시를 시작합니다.' -ForegroundColor Red; Get-Content -Path '${psLogFile}' -Tail 200 -Wait -Encoding utf8 | Where-Object { $_ -match 'ERROR|Exception|FATAL|경고|실패|RuntimeException|NullPointer' }${pipeSuffix} }`
            }
          } else {
            psCmd = `${psInit} $host.UI.RawUI.WindowTitle = '${psWinTitle}'; Clear-Host; Write-Host '========================================================================================================' -ForegroundColor DarkCyan; Write-Host '  ELVIS CSMS Lite - ${psDisplay} 실시간 콘솔 로그 [${wrapLabel}]' -ForegroundColor Cyan; Write-Host '  로그 경로: ${psLogFile}' -ForegroundColor DarkGray; Write-Host '  (종료: 창 닫기 또는 Ctrl+C)' -ForegroundColor DarkGray; Write-Host '========================================================================================================' -ForegroundColor DarkCyan; Write-Host ''; if (Test-Path '${psLogFile}') { Get-Content -Path '${psLogFile}' -Tail 150 -Wait -Encoding utf8${pipeSuffix} } else { Write-Host '[안내] 아직 로그 파일이 없습니다. 서비스 기동 대기 중...' -ForegroundColor Yellow; while (-not (Test-Path '${psLogFile}')) { Start-Sleep -Seconds 2 }; Clear-Host; Write-Host '[연결됨] 로그 감시를 시작합니다.' -ForegroundColor Green; Get-Content -Path '${psLogFile}' -Tail 150 -Wait -Encoding utf8${pipeSuffix} }`
          }

          try {
            const child = spawn('cmd.exe', ['/c', 'start', safeTitleCmd, 'powershell', '-NoExit', '-EncodedCommand', encodePsBase64(psCmd)], {
              detached: true,
              stdio: 'ignore'
            })
            child.unref()
            res.end(JSON.stringify({ success: true, message: `${display} ${isErrorMode ? '에러' : '표준'} 터미널 콘솔이 실행되었습니다.` }))
          } catch (err: any) {
            res.statusCode = 500
            res.end(JSON.stringify({ success: false, message: '터미널 기동 실패: ' + err.message }))
          }
          return
        }

        // 11-2. Windows Terminal(wt.exe) 멀티 팬 화면 분할 일괄 로그 창 기동
        // - all (5분할): 1:WS, 3:SYNC, 2+4:API, 5:MYSQL, 6:KAFKA
        // - core (3분할): 1:WS, 3:SYNC, 2+4:API
        // - infra (2분할): 5:MYSQL, 6:KAFKA
        if (pathname === '/api/infra/logs/open-group') {
          const currentEnv = readEnv()
          const logDir = currentEnv.logDir || 'D:\\elvis-lite\\logs'
          const group = url.searchParams.get('group') || 'all'
          if (!['all', 'infra', 'core'].includes(group)) {
            res.statusCode = 400
            res.end(JSON.stringify({ success: false, message: `허용되지 않은 로그 그룹입니다: ${group}` }))
            return
          }
          const isErrorMode = url.searchParams.get('type') === 'error'
          const isWordWrap = url.searchParams.get('wrap') === '1'
          const modeSuffix = isErrorMode ? '에러 로그' : '실시간 로그'
          const wrapLabel = isWordWrap ? '줄바꿈 ON' : '줄바꿈 OFF'

          // 1) 독립 Rust 네이티브 GUI 모듈(elvis-log-viewer.exe) 우선 실행
          const viewerCandidates = [
            path.resolve(__dirname, '..', 'elvis-log-viewer', 'target', 'release', 'elvis-log-viewer.exe'),
            path.resolve(__dirname, '..', 'elvis-log-viewer', 'target', 'debug', 'elvis-log-viewer.exe')
          ]
          const viewerExe = viewerCandidates.find((p) => fs.existsSync(p))
          if (viewerExe) {
            const portsArg = `${currentEnv.mysqlPort},${currentEnv.kafkaPort},${currentEnv.wsPort},${currentEnv.syncPort},${currentEnv.apiPort}`
            const viewerArgs = [
              '--log-dir', logDir,
              '--group', group,
              '--type', isErrorMode ? 'error' : 'all',
              '--wrap', isWordWrap ? '1' : '0',
              '--ports', portsArg
            ]
            try {
              const child = spawn(viewerExe, viewerArgs, { detached: true, stdio: 'ignore' })
              child.unref()
              const msg = group === 'infra'
                ? `미들웨어 2분할 ${modeSuffix} 뷰어를 열었습니다. (${wrapLabel})`
                : group === 'core'
                  ? `코어 엔진 3분할 ${modeSuffix} 뷰어를 열었습니다. (${wrapLabel})`
                  : `전체 5분할 ${modeSuffix} 뷰어를 열었습니다. (${wrapLabel})`
              res.end(JSON.stringify({ success: true, message: msg }))
              return
            } catch (_) {
              // 실패 시 아래 wt.exe 분할로 폴백
            }
          }

          const titles: Record<string, string> = {
            mysql: `MySQL 9.71 (:${currentEnv.mysqlPort})`,
            kafka: `Apache Kafka (:${currentEnv.kafkaPort})`,
            ws: `elvis-connect-ws (:${currentEnv.wsPort})`,
            sync: `elvis-connect-sync (:${currentEnv.syncPort})`,
            api: `elvis-control-api (:${currentEnv.apiPort})`
          }

          const buildPaneEncoded = (svc: string): string => {
            const display = escapePsSingleQuoted(titles[svc] || svc)
            const logFile = path.resolve(logDir, `${svc}.log`)
            const errFile = path.resolve(logDir, `${svc}-err.log`)
            const psLogFile = escapePsSingleQuoted(logFile)
            const psErrFile = escapePsSingleQuoted(errFile)
            const psFileBase = escapePsSingleQuoted(path.basename(logFile))

            const initScript = isWordWrap
              ? `[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; chcp 65001 >$null;`
              : `[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; chcp 65001 >$null; function Out-NoWrap { process { $line = ([string]$_).Replace([string][char]9, '  ').Replace([string][char]13, ''); $line = [regex]::Replace($line, ([string][char]27) + '\\[[0-9;]*[A-Za-z]', ''); $max = 78; try { $w1 = [Console]::WindowWidth; $w2 = $Host.UI.RawUI.WindowSize.Width; $winW = if ($w1 -gt 5 -and $w2 -gt 5) { [Math]::Min($w1, $w2) } elseif ($w1 -gt 5) { $w1 } elseif ($w2 -gt 5) { $w2 } else { 80 }; $max = [Math]::Max(8, $winW - 2) } catch {}; $w = 0; $sb = [System.Text.StringBuilder]::new(); foreach ($ch in $line.ToCharArray()) { $cw = if ([int]$ch -gt 127) { 2 } else { 1 }; if ($w + $cw -gt $max) { break }; [void]$sb.Append($ch); $w += $cw }; Write-Host $sb.ToString() } };`
            const postClearVt = isWordWrap
              ? ''
              : `$prevW = -1; $stable = 0; for ($i = 0; $i -lt 8; $i++) { Start-Sleep -Milliseconds 150; $curW = 0; try { $curW = [Console]::WindowWidth } catch {}; if ($curW -gt 5 -and $curW -eq $prevW) { $stable++; if ($stable -ge 3) { break } } else { $stable = 0; $prevW = $curW } }; Clear-Host;`
            const pipeSuffix = isWordWrap ? '' : ' | Out-NoWrap'

            let psCmd = ''
            if (isErrorMode) {
              if (fs.existsSync(errFile)) {
                psCmd = `${initScript} Clear-Host; ${postClearVt} Write-Host '=== [에러] ${display} [${wrapLabel}] ===' -ForegroundColor Red; Get-Content -Path '${psErrFile}' -Tail 150 -Wait -Encoding utf8${pipeSuffix}`
              } else {
                psCmd = `${initScript} Clear-Host; ${postClearVt} Write-Host '=== [에러] ${display} [${wrapLabel}] ===' -ForegroundColor Red; if (Test-Path '${psLogFile}') { Get-Content -Path '${psLogFile}' -Tail 200 -Wait -Encoding utf8 | Where-Object { $_ -match 'ERROR|Exception|FATAL|경고|실패|RuntimeException|NullPointer' }${pipeSuffix} } else { Write-Host '[대기] ${psFileBase} 파일 생성 대기 중...' -ForegroundColor Yellow; while (-not (Test-Path '${psLogFile}')) { Start-Sleep -Seconds 2 }; Get-Content -Path '${psLogFile}' -Tail 200 -Wait -Encoding utf8 | Where-Object { $_ -match 'ERROR|Exception|FATAL|경고|실패|RuntimeException|NullPointer' }${pipeSuffix} }`
              }
            } else {
              psCmd = `${initScript} Clear-Host; ${postClearVt} Write-Host '=== ${display} [${wrapLabel}] ===' -ForegroundColor Cyan; if (Test-Path '${psLogFile}') { Get-Content -Path '${psLogFile}' -Tail 150 -Wait -Encoding utf8${pipeSuffix} } else { Write-Host '[대기] ${psFileBase} 파일 생성 대기 중...' -ForegroundColor Yellow; while (-not (Test-Path '${psLogFile}')) { Start-Sleep -Seconds 2 }; Get-Content -Path '${psLogFile}' -Tail 150 -Wait -Encoding utf8${pipeSuffix} }`
            }
            return encodePsBase64(psCmd)
          }

          const wtArgs: string[] = ['-M', '-w', 'new']
          if (group === 'infra') {
            wtArgs.push(
              'new-tab', '--title', `ELVIS 미들웨어 ${modeSuffix} (2분할 · ${wrapLabel})`, '--suppressApplicationTitle',
              'powershell', '-NoProfile', '-NoExit', '-EncodedCommand', buildPaneEncoded('mysql'),
              ';',
              'split-pane', '-V', '-s', '0.5', '--suppressApplicationTitle',
              'powershell', '-NoProfile', '-NoExit', '-EncodedCommand', buildPaneEncoded('kafka')
            )
          } else if (group === 'core') {
            wtArgs.push(
              'new-tab', '--title', `ELVIS 코어 엔진 ${modeSuffix} (3분할 · ${wrapLabel})`, '--suppressApplicationTitle',
              'powershell', '-NoProfile', '-NoExit', '-EncodedCommand', buildPaneEncoded('ws'),
              ';',
              'split-pane', '-V', '-s', '0.5', '--suppressApplicationTitle',
              'powershell', '-NoProfile', '-NoExit', '-EncodedCommand', buildPaneEncoded('api'),
              ';',
              'move-focus', 'left',
              ';',
              'split-pane', '-H', '-s', '0.5', '--suppressApplicationTitle',
              'powershell', '-NoProfile', '-NoExit', '-EncodedCommand', buildPaneEncoded('sync')
            )
          } else {
            // 3행 2열 격자: 1:WS, 3:SYNC, 2+4:API, 5:MYSQL, 6:KAFKA
            wtArgs.push(
              'new-tab', '--title', `ELVIS 전체 ${modeSuffix} (5분할 · ${wrapLabel})`, '--suppressApplicationTitle',
              'powershell', '-NoProfile', '-NoExit', '-EncodedCommand', buildPaneEncoded('ws'),
              ';',
              'split-pane', '-H', '-s', '0.333', '--suppressApplicationTitle',
              'powershell', '-NoProfile', '-NoExit', '-EncodedCommand', buildPaneEncoded('mysql'),
              ';',
              'split-pane', '-V', '-s', '0.5', '--suppressApplicationTitle',
              'powershell', '-NoProfile', '-NoExit', '-EncodedCommand', buildPaneEncoded('kafka'),
              ';',
              'move-focus', 'up',
              ';',
              'split-pane', '-V', '-s', '0.5', '--suppressApplicationTitle',
              'powershell', '-NoProfile', '-NoExit', '-EncodedCommand', buildPaneEncoded('api'),
              ';',
              'move-focus', 'left',
              ';',
              'split-pane', '-H', '-s', '0.5', '--suppressApplicationTitle',
              'powershell', '-NoProfile', '-NoExit', '-EncodedCommand', buildPaneEncoded('sync')
            )
          }

          try {
            const child = spawn('wt.exe', wtArgs, {
              detached: true,
              stdio: 'ignore'
            })
            let responded = false
            child.once('error', (err: any) => {
              if (!responded) {
                responded = true
                res.statusCode = 500
                res.end(JSON.stringify({ success: false, message: 'Windows Terminal(wt.exe) 기동 실패: ' + err.message }))
              }
            })
            child.once('spawn', () => {
              if (!responded) {
                responded = true
                child.unref()
                const msg = group === 'infra'
                  ? `미들웨어 2분할 ${modeSuffix} 터미널을 열었습니다. (${wrapLabel})`
                  : group === 'core'
                    ? `코어 엔진 3분할 ${modeSuffix} 터미널을 열었습니다. (${wrapLabel})`
                    : `전체 5분할 ${modeSuffix} 터미널을 열었습니다. (${wrapLabel})`
                res.end(JSON.stringify({ success: true, message: msg }))
              }
            })
          } catch (err: any) {
            res.statusCode = 500
            res.end(JSON.stringify({ success: false, message: '분할 터미널 기동 실패: ' + err.message }))
          }
          return
        }

        // 12. DDL 스키마 & 시드 데이터 적재 (POST /api/infra/schema)
        if (pathname === '/api/infra/schema') {
          const batPath = path.resolve(scriptsDir, 'load-mysql-schema.bat')
          exec(`cmd.exe /c "${batPath}"`, {
            cwd: scriptsDir,
            env: { ...process.env, NO_PAUSE: '1' }
          }, (err, stdout, stderr) => {
            if (err) {
              res.statusCode = 500
              res.end(JSON.stringify({ success: false, message: '스키마 적재 실패: ' + (stderr || err.message) }))
              return
            }
            res.end(JSON.stringify({
              success: true,
              message: 'MySQL (elvis-lite) DDL 스키마 및 초기 시드 데이터가 성공적으로 적재되었습니다.'
            }))
          })
          return
        }

        // 13. Kafka 4대 핵심 토픽 생성 (POST /api/infra/topics)
        if (pathname === '/api/infra/topics') {
          const currentEnv = readEnv()
          const batPath = path.resolve(scriptsDir, 'create-kafka-topics.bat')
          try {
            await execFileAsync('cmd.exe', ['/c', batPath], {
              cwd: scriptsDir,
              env: { ...process.env, NO_PAUSE: '1' }
            })
            const kafkaTopicsBat = path.resolve(rootDir, 'binaries/kafka_2.13-4.3.1/bin/windows/kafka-topics.bat')
            if (fs.existsSync(kafkaTopicsBat)) {
              await execFileAsync('cmd.exe', [
                '/c',
                kafkaTopicsBat,
                '--create',
                '--if-not-exists',
                '--topic',
                'ocpp-dlt',
                '--bootstrap-server',
                `localhost:${currentEnv.kafkaPort}`,
                '--partitions',
                '3',
                '--replication-factor',
                '1'
              ], {
                cwd: rootDir,
                env: { ...process.env, NO_PAUSE: '1' }
              }).catch(() => {})
            }
            res.end(JSON.stringify({
              success: true,
              message: 'Kafka 4대 핵심 토픽(ocpp-raw-events, ocpp-outbound-commands, ocpp-ui-notifications, ocpp-dlt)이 생성되었습니다.'
            }))
          } catch (err: any) {
            res.statusCode = 500
            res.end(JSON.stringify({ success: false, message: '토픽 생성 실패: ' + (err?.stderr || err?.message || '알 수 없는 오류') }))
          }
          return
        }

        next()
      })
    }
  }
}

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue(), infraLauncherPlugin()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    }
  },
  server: {
    port: 1420,
    strictPort: true,
    host: '127.0.0.1',
    watch: {
      ignored: ['**/src-tauri/**']
    }
  }
})
