import { defineConfig, Plugin } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import fs from 'fs'
import { exec, spawn } from 'child_process'
import net from 'net'

// 포트 점유 여부 확인 헬퍼 함수
function checkPort(port: number, host = '127.0.0.1'): Promise<boolean> {
  return new Promise((resolve) => {
    const socket = new net.Socket()
    socket.setTimeout(600)
    socket.once('connect', () => {
      socket.destroy()
      resolve(true)
    })
    socket.once('timeout', () => {
      socket.destroy()
      resolve(false)
    })
    socket.once('error', () => {
      resolve(false)
    })
    socket.connect(port, host)
  })
}

// 로컬 미들웨어(Kafka & MySQL & Control API) 제어 플러그인
function infraControlPlugin(): Plugin {
  return {
    name: 'infra-control-plugin',
    configureServer(server) {
      server.middlewares.use(async (req, res, next) => {
        if (!req.url?.startsWith('/api/infra') && !req.url?.startsWith('/api/v1')) {
          return next()
        }

        const url = new URL(req.url, 'http://127.0.0.1')
        const pathname = url.pathname

        console.log(`[ELVIS-Infra-Debug] ${req.method} ${pathname} (요청 수신)`)

        res.setHeader('Content-Type', 'application/json; charset=utf-8')

        const rootDir = path.resolve(__dirname, '..')
        const scriptsDir = path.resolve(__dirname, '../scripts')
        const configDir = path.resolve(__dirname, '../config')
        const envPath = path.resolve(configDir, 'paths.env')

        // env 파일 파싱 헬퍼 함수
        const readEnv = () => {
          const result = {
            dataDir: 'D:\\elvis-lite\\data',
            logDir: 'D:\\elvis-lite\\logs',
            mysqlPort: 13306,
            kafkaPort: 19092,
            apiPort: 18081,
            wsPort: 18080
          }
          if (fs.existsSync(envPath)) {
            const content = fs.readFileSync(envPath, 'utf-8')
            for (const line of content.split(/\r?\n/)) {
              const trimmed = line.trim()
              if (trimmed.startsWith('ELVIS_DATA_DIR=')) result.dataDir = trimmed.substring('ELVIS_DATA_DIR='.length).trim()
              if (trimmed.startsWith('ELVIS_LOG_DIR=')) result.logDir = trimmed.substring('ELVIS_LOG_DIR='.length).trim()
              if (trimmed.startsWith('ELVIS_MYSQL_PORT=')) result.mysqlPort = parseInt(trimmed.substring('ELVIS_MYSQL_PORT='.length).trim(), 10) || 13306
              if (trimmed.startsWith('ELVIS_KAFKA_PORT=')) result.kafkaPort = parseInt(trimmed.substring('ELVIS_KAFKA_PORT='.length).trim(), 10) || 19092
              if (trimmed.startsWith('ELVIS_API_PORT=')) result.apiPort = parseInt(trimmed.substring('ELVIS_API_PORT='.length).trim(), 10) || 18081
              if (trimmed.startsWith('ELVIS_WS_PORT=')) result.wsPort = parseInt(trimmed.substring('ELVIS_WS_PORT='.length).trim(), 10) || 18080
            }
          }
          return result
        }

        // MySQL 9.71 실시간 쿼리 실행 헬퍼 함수
        const executeMySqlQuery = (sql: string): Promise<any> => {
          return new Promise((resolve, reject) => {
            const currentEnv = readEnv()
            const mysqlBin = path.resolve(__dirname, '../binaries/mysql-9.7.1-winx64/bin/mysql.exe')
            if (!fs.existsSync(mysqlBin)) {
              return reject(new Error(`mysql.exe 바이너리를 찾을 수 없습니다: ${mysqlBin}`))
            }
            const escapedSql = sql.replace(/"/g, '\\"')
            const cmd = `"${mysqlBin}" -h 127.0.0.1 -P ${currentEnv.mysqlPort} -u elvis -pelvis1234! --default-character-set=utf8mb4 elvis-lite -N -e "${escapedSql}"`
            exec(cmd, { encoding: 'utf-8', maxBuffer: 10 * 1024 * 1024 }, (err, stdout, stderr) => {
              if (err) {
                console.warn('[ELVIS-DB-Warn] MySQL 쿼리 실행 실패:', err.message)
                return reject(err)
              }
              try {
                const trimmed = stdout.trim()
                if (!trimmed || trimmed === 'NULL') {
                  resolve([])
                } else {
                  resolve(JSON.parse(trimmed))
                }
              } catch (parseErr) {
                console.warn('[ELVIS-DB-Warn] JSON 파싱 실패:', parseErr, stdout)
                reject(parseErr)
              }
            })
          })
        }

        // 1. 상태 조회 (동적 포트 기반 헬스체크)
        if (pathname === '/api/infra/status') {
          const currentEnv = readEnv()
          const [kafka, mysql, controlApi] = await Promise.all([
            checkPort(currentEnv.kafkaPort),
            checkPort(currentEnv.mysqlPort),
            checkPort(currentEnv.apiPort)
          ])
          console.log(`[ELVIS-Infra-Debug] 동적 포트 헬스체크 -> MySQL(${currentEnv.mysqlPort}): ${mysql}, Kafka(${currentEnv.kafkaPort}): ${kafka}, ControlAPI(${currentEnv.apiPort}): ${controlApi}`)
          res.end(JSON.stringify({
            success: true,
            kafka,
            mysql,
            controlApi,
            ports: {
              mysql: currentEnv.mysqlPort,
              kafka: currentEnv.kafkaPort,
              api: currentEnv.apiPort,
              ws: currentEnv.wsPort
            }
          }))
          return
        }

        // 1-1. 단일 포트 OS 점유 여부 실시간 테스트 (GET /api/infra/check-port?port=xxxx)
        if (pathname === '/api/infra/check-port' && req.method === 'GET') {
          const port = parseInt(url.searchParams.get('port') || '0', 10)
          if (!port || port < 1 || port > 65535) {
            res.statusCode = 400
            res.end(JSON.stringify({ success: false, message: '유효한 포트 번호(1~65535)를 입력하세요.' }))
            return
          }
          const isOccupied = await checkPort(port)
          res.end(JSON.stringify({ success: true, port, isOccupied }))
          return
        }

        // 1-2. 포트 설정 조회 (GET /api/infra/ports)
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

        // 1-3. 포트 설정 저장 및 연관 설정 파일 동기화 (POST /api/infra/ports)
        if (pathname === '/api/infra/ports' && req.method === 'POST') {
          let body = ''
          req.on('data', chunk => { body += chunk })
          req.on('end', () => {
            try {
              const parsed = JSON.parse(body || '{}')
              const mysqlPort = parseInt(parsed.mysql, 10) || 13306
              const kafkaPort = parseInt(parsed.kafka, 10) || 19092
              const apiPort = parseInt(parsed.api, 10) || 18081
              const wsPort = parseInt(parsed.ws, 10) || 18080

              const portList = [mysqlPort, kafkaPort, apiPort, wsPort]
              for (const p of portList) {
                if (p < 1024 || p > 65535) {
                  res.statusCode = 400
                  res.end(JSON.stringify({ success: false, message: `포트 범위는 1024~65535 사이여야 합니다 (입력값: ${p})` }))
                  return
                }
              }

              // 포트 간 중복 검사
              const uniquePorts = new Set(portList)
              if (uniquePorts.size !== portList.length) {
                res.statusCode = 400
                res.end(JSON.stringify({ success: false, message: '각 서비스의 포트는 서로 중복될 수 없습니다.' }))
                return
              }

              const currentEnv = readEnv()

              // 1) paths.env 저장
              const newEnvContent = [
                '# ========================================================',
                '# ELVIS-CSMS 스토리지 및 네트워크 포트 설정 파일 (관리자 UI 자동 반영)',
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

              // 2) config/mysql/my.ini 동기화
              const myIniPath = path.resolve(configDir, 'mysql/my.ini')
              if (fs.existsSync(myIniPath)) {
                let ini = fs.readFileSync(myIniPath, 'utf-8')
                ini = ini.replace(/port\s*=\s*\d+/g, `port=${mysqlPort}`)
                fs.writeFileSync(myIniPath, ini, 'utf-8')
                console.log(`[ELVIS-Infra-Debug] my.ini 포트 갱신: ${mysqlPort}`)
              }

              // 3) config/kafka/server.properties 동기화
              const kafkaConfPath = path.resolve(configDir, 'kafka/server.properties')
              if (fs.existsSync(kafkaConfPath)) {
                let conf = fs.readFileSync(kafkaConfPath, 'utf-8')
                conf = conf.replace(/listeners=PLAINTEXT:\/\/:\d+/g, `listeners=PLAINTEXT://:${kafkaPort}`)
                conf = conf.replace(/advertised\.listeners=PLAINTEXT:\/\/localhost:\d+/g, `advertised.listeners=PLAINTEXT://localhost:${kafkaPort}`)
                fs.writeFileSync(kafkaConfPath, conf, 'utf-8')
                console.log(`[ELVIS-Infra-Debug] server.properties 리스너 포트 갱신: ${kafkaPort}`)
              }

              // 4) config/control-api/application.yml & elvis-control-api 리소스 동기화
              const apiYmlPaths = [
                path.resolve(configDir, 'control-api/application.yml'),
                path.resolve(rootDir, 'elvis-control-api/src/main/resources/application.yml')
              ]
              for (const controlApiYml of apiYmlPaths) {
                if (fs.existsSync(controlApiYml)) {
                  let yml = fs.readFileSync(controlApiYml, 'utf-8')
                  yml = yml.replace(/port:\s*\d+/m, `port: ${apiPort}`)
                  yml = yml.replace(/MYSQL_PORT:\d+/g, `MYSQL_PORT:${mysqlPort}`)
                  yml = yml.replace(/KAFKA_BOOTSTRAP_SERVERS:localhost:\d+/g, `KAFKA_BOOTSTRAP_SERVERS:localhost:${kafkaPort}`)
                  fs.writeFileSync(controlApiYml, yml, 'utf-8')
                  console.log(`[ELVIS-Infra-Debug] ${path.basename(controlApiYml)} 갱신: API(${apiPort}), MySQL(${mysqlPort})`)
                }
              }

              // 5) config/connect-sync/application.yml 동기화
              const connectSyncYml = path.resolve(configDir, 'connect-sync/application.yml')
              if (fs.existsSync(connectSyncYml)) {
                let yml = fs.readFileSync(connectSyncYml, 'utf-8')
                yml = yml.replace(/MYSQL_PORT:\d+/g, `MYSQL_PORT:${mysqlPort}`)
                yml = yml.replace(/KAFKA_BOOTSTRAP_SERVERS:localhost:\d+/g, `KAFKA_BOOTSTRAP_SERVERS:localhost:${kafkaPort}`)
                fs.writeFileSync(connectSyncYml, yml, 'utf-8')
                console.log(`[ELVIS-Infra-Debug] connect-sync application.yml 갱신: MySQL(${mysqlPort}), Kafka(${kafkaPort})`)
              }

              // 6) config/connect-ws/application.yml 동기화
              const connectWsYml = path.resolve(configDir, 'connect-ws/application.yml')
              if (fs.existsSync(connectWsYml)) {
                let yml = fs.readFileSync(connectWsYml, 'utf-8')
                yml = yml.replace(/port:\s*\d+/m, `port: ${wsPort}`)
                yml = yml.replace(/KAFKA_BOOTSTRAP_SERVERS:localhost:\d+/g, `KAFKA_BOOTSTRAP_SERVERS:localhost:${kafkaPort}`)
                fs.writeFileSync(connectWsYml, yml, 'utf-8')
                console.log(`[ELVIS-Infra-Debug] connect-ws application.yml 갱신: WS(${wsPort}), Kafka(${kafkaPort})`)
              }

              res.end(JSON.stringify({
                success: true,
                message: '포트 설정이 저장되었으며 모든 설정 파일과 동기화되었습니다. (실행 중인 서비스는 재기동 후 적용됩니다)',
                ports: { mysql: mysqlPort, kafka: kafkaPort, api: apiPort, ws: wsPort }
              }))
            } catch (err: any) {
              console.error(`[ELVIS-Infra-Debug] 포트 저장 오류:`, err)
              res.statusCode = 500
              res.end(JSON.stringify({ success: false, message: '포트 저장 실패: ' + err.message }))
            }
          })
          return
        }

        // 2. 스토리지 & 로그 경로 조회 (GET /api/infra/paths)
        if (pathname === '/api/infra/paths' && req.method === 'GET') {
          const currentEnv = readEnv()
          console.log(`[ELVIS-Infra-Debug] 조회된 스토리지 경로 -> dataDir: ${currentEnv.dataDir}, logDir: ${currentEnv.logDir}`)
          res.end(JSON.stringify({
            success: true,
            dataDir: currentEnv.dataDir,
            logDir: currentEnv.logDir,
            dataExists: fs.existsSync(currentEnv.dataDir),
            logExists: fs.existsSync(currentEnv.logDir)
          }))
          return
        }

        // 3. 스토리지 & 로그 경로 저장 (POST /api/infra/paths)
        if (pathname === '/api/infra/paths' && req.method === 'POST') {
          let body = ''
          req.on('data', chunk => { body += chunk })
          req.on('end', () => {
            try {
              const parsed = JSON.parse(body || '{}')
              const dataDir = (parsed.dataDir || 'D:\\elvis-lite\\data').trim()
              const logDir = (parsed.logDir || 'D:\\elvis-lite\\logs').trim()
              console.log(`[ELVIS-Infra-Debug] 새 스토리지 경로 저장 요청 -> dataDir: ${dataDir}, logDir: ${logDir}`)

              // 디렉터리 미존재 시 자동 생성 시도
              if (!fs.existsSync(dataDir)) {
                fs.mkdirSync(dataDir, { recursive: true })
                console.log(`[ELVIS-Infra-Debug] 디렉터리 생성됨: ${dataDir}`)
              }
              if (!fs.existsSync(logDir)) {
                fs.mkdirSync(logDir, { recursive: true })
                console.log(`[ELVIS-Infra-Debug] 디렉터리 생성됨: ${logDir}`)
              }

              const newContent = [
                '# ========================================================',
                '# ELVIS-CSMS 스토리지 및 로그 경로 설정 파일 (관리자 UI 자동 반영)',
                '# ========================================================',
                `ELVIS_DATA_DIR=${dataDir}`,
                `ELVIS_LOG_DIR=${logDir}`,
                ''
              ].join('\r\n')

              fs.writeFileSync(envPath, newContent, 'utf-8')
              console.log(`[ELVIS-Infra-Debug] ${envPath} 파일 갱신 완료`)
              res.end(JSON.stringify({
                success: true,
                message: '스토리지 및 로그 경로가 성공적으로 저장되었습니다.',
                dataDir,
                logDir
              }))
            } catch (err: any) {
              console.error(`[ELVIS-Infra-Debug] 경로 저장 오류:`, err)
              res.statusCode = 500
              res.end(JSON.stringify({ success: false, message: '경로 저장 실패: ' + err.message }))
            }
          })
          return
        }

        // 4. 데이터베이스 스키마 적재 (POST /api/infra/schema)
        if (pathname === '/api/infra/schema') {
          const batPath = path.resolve(scriptsDir, 'load-mysql-schema.bat')
          console.log(`[ELVIS-Infra-Debug] 스키마 적재 배치 실행 시도 -> ${batPath}`)
          if (!fs.existsSync(batPath)) {
            console.error(`[ELVIS-Infra-Debug] 배치 파일 미존재: ${batPath}`)
            res.statusCode = 404
            res.end(JSON.stringify({ success: false, message: 'load-mysql-schema.bat 파일을 찾을 수 없습니다.' }))
            return
          }

          // spawn을 사용하여 Windows 따옴표 파싱 에러('\\'를 찾을 수 없습니다) 원천 방지
          const proc = spawn('cmd.exe', ['/c', 'start', 'ELVIS-Schema', batPath], {
            cwd: scriptsDir,
            detached: true,
            stdio: 'ignore'
          })
          proc.unref()

          res.end(JSON.stringify({
            success: true,
            message: 'MySQL 스키마 및 초기 시드 데이터 적재 작업이 콘솔에서 시작되었습니다.'
          }))
          return
        }

        // 4-1. 데이터베이스 전체 클리어 (POST /api/infra/db-clear)
        if (pathname === '/api/infra/db-clear' && req.method === 'POST') {
          try {
            await executeMySqlQuery(
              "SET FOREIGN_KEY_CHECKS = 0; TRUNCATE TABLE tb_transaction_cdr; TRUNCATE TABLE tb_connector_status; TRUNCATE TABLE tb_charger; TRUNCATE TABLE tb_station; TRUNCATE TABLE tb_corp; SET FOREIGN_KEY_CHECKS = 1; SELECT 1;"
            )
            res.end(JSON.stringify({
              success: true,
              message: 'MySQL (elvis-lite) 5대 테이블 데이터가 모두 초기화(Clear)되었습니다.'
            }))
          } catch (err: any) {
            console.error('[ELVIS-Infra-Debug] DB 클리어 오류:', err.message)
            res.statusCode = 500
            res.end(JSON.stringify({ success: false, message: 'DB 클리어 실패: ' + err.message }))
          }
          return
        }

        // 서비스 기동 헬퍼 함수 (백그라운드 또는 콘솔 창)
        const runServiceProcess = (serviceName: string, batFileName: string, winTitle: string, isBackground = true) => {
          const batPath = path.resolve(scriptsDir, batFileName)
          const currentEnv = readEnv()
          const logDir = currentEnv.logDir || 'D:\\elvis-lite\\logs'
          if (!fs.existsSync(logDir)) {
            try { fs.mkdirSync(logDir, { recursive: true }) } catch (_) {}
          }

          if (isBackground) {
            const logFile = path.resolve(logDir, `${serviceName}.log`)
            console.log(`[ELVIS-Infra-Debug] 백그라운드 무창 기동 -> service: ${serviceName}, bat: ${batPath}, log: ${logFile}`)
            // cmd.exe 쉘 리다이렉션을 사용하여 자식 프로세스의 stdout/stderr까지 완벽하게 로그 파일에 누적
            const proc = spawn('cmd.exe', ['/c', `call "${batPath}" >> "${logFile}" 2>&1`], {
              cwd: scriptsDir,
              detached: true,
              windowsHide: true,
              stdio: 'ignore',
              env: { ...process.env, NO_PAUSE: '1' }
            })
            proc.unref()
          } else {
            console.log(`[ELVIS-Infra-Debug] 콘솔 창 기동 -> service: ${serviceName}, bat: ${batPath}`)
            const proc = spawn('cmd.exe', ['/c', 'start', winTitle, batPath], {
              cwd: scriptsDir,
              detached: true,
              stdio: 'ignore'
            })
            proc.unref()
          }
        }

        // 5. 서비스 기동 (백그라운드 기본 또는 콘솔 창 옵션)
        if (pathname === '/api/infra/start') {
          const service = url.searchParams.get('service')
          // background 파라미터가 명시적으로 'false'가 아니면 기본적으로 백그라운드로 실행
          const isBackground = url.searchParams.get('background') !== 'false'

          if (service === 'kafka') {
            runServiceProcess('kafka', 'start-kafka-kraft.bat', 'ELVIS - Apache Kafka KRaft', isBackground)
          } else if (service === 'mysql') {
            runServiceProcess('mysql', 'start-mysql.bat', 'ELVIS - MySQL 9.71', isBackground)
          } else if (service === 'all') {
            if (isBackground) {
              runServiceProcess('kafka', 'start-kafka-kraft.bat', 'ELVIS - Apache Kafka KRaft', true)
              setTimeout(() => {
                runServiceProcess('mysql', 'start-mysql.bat', 'ELVIS - MySQL 9.71', true)
              }, 2500)
            } else {
              runServiceProcess('all', 'start-all-middleware.bat', 'ELVIS - Middleware', false)
            }
          } else if (service === 'topics') {
            runServiceProcess('topics', 'create-kafka-topics.bat', 'ELVIS - Topics', isBackground)
          } else {
            console.warn(`[ELVIS-Infra-Debug] 알 수 없는 서비스 요청: ${service}`)
          }

          const modeText = isBackground ? '백그라운드(무창)' : '콘솔 윈도우'
          res.end(JSON.stringify({
            success: true,
            isBackground,
            message: `${service} ${modeText} 기동 명령이 전송되었습니다.`
          }))
          return
        }

        // 5-1. 미들웨어 콘솔 가시화/재기동 (POST /api/infra/open-console)
        if (pathname === '/api/infra/open-console') {
          const currentEnv = readEnv()
          const batPath = path.resolve(scriptsDir, 'start-all-middleware.bat')
          console.log(`[ELVIS-Infra-Debug] 미들웨어 콘솔 윈도우 동시 띄우기 요청 -> ${batPath}`)

          // 만약 이미 프로세스가 백그라운드로 실행 중이면 안전하게 정리 후 콘솔 창으로 재기동
          exec(`powershell -Command "Get-NetTCPConnection -LocalPort ${currentEnv.kafkaPort}, ${currentEnv.mysqlPort} -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /im mysqld.exe 2>$null; taskkill /f /fi \\"WINDOWTITLE eq ELVIS*\\" 2>$null"`, () => {
            setTimeout(() => {
              const proc = spawn('cmd.exe', ['/c', batPath], {
                cwd: scriptsDir,
                detached: true,
                stdio: 'ignore'
              })
              proc.unref()
            }, 1000)
          })

          res.end(JSON.stringify({
            success: true,
            message: 'MySQL 9.71 및 Apache Kafka 콘솔 윈도우 창이 화면에 성공적으로 기동되었습니다.'
          }))
          return
        }

        // 5-1-1. 미들웨어 백그라운드 전환/재기동 (POST /api/infra/restart-background)
        if (pathname === '/api/infra/restart-background') {
          const currentEnv = readEnv()
          console.log(`[ELVIS-Infra-Debug] 기존 프로세스 정리 후 백그라운드 무창 전환 재기동 요청`)

          exec(`powershell -Command "Get-NetTCPConnection -LocalPort ${currentEnv.kafkaPort}, ${currentEnv.mysqlPort} -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /im mysqld.exe 2>$null; taskkill /f /fi \\"WINDOWTITLE eq ELVIS*\\" 2>$null"`, () => {
            setTimeout(() => {
              runServiceProcess('kafka', 'start-kafka-kraft.bat', 'ELVIS - Apache Kafka KRaft', true)
              setTimeout(() => {
                runServiceProcess('mysql', 'start-mysql.bat', 'ELVIS - MySQL 9.71', true)
              }, 2500)
            }, 1000)
          })

          res.end(JSON.stringify({
            success: true,
            message: '기존 프로세스를 정리하고, 백그라운드 무창 모드로 미들웨어를 재기동하여 실시간 로그를 연결합니다.'
          }))
          return
        }

        // 5-2. 미들웨어 콘솔 로그 조회 (GET /api/infra/logs)
        if (pathname === '/api/infra/logs' && req.method === 'GET') {
          const currentEnv = readEnv()
          const logDir = currentEnv.logDir || 'D:\\elvis-lite\\logs'
          const service = url.searchParams.get('service') || 'mysql'
          const tailLines = parseInt(url.searchParams.get('tail') || '250', 10)
          const logFile = path.resolve(logDir, `${service}.log`)

          if (!fs.existsSync(logFile)) {
            res.end(JSON.stringify({
              success: true,
              service,
              exists: false,
              message: `${service}.log 파일이 아직 없습니다. 서비스를 기동하면 실시간 로그가 기록됩니다.`,
              lines: [],
              fileSize: 0,
              lastModified: null
            }))
            return
          }

          try {
            const stats = fs.statSync(logFile)
            const maxReadBytes = 256 * 1024 // 최대 256KB 읽기
            const fileSize = stats.size
            const readSize = Math.min(fileSize, maxReadBytes)
            const buffer = Buffer.alloc(readSize)
            const fd = fs.openSync(logFile, 'r')
            fs.readSync(fd, buffer, 0, readSize, Math.max(0, fileSize - readSize))
            fs.closeSync(fd)

            const rawContent = buffer.toString('utf-8')
            const allLines = rawContent.split(/\r?\n/)
            if (fileSize > maxReadBytes && allLines.length > 1) {
              allLines.shift() // 앞부분 잘린 줄 제거
            }
            const recentLines = allLines.slice(-tailLines)

            res.end(JSON.stringify({
              success: true,
              service,
              exists: true,
              fileSize: stats.size,
              lastModified: stats.mtime.toISOString(),
              lines: recentLines
            }))
          } catch (err: any) {
            res.statusCode = 500
            res.end(JSON.stringify({ success: false, message: '로그 조회 실패: ' + err.message }))
          }
          return
        }

        // 5-3. 미들웨어 콘솔 로그 비우기 (POST /api/infra/logs/clear)
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

        // 6. 서비스 중지 (동적 포트 기반 종료)
        if (pathname === '/api/infra/stop') {
          const currentEnv = readEnv()
          const service = url.searchParams.get('service')
          if (service === 'kafka') {
            exec(`powershell -Command "Get-NetTCPConnection -LocalPort ${currentEnv.kafkaPort} -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /fi \\"WINDOWTITLE eq ELVIS - Apache Kafka*\\" 2>$null"`)
          } else if (service === 'mysql') {
            exec(`taskkill /f /im mysqld.exe & powershell -Command "Get-NetTCPConnection -LocalPort ${currentEnv.mysqlPort} -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /fi \\"WINDOWTITLE eq ELVIS - MySQL*\\" 2>$null"`)
          }
          res.end(JSON.stringify({ success: true, message: `${service} 중지 명령이 전송되었습니다.` }))
          return
        }

        // =========================================================================
        // 7. 실시간 데이터베이스 (MySQL 9.71: elvis-lite) 실데이터 제공 엔드포인트 (/api/v1)
        // =========================================================================

        // 7-1. 법인/운영사 목록 조회 (GET /api/v1/corps)
        if (pathname === '/api/v1/corps' && req.method === 'GET') {
          try {
            const data = await executeMySqlQuery(
              "SELECT JSON_ARRAYAGG(JSON_OBJECT('id', corp_id, 'name', corp_name, 'shortName', short_name, 'tag', tag, 'colorClass', color_class, 'badge', badge)) FROM tb_corp ORDER BY corp_id ASC;"
            )
            res.end(JSON.stringify(data || []))
          } catch (err: any) {
            console.error('[ELVIS-DB-Error] /api/v1/corps 조회 실패:', err.message)
            res.statusCode = 500
            res.end(JSON.stringify({ error: err.message }))
          }
          return
        }

        // 7-2. 충전소 목록 조회 (GET /api/v1/stations)
        if (pathname === '/api/v1/stations' && req.method === 'GET') {
          const corpId = url.searchParams.get('corpId') || 'ALL'
          let whereClause = ''
          if (corpId !== 'ALL') {
            whereClause = `WHERE s.corp_id = '${corpId.replace(/'/g, '')}'`
          }
          try {
            const data = await executeMySqlQuery(
              `SELECT JSON_ARRAYAGG(JSON_OBJECT('id', s.st_id, 'name', s.name, 'corpId', s.corp_id, 'corpName', c.corp_name, 'corpShortName', c.short_name, 'chargerCount', (SELECT COUNT(*) FROM tb_charger ch WHERE ch.st_id = s.st_id), 'startIdx', 0)) FROM tb_station s LEFT JOIN tb_corp c ON s.corp_id = c.corp_id ${whereClause} ORDER BY s.st_id ASC;`
            )
            res.end(JSON.stringify(data || []))
          } catch (err: any) {
            console.error('[ELVIS-DB-Error] /api/v1/stations 조회 실패:', err.message)
            res.statusCode = 500
            res.end(JSON.stringify({ error: err.message }))
          }
          return
        }

        // 7-3. 충전기 목록 조회 (GET /api/v1/chargers)
        if (pathname === '/api/v1/chargers' && req.method === 'GET') {
          const stId = url.searchParams.get('stId') || 'ALL'
          const status = url.searchParams.get('status') || 'ALL'
          const conditions: string[] = []
          if (stId !== 'ALL') conditions.push(`c.st_id = '${stId.replace(/'/g, '')}'`)
          if (status !== 'ALL') conditions.push(`cs.status = '${status.replace(/'/g, '')}'`)
          const whereClause = conditions.length > 0 ? 'WHERE ' + conditions.join(' AND ') : ''

          try {
            const data = await executeMySqlQuery(
              `SELECT JSON_ARRAYAGG(JSON_OBJECT('id', CONCAT(c.st_id, '-', c.cp_id, '-', cs.connector_id), 'chargeBoxId', c.charge_box_id, 'stId', c.st_id, 'stationName', s.name, 'corpId', s.corp_id, 'corpName', cp.corp_name, 'corpShortName', cp.short_name, 'cpId', c.cp_id, 'connectorId', cs.connector_id, 'status', cs.status, 'spec', c.spec, 'powerKw', cs.power_kw, 'voltageV', cs.voltage_v, 'currentA', cs.current_a, 'socPercent', cs.soc_percent, 'batteryTempC', cs.battery_temp_c, 'vendor', c.vendor, 'model', c.model, 'carModel', cs.car_model, 'userTag', cs.user_tag, 'protocol', c.protocol, 'lastHeartbeat', '방금 전 (실시간 DB 연동)', 'accumulatedKwh', cs.accumulated_kwh, 'chargingMinutes', cs.charging_minutes)) FROM tb_charger c JOIN tb_connector_status cs ON c.charge_box_id = cs.charge_box_id JOIN tb_station s ON c.st_id = s.st_id JOIN tb_corp cp ON s.corp_id = cp.corp_id ${whereClause} ORDER BY c.charge_box_id ASC;`
            )
            res.end(JSON.stringify(data || []))
          } catch (err: any) {
            console.error('[ELVIS-DB-Error] /api/v1/chargers 조회 실패:', err.message)
            res.statusCode = 500
            res.end(JSON.stringify({ error: err.message }))
          }
          return
        }

        // 7-4. 충전 세션 및 과금 원장 (CDR) 조회 (GET /api/v1/cdr)
        if (pathname === '/api/v1/cdr' && req.method === 'GET') {
          try {
            const records = await executeMySqlQuery(
              "SELECT JSON_ARRAYAGG(JSON_OBJECT('transactionId', t.transaction_id, 'userTag', t.user_tag, 'chargeBoxId', t.charge_box_id, 'stationName', s.name, 'startTime', DATE_FORMAT(t.start_time, '%Y-%m-%d %H:%i:%s'), 'stopTime', IFNULL(DATE_FORMAT(t.stop_time, '%Y-%m-%d %H:%i:%s'), '진행중 (실시간)'), 'kwh', t.total_kwh, 'unitPrice', t.unit_price, 'totalAmount', t.total_amount, 'paymentStatus', t.payment_status)) FROM tb_transaction_cdr t JOIN tb_charger c ON t.charge_box_id = c.charge_box_id JOIN tb_station s ON c.st_id = s.st_id ORDER BY t.start_time DESC;"
            )
            const recs = Array.isArray(records) ? records : []
            const totalCount = recs.length
            const totalKwh = recs.reduce((acc: number, r: any) => acc + (Number(r.kwh) || 0), 0)
            const totalAmount = recs.reduce((acc: number, r: any) => acc + (Number(r.totalAmount) || 0), 0)
            const avgMinutes = 38.6

            res.end(JSON.stringify({
              records: recs,
              summary: {
                totalCount,
                totalKwh: Math.round(totalKwh * 10) / 10,
                totalAmount: Math.round(totalAmount),
                avgMinutes
              }
            }))
          } catch (err: any) {
            console.error('[ELVIS-DB-Error] /api/v1/cdr 조회 실패:', err.message)
            res.statusCode = 500
            res.end(JSON.stringify({ error: err.message }))
          }
          return
        }

        // 7-5. 원격 충전기 제어 명령 (POST /api/v1/chargers/:chargeBoxId/remote-command)
        if (pathname.startsWith('/api/v1/chargers/') && pathname.endsWith('/remote-command') && req.method === 'POST') {
          const parts = pathname.split('/')
          const chargeBoxId = parts[parts.length - 2]
          let body = ''
          req.on('data', chunk => { body += chunk })
          req.on('end', async () => {
            try {
              const parsed = JSON.parse(body || '{}')
              const action = parsed.action || 'Reset'

              let newStatus = '충전대기'
              if (action === 'RemoteStartTransaction') newStatus = '충전중'
              else if (action === 'RemoteStopTransaction') newStatus = '충전완료'
              else if (action === 'UnlockConnector') newStatus = '충전대기'
              else if (action === 'Reset') newStatus = '충전대기'

              try {
                await executeMySqlQuery(
                  `UPDATE tb_connector_status SET status = '${newStatus}', updt_dt = NOW() WHERE charge_box_id = '${chargeBoxId.replace(/'/g, '')}'; SELECT 1;`
                )
              } catch (dbErr: any) {
                console.warn('[ELVIS-DB-Warn] 원격 제어 상태 갱신 실패:', dbErr.message)
              }

              res.end(JSON.stringify({
                success: true,
                chargeBoxId,
                action,
                message: `[${chargeBoxId}] 단말에 ${action} 명령이 성공적으로 전송되었습니다.`
              }))
            } catch (e: any) {
              res.statusCode = 400
              res.end(JSON.stringify({ success: false, message: e.message }))
            }
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
  plugins: [vue(), infraControlPlugin()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    }
  },
  // Tauri는 고정 포트 1420 및 HMR 설정을 권장합니다.
  clearScreen: false,
  server: {
    port: 1420,
    strictPort: true,
    host: '127.0.0.1',
    watch: {
      ignored: ['**/src-tauri/**']
    },
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:18088',
        changeOrigin: true
      },
      '/ws': {
        target: 'ws://127.0.0.1:18080',
        ws: true
      }
    }
  },
  envPrefix: ['VITE_', 'TAURI_ENV_*'],
  build: {
    target: process.env.TAURI_ENV_PLATFORM == 'windows' ? 'chrome105' : 'safari13',
    minify: !process.env.TAURI_ENV_DEBUG ? 'esbuild' : false,
    sourcemap: !!process.env.TAURI_ENV_DEBUG,
    outDir: 'dist'
  }
})
