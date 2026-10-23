import { defineConfig, Plugin } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import fs from 'fs'
import { exec, spawn } from 'child_process'
import net from 'net'

// 포트 점유 여부 확인 헬퍼 함수 (IPv4 & IPv6 동시 점검)
function checkPort(port: number): Promise<boolean> {
  return new Promise((resolve) => {
    let resolved = false
    const checkHost = (host: string) => {
      const socket = new net.Socket()
      socket.setTimeout(500)
      socket.once('connect', () => {
        socket.destroy()
        if (!resolved) {
          resolved = true
          resolve(true)
        }
      })
      socket.once('timeout', () => {
        socket.destroy()
      })
      socket.once('error', () => {
        socket.destroy()
      })
      socket.connect(port, host)
    }

    checkHost('127.0.0.1')
    checkHost('::1')

    setTimeout(() => {
      if (!resolved) {
        resolved = true
        resolve(false)
      }
    }, 600)
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
            testerPort: 9085,
            clickhousePort: 8123,
            clickhouseDataPort: 9083
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
              if (key === 'ELVIS_CLICKHOUSE_HTTP_PORT' && val) env.clickhousePort = parseInt(val.trim(), 10) || env.clickhousePort
              if (key === 'ELVIS_DATA_CH_PORT' && val) env.clickhouseDataPort = parseInt(val.trim(), 10) || env.clickhouseDataPort
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

          // 모든 백엔드 엔진(mysql, kafka, clickhouse, ws, sync, api, clickhouse-data, tester)은
          // 완전한 무창(100% Hidden) 백그라운드 독립 데몬으로 실행되며 런처 생명주기와 완전히 분리됩니다.
          const isPersistentEngine = ['mysql', 'kafka', 'clickhouse', 'ws', 'sync', 'api', 'clickhouse-data', 'tester'].includes(serviceName)
          const forceBackground = isPersistentEngine || isBackground

          if (forceBackground) {
            console.log(`[ELVIS-Launcher] WMI 기반 완전 독립 백그라운드 데몬 기동 -> service: ${serviceName}, bat: ${batFileName}`)
            // run-daemon.ps1 (WMI Win32_Process.Create)를 호출하여 부모 콘솔 및 런처 프로세스 트리와 100% 분리된 독립 OS 데몬으로 기동
            const daemonScript = path.resolve(scriptsDir, 'run-daemon.ps1')
            const psCmd = `powershell -NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File "${daemonScript}" -BatName "${batFileName}" -ServiceName "${serviceName}"`
            exec(psCmd, {
              cwd: scriptsDir,
              windowsHide: true,
              env: { ...process.env, NO_PAUSE: '1' }
            })
          } else {
            console.log(`[ELVIS-Launcher] 콘솔 창 기동 -> service: ${serviceName}, bat: ${batPath}`)
            exec(`start "${winTitle}" cmd /c "call \"${batPath}\""`, {
              cwd: scriptsDir,
              env: { ...process.env, NO_PAUSE: '1' }
            })
          }
        }

        // 1. 미들웨어 및 전체 서비스 헬스체크 (GET /api/infra/status)
        if (pathname === '/api/infra/status') {
          const currentEnv = readEnv()
          const [isKafka, isMysql, isControlApi, isConnectWs, isConnectSync, isClickhouse, isClickhouseData] = await Promise.all([
            checkPort(currentEnv.kafkaPort),
            checkPort(currentEnv.mysqlPort),
            checkPort(currentEnv.apiPort),
            checkPort(currentEnv.wsPort),
            checkPort(currentEnv.syncPort),
            checkPort(currentEnv.clickhousePort),
            checkPort(currentEnv.clickhouseDataPort)
          ])

          res.end(JSON.stringify({
            kafka: isKafka,
            mysql: isMysql,
            controlApi: isControlApi,
            connectWs: isConnectWs,
            connectSync: isConnectSync,
            clickhouse: isClickhouse,
            clickhouseData: isClickhouseData,
            ports: {
              mysql: currentEnv.mysqlPort,
              kafka: currentEnv.kafkaPort,
              api: currentEnv.apiPort,
              ws: currentEnv.wsPort,
              sync: currentEnv.syncPort,
              clickhouse: currentEnv.clickhousePort,
              clickhouseData: currentEnv.clickhouseDataPort
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

              // paths.env 저장
              const newEnvContent = [
                '# ========================================================',
                '# ELVIS-CSMS 스토리지 및 네트워크 포트 설정 파일 (관리자 자동 반영)',
                '# ========================================================',
                `ELVIS_DATA_DIR=${currentEnv.dataDir}`,
                `ELVIS_LOG_DIR=${currentEnv.logDir}`,
                '',
                '# 네트워크 미들웨어 포트',
                `ELVIS_MYSQL_PORT=${mysqlPort}`,
                `ELVIS_KAFKA_PORT=${kafkaPort}`,
                `ELVIS_API_PORT=${apiPort}`,
                `ELVIS_WS_PORT=${wsPort}`,
                ''
              ].join('\r\n')
              fs.writeFileSync(envPath, newEnvContent, 'utf-8')

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
                conf = conf.replace(/advertised\.listeners=PLAINTEXT:\/\/localhost:(\d+)/g, `advertised.listeners=PLAINTEXT://localhost:${kafkaPort}`)
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
            runServiceProcess('tester', 'start-elvis-tester.bat', 'ELVIS - Tester Harness', isBackground)
          } else if (service === 'clickhouse') {
            runServiceProcess('clickhouse', 'start-clickhouse.bat', 'ELVIS - ClickHouse v26.7 Server', isBackground)
          } else if (service === 'clickhouse-data') {
            runServiceProcess('clickhouse-data', 'start-elvis-data-clickhouse.bat', 'ELVIS - ClickHouse Bulk Engine', isBackground)
          } else if (service === 'all-extensions') {
            // 확장 기능 전체 기동: ClickHouse Server -> elvis-data-clickhouse
            runServiceProcess('clickhouse', 'start-clickhouse.bat', 'ELVIS - ClickHouse v26.7 Server', isBackground)
            setTimeout(() => {
              runServiceProcess('clickhouse-data', 'start-elvis-data-clickhouse.bat', 'ELVIS - ClickHouse Bulk Engine', isBackground)
            }, 3000)
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

        // 7. 서비스 중지 (POST /api/infra/stop?service=mysql|kafka|ws|sync|api|clickhouse|clickhouse-data|all-extensions|tester|all-infra|all-services|all)
        if (pathname === '/api/infra/stop') {
          const currentEnv = readEnv()
          const service = url.searchParams.get('service')
          if (service === 'kafka') {
            exec(`powershell -Command "Get-NetTCPConnection -LocalPort ${currentEnv.kafkaPort} -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /fi \\"WINDOWTITLE eq ELVIS - Apache Kafka*\\" 2>$null; Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -match 'kafka.Kafka' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }"`)
          } else if (service === 'mysql') {
            exec(`taskkill /f /im mysqld.exe & powershell -Command "Get-NetTCPConnection -LocalPort ${currentEnv.mysqlPort} -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /fi \\"WINDOWTITLE eq ELVIS - MySQL*\\" 2>$null"`)
          } else if (service === 'ws') {
            exec(`powershell -Command "Get-NetTCPConnection -LocalPort ${currentEnv.wsPort} -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /fi \\"WINDOWTITLE eq ELVIS - Connect WS*\\" 2>$null"`)
          } else if (service === 'sync') {
            exec(`powershell -Command "Get-NetTCPConnection -LocalPort ${currentEnv.syncPort} -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /fi \\"WINDOWTITLE eq ELVIS - Connect Sync*\\" 2>$null"`)
          } else if (service === 'api') {
            exec(`powershell -Command "Get-NetTCPConnection -LocalPort ${currentEnv.apiPort} -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /fi \\"WINDOWTITLE eq ELVIS - Control API*\\" 2>$null"`)
          } else if (service === 'clickhouse') {
            exec(`powershell -Command "Get-NetTCPConnection -LocalPort ${currentEnv.clickhousePort} -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /fi \\"WINDOWTITLE eq ELVIS - ClickHouse*\\" 2>$null; wsl.exe -d Ubuntu-24.04 -u root pkill -f 'clickhouse server' 2>$null"`)
          } else if (service === 'clickhouse-data') {
            exec(`powershell -Command "Get-NetTCPConnection -LocalPort ${currentEnv.clickhouseDataPort} -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /fi \\"WINDOWTITLE eq ELVIS - ClickHouse Bulk*\\" 2>$null"`)
          } else if (service === 'all-extensions') {
            exec(`powershell -Command "Get-NetTCPConnection -LocalPort ${currentEnv.clickhousePort}, ${currentEnv.clickhouseDataPort} -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /fi \\"WINDOWTITLE eq ELVIS - ClickHouse*\\" 2>$null; wsl.exe -d Ubuntu-24.04 -u root pkill -f 'clickhouse server' 2>$null"`)
          } else if (service === 'tester') {
            exec(`powershell -Command "Get-NetTCPConnection -LocalPort ${currentEnv.testerPort} -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /fi \\"WINDOWTITLE eq ELVIS - Tester Harness*\\" 2>$null"`)
          } else if (service === 'all-infra') {
            exec(`taskkill /f /im mysqld.exe & powershell -Command "Get-NetTCPConnection -LocalPort ${currentEnv.kafkaPort}, ${currentEnv.mysqlPort} -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /fi \\"WINDOWTITLE eq ELVIS - Apache Kafka*\\" 2>$null; taskkill /f /fi \\"WINDOWTITLE eq ELVIS - MySQL*\\" 2>$null; Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -match 'kafka.Kafka' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }"`)
          } else if (service === 'all-services') {
            exec(`powershell -Command "Get-NetTCPConnection -LocalPort ${currentEnv.wsPort}, ${currentEnv.syncPort}, ${currentEnv.apiPort} -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /fi \\"WINDOWTITLE eq ELVIS - Connect*\\" 2>$null; taskkill /f /fi \\"WINDOWTITLE eq ELVIS - Control*\\" 2>$null"`)
          } else if (service === 'all') {
            exec(`taskkill /f /im mysqld.exe & powershell -Command "Get-NetTCPConnection -LocalPort ${currentEnv.kafkaPort}, ${currentEnv.mysqlPort}, ${currentEnv.wsPort}, ${currentEnv.syncPort}, ${currentEnv.apiPort}, ${currentEnv.clickhousePort}, ${currentEnv.clickhouseDataPort} -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /fi \\"WINDOWTITLE eq ELVIS - Connect*\\" 2>$null; taskkill /f /fi \\"WINDOWTITLE eq ELVIS - Control*\\" 2>$null; taskkill /f /fi \\"WINDOWTITLE eq ELVIS - Apache*\\" 2>$null; taskkill /f /fi \\"WINDOWTITLE eq ELVIS - MySQL*\\" 2>$null; taskkill /f /fi \\"WINDOWTITLE eq ELVIS - ClickHouse*\\" 2>$null; wsl.exe -d Ubuntu-24.04 -u root pkill -f 'clickhouse server' 2>$null; Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -match 'kafka.Kafka' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }"`)
          }
          res.end(JSON.stringify({ success: true, message: `${service} 중지 명령이 전송되었습니다.` }))
          return
        }

        // 8. 백그라운드 전환 재기동 (POST /api/infra/restart-background)
        if (pathname === '/api/infra/restart-background') {
          const currentEnv = readEnv()
          console.log(`[ELVIS-Launcher] 기존 프로세스 정리 후 백그라운드 무창 전환 재기동 요청`)

          exec(`powershell -Command "Get-NetTCPConnection -LocalPort ${currentEnv.kafkaPort}, ${currentEnv.mysqlPort}, ${currentEnv.wsPort}, ${currentEnv.syncPort}, ${currentEnv.apiPort} -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /im mysqld.exe 2>$null; taskkill /f /fi \\"WINDOWTITLE eq ELVIS - Connect*\\" 2>$null; taskkill /f /fi \\"WINDOWTITLE eq ELVIS - Control*\\" 2>$null; taskkill /f /fi \\"WINDOWTITLE eq ELVIS - Apache*\\" 2>$null; taskkill /f /fi \\"WINDOWTITLE eq ELVIS - MySQL*\\" 2>$null; Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -match 'kafka.Kafka' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }"`, () => {
            setTimeout(() => {
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
            }, 1200)
          })

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

          exec(`powershell -Command "Get-NetTCPConnection -LocalPort ${currentEnv.kafkaPort}, ${currentEnv.mysqlPort} -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /im mysqld.exe 2>$null; taskkill /f /fi \\"WINDOWTITLE eq ELVIS - Apache Kafka*\\" 2>$null; taskkill /f /fi \\"WINDOWTITLE eq ELVIS - MySQL*\\" 2>$null; Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -match 'kafka.Kafka' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }"`, () => {
            setTimeout(() => {
              runServiceProcess('kafka', 'start-kafka-kraft.bat', 'ELVIS - Apache Kafka KRaft', true)
              setTimeout(() => {
                runServiceProcess('mysql', 'start-mysql.bat', 'ELVIS - MySQL 9.71', true)
              }, 2500)
            }, 1200)
          })

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

          exec(`powershell -Command "Get-NetTCPConnection -LocalPort ${currentEnv.wsPort}, ${currentEnv.syncPort}, ${currentEnv.apiPort} -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /fi \\"WINDOWTITLE eq ELVIS - Connect*\\" 2>$null; taskkill /f /fi \\"WINDOWTITLE eq ELVIS - Control*\\" 2>$null"`, () => {
            setTimeout(() => {
              runServiceProcess('ws', 'start-elvis-connect-ws.bat', 'ELVIS - Connect WS Gateway', true)
              runServiceProcess('sync', 'start-elvis-connect-sync.bat', 'ELVIS - Connect Sync Engine', true)
              setTimeout(() => {
                runServiceProcess('api', 'start-elvis-control-api.bat', 'ELVIS - Control API Server', true)
              }, 2500)
            }, 1200)
          })

          res.end(JSON.stringify({
            success: true,
            message: '코어 백엔드 엔진(WS, Sync, API)을 순차 재기동합니다.'
          }))
          return
        }

        // 8-3. 개별 단일 서비스 재기동 (POST /api/infra/restart?service=mysql|kafka|ws|sync|api&background=true|false)
        if (pathname === '/api/infra/restart') {
          const currentEnv = readEnv()
          const service = url.searchParams.get('service')
          const isBackground = url.searchParams.get('background') !== 'false'
          console.log(`[ELVIS-Launcher] 개별 서비스 재기동 요청 -> service: ${service}, isBackground: ${isBackground}`)

          const executeStop = (svc: string, onStopped: () => void) => {
            if (svc === 'kafka') {
              exec(`powershell -Command "Get-NetTCPConnection -LocalPort ${currentEnv.kafkaPort} -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /fi \\"WINDOWTITLE eq ELVIS - Apache Kafka*\\" 2>$null; Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -match 'kafka.Kafka' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }"`, onStopped)
            } else if (svc === 'mysql') {
              exec(`taskkill /f /im mysqld.exe & powershell -Command "Get-NetTCPConnection -LocalPort ${currentEnv.mysqlPort} -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /fi \\"WINDOWTITLE eq ELVIS - MySQL*\\" 2>$null"`, onStopped)
            } else if (svc === 'ws') {
              exec(`powershell -Command "Get-NetTCPConnection -LocalPort ${currentEnv.wsPort} -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /fi \\"WINDOWTITLE eq ELVIS - Connect WS*\\" 2>$null"`, onStopped)
            } else if (svc === 'sync') {
              exec(`powershell -Command "Get-NetTCPConnection -LocalPort ${currentEnv.syncPort} -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /fi \\"WINDOWTITLE eq ELVIS - Connect Sync*\\" 2>$null"`, onStopped)
            } else if (svc === 'api') {
              exec(`powershell -Command "Get-NetTCPConnection -LocalPort ${currentEnv.apiPort} -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /fi \\"WINDOWTITLE eq ELVIS - Control API*\\" 2>$null"`, onStopped)
            } else if (svc === 'clickhouse') {
              exec(`powershell -Command "Get-NetTCPConnection -LocalPort ${currentEnv.clickhousePort} -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /fi \\"WINDOWTITLE eq ELVIS - ClickHouse*\\" 2>$null; wsl.exe -d Ubuntu-24.04 -u root pkill -f 'clickhouse server' 2>$null"`, onStopped)
            } else if (svc === 'clickhouse-data') {
              exec(`powershell -Command "Get-NetTCPConnection -LocalPort ${currentEnv.clickhouseDataPort} -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /fi \\"WINDOWTITLE eq ELVIS - ClickHouse Bulk*\\" 2>$null"`, onStopped)
            } else {
              onStopped()
            }
          }

          const executeStart = (svc: string) => {
            if (svc === 'kafka') {
              runServiceProcess('kafka', 'start-kafka-kraft.bat', 'ELVIS - Apache Kafka KRaft', isBackground)
            } else if (svc === 'mysql') {
              runServiceProcess('mysql', 'start-mysql.bat', 'ELVIS - MySQL 9.71', isBackground)
            } else if (svc === 'ws') {
              runServiceProcess('ws', 'start-elvis-connect-ws.bat', 'ELVIS - Connect WS Gateway', isBackground)
            } else if (svc === 'sync') {
              runServiceProcess('sync', 'start-elvis-connect-sync.bat', 'ELVIS - Connect Sync Engine', isBackground)
            } else if (svc === 'api') {
              runServiceProcess('api', 'start-elvis-control-api.bat', 'ELVIS - Control API Server', isBackground)
            } else if (svc === 'clickhouse') {
              runServiceProcess('clickhouse', 'start-clickhouse.bat', 'ELVIS - ClickHouse v26.7 Server', isBackground)
            } else if (svc === 'clickhouse-data') {
              runServiceProcess('clickhouse-data', 'start-elvis-data-clickhouse.bat', 'ELVIS - ClickHouse Bulk Engine', isBackground)
            }
          }

          if (service) {
            executeStop(service, () => {
              setTimeout(() => {
                executeStart(service)
              }, 1200)
            })
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
          exec(`powershell -Command "Get-NetTCPConnection -LocalPort ${currentEnv.kafkaPort}, ${currentEnv.mysqlPort} -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /im mysqld.exe 2>$null; taskkill /f /fi \\"WINDOWTITLE eq ELVIS - *\\" 2>$null; Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -match 'kafka.Kafka' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }"`, () => {
            setTimeout(() => {
              runServiceProcess('kafka', 'start-kafka-kraft.bat', 'ELVIS - Apache Kafka KRaft', false)
              setTimeout(() => {
                runServiceProcess('mysql', 'start-mysql.bat', 'ELVIS - MySQL 9.71', false)
              }, 2000)
            }, 1200)
          })

          res.end(JSON.stringify({
            success: true,
            message: 'MySQL 9.71 및 Apache Kafka 콘솔 윈도우 창이 화면에 성공적으로 기동되었습니다.'
          }))
          return
        }

        // 9-1. 프로세스 및 포트 점유 실시간 진단 콘솔 기동 (POST /api/infra/open-process-monitor?target=all|infra|core|extension)
        if (pathname === '/api/infra/open-process-monitor') {
          const target = url.searchParams.get('target') || 'all'
          const batPath = path.resolve(scriptsDir, 'check-status.bat')
          exec(`start "ELVIS - 프로세스 및 포트 점유 실시간 진단" cmd /c "call \"${batPath}\" ${target}"`, {
            cwd: scriptsDir
          })
          res.end(JSON.stringify({
            success: true,
            target,
            message: `[${target.toUpperCase()}] 프로세스 및 포트 점유 실시간 진단 터미널이 열렸습니다.`
          }))
          return
        }

        // 10. 콘솔 로그 조회 (GET /api/infra/logs?service=mysql|clickhouse&tail=250&type=all|error)
        if (pathname === '/api/infra/logs' && req.method === 'GET') {
          const currentEnv = readEnv()
          const logDir = currentEnv.logDir || 'D:\\elvis-lite\\logs'
          const service = url.searchParams.get('service') || 'mysql'
          const tailLines = parseInt(url.searchParams.get('tail') || '250', 10)
          const logType = url.searchParams.get('type') || 'all'

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

        // 11-1. 실시간 콘솔 터미널 창 기동 (POST /api/infra/logs/open-terminal?service=mysql&type=all|error)
        if (pathname === '/api/infra/logs/open-terminal') {
          const currentEnv = readEnv()
          const logDir = currentEnv.logDir || 'D:\\elvis-lite\\logs'
          const service = url.searchParams.get('service') || 'mysql'
          const logType = url.searchParams.get('type') || 'all'

          const titles: Record<string, string> = {
            mysql: 'MySQL 9.71 (:13306)',
            kafka: 'Apache Kafka (:19092)',
            ws: 'elvis-connect-ws (:9080)',
            sync: 'elvis-connect-sync (:9081)',
            api: 'elvis-control-api (:9082)',
            clickhouse: 'ClickHouse v26.7 (:8123)',
            'clickhouse-data': 'elvis-data-clickhouse (:9083)',
            tester: 'elvis-tester (:9085)'
          }
          const display = titles[service] || service

          let logFile = path.resolve(logDir, `${service}.log`)
          const errFile = path.resolve(logDir, `${service}-err.log`)
          const isErrorMode = logType === 'error'

          let winTitle = `ELVIS - ${display} 실시간 콘솔 로그`
          if (isErrorMode) {
            winTitle = `ELVIS - ${display} 실시간 [에러] 콘솔 로그`
          }

          // 1) 기존 동일 서비스 로그창이 이미 열려 있는지 검사 -> 활성화(Focus) 성공 시 중복 생성 방지
          const checkCmd = `powershell -NoProfile -Command "$w = New-Object -ComObject WScript.Shell; if ($w.AppActivate('${winTitle}')) { exit 10 }; $p = Get-Process -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowTitle -like '*${winTitle}*' }; if ($p) { if ($w.AppActivate($p[0].Id)) { exit 10 } }; exit 0"`

          exec(checkCmd, (checkErr) => {
            if (checkErr && checkErr.code === 10) {
              res.end(JSON.stringify({ success: true, message: `기존 ${display} 콘솔 창을 활성화했습니다.` }))
              return
            }

            // 2) 신규 창 기동 스크립트 작성
            let psCmd = ''
            if (isErrorMode) {
              // 에러 모드: ClickHouse 등 전용 에러 파일이 있으면 그 파일 감시, 없으면 로그 파일에서 ERROR 라인 실시간 필터링
              if (service === 'clickhouse' || fs.existsSync(errFile)) {
                psCmd = `[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; chcp 65001 >$null; $host.UI.RawUI.WindowTitle = '${winTitle}'; Clear-Host; Write-Host '========================================================================================================' -ForegroundColor DarkRed; Write-Host '  ELVIS CSMS Lite - ${display} 실시간 [에러] 콘솔 로그' -ForegroundColor Red; Write-Host '  에러 로그 경로: ${errFile}' -ForegroundColor DarkGray; Write-Host '  (종료: 창 닫기 또는 Ctrl+C)' -ForegroundColor DarkGray; Write-Host '========================================================================================================' -ForegroundColor DarkRed; Write-Host ''; if (Test-Path '${errFile}') { Write-Host '[모니터링] ${display} 전용 에러 로그 스트림을 감시합니다.' -ForegroundColor Red; Get-Content -Path '${errFile}' -Tail 150 -Wait -Encoding utf8 } else { Write-Host '[안내] 에러 로그 파일(${path.basename(errFile)}) 대기 중...' -ForegroundColor Yellow; while (-not (Test-Path '${errFile}')) { Start-Sleep -Seconds 2 }; Clear-Host; Write-Host '[연결됨] 에러 로그 감시를 시작합니다.' -ForegroundColor Red; Get-Content -Path '${errFile}' -Tail 150 -Wait -Encoding utf8 }`
              } else {
                psCmd = `[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; chcp 65001 >$null; $host.UI.RawUI.WindowTitle = '${winTitle}'; Clear-Host; Write-Host '========================================================================================================' -ForegroundColor DarkRed; Write-Host '  ELVIS CSMS Lite - ${display} 실시간 [에러/예외] 필터 스트림' -ForegroundColor Red; Write-Host '  감시 대상: ${logFile} (ERROR | Exception | FATAL | 실패 필터링)' -ForegroundColor DarkGray; Write-Host '  (종료: 창 닫기 또는 Ctrl+C)' -ForegroundColor DarkGray; Write-Host '========================================================================================================' -ForegroundColor DarkRed; Write-Host ''; if (Test-Path '${logFile}') { Write-Host '[실시간 필터] ERROR/Exception 발생 이벤트를 대기 및 모니터링합니다...' -ForegroundColor DarkYellow; Get-Content -Path '${logFile}' -Tail 200 -Wait -Encoding utf8 | Where-Object { $_ -match 'ERROR|Exception|FATAL|경고|실패|RuntimeException|NullPointer' } } else { Write-Host '[안내] 원본 로그 파일 대기 중...' -ForegroundColor Yellow; while (-not (Test-Path '${logFile}')) { Start-Sleep -Seconds 2 }; Clear-Host; Write-Host '[연결됨] 에러 필터 감시를 시작합니다.' -ForegroundColor Red; Get-Content -Path '${logFile}' -Tail 200 -Wait -Encoding utf8 | Where-Object { $_ -match 'ERROR|Exception|FATAL|경고|실패|RuntimeException|NullPointer' } }`
              }
            } else {
              // 일반 전체 로그 모드
              psCmd = `[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; chcp 65001 >$null; $host.UI.RawUI.WindowTitle = '${winTitle}'; Clear-Host; Write-Host '========================================================================================================' -ForegroundColor DarkCyan; Write-Host '  ELVIS CSMS Lite - ${display} 실시간 콘솔 로그' -ForegroundColor Cyan; Write-Host '  로그 경로: ${logFile}' -ForegroundColor DarkGray; Write-Host '  (종료: 창 닫기 또는 Ctrl+C)' -ForegroundColor DarkGray; Write-Host '========================================================================================================' -ForegroundColor DarkCyan; Write-Host ''; if (Test-Path '${logFile}') { Get-Content -Path '${logFile}' -Tail 150 -Wait -Encoding utf8 } else { Write-Host '[안내] 아직 로그 파일이 없습니다. 서비스 기동 대기 중...' -ForegroundColor Yellow; while (-not (Test-Path '${logFile}')) { Start-Sleep -Seconds 2 }; Clear-Host; Write-Host '[연결됨] 로그 감시를 시작합니다.' -ForegroundColor Green; Get-Content -Path '${logFile}' -Tail 150 -Wait -Encoding utf8 }`
            }

            try {
              const child = spawn('cmd.exe', ['/c', 'start', winTitle, 'powershell', '-NoExit', '-Command', psCmd], {
                detached: true,
                stdio: 'ignore'
              })
              child.unref()
              res.end(JSON.stringify({ success: true, message: `${display} ${isErrorMode ? '에러' : '표준'} 터미널 콘솔이 실행되었습니다.` }))
            } catch (err: any) {
              res.statusCode = 500
              res.end(JSON.stringify({ success: false, message: '터미널 기동 실패: ' + err.message }))
            }
          })
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
          const batPath = path.resolve(scriptsDir, 'create-kafka-topics.bat')
          exec(`cmd.exe /c "${batPath}"`, {
            cwd: scriptsDir,
            env: { ...process.env, NO_PAUSE: '1' }
          }, (err, stdout, stderr) => {
            if (err) {
              res.statusCode = 500
              res.end(JSON.stringify({ success: false, message: '토픽 생성 실패: ' + (stderr || err.message) }))
              return
            }
            res.end(JSON.stringify({
              success: true,
              message: 'Kafka 4대 핵심 토픽(ocpp-raw-events, ocpp-outbound-commands, ocpp-ui-notifications, ocpp-raw-events.DLT)이 생성되었습니다.'
            }))
          })
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
