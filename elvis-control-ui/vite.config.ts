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
        if (!req.url?.startsWith('/api/infra')) {
          return next()
        }

        const url = new URL(req.url, 'http://127.0.0.1')
        const pathname = url.pathname

        console.log(`[ELVIS-Infra-Debug] ${req.method} ${pathname} (요청 수신)`)

        res.setHeader('Content-Type', 'application/json; charset=utf-8')

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

        // 5. 서비스 기동 (spawn을 사용하여 셸 따옴표 파싱 에러 '\\' 원천 차단)
        if (pathname === '/api/infra/start') {
          const service = url.searchParams.get('service')
          let batFile = ''
          let windowTitle = 'ELVIS'

          if (service === 'kafka') {
            batFile = 'start-kafka-kraft.bat'
            windowTitle = 'ELVIS-Kafka'
          } else if (service === 'mysql') {
            batFile = 'start-mysql.bat'
            windowTitle = 'ELVIS-MySQL'
          } else if (service === 'all') {
            batFile = 'start-all-middleware.bat'
            windowTitle = 'ELVIS-All'
          } else if (service === 'topics') {
            batFile = 'create-kafka-topics.bat'
            windowTitle = 'ELVIS-Topics'
          }

          if (batFile) {
            const batPath = path.resolve(scriptsDir, batFile)
            console.log(`[ELVIS-Infra-Debug] 서비스 기동 실행 -> service: ${service}, batPath: ${batPath}, cwd: ${scriptsDir}`)
            const proc = spawn('cmd.exe', ['/c', 'start', windowTitle, batPath], {
              cwd: scriptsDir,
              detached: true,
              stdio: 'ignore'
            })
            proc.unref()
          } else {
            console.warn(`[ELVIS-Infra-Debug] 알 수 없는 서비스 요청: ${service}`)
          }

          res.end(JSON.stringify({ success: true, message: `${service} 시작 명령이 전송되었습니다.` }))
          return
        }

        // 6. 서비스 중지
        if (pathname === '/api/infra/stop') {
          const service = url.searchParams.get('service')
          if (service === 'kafka') {
            exec(`powershell -Command "Get-NetTCPConnection -LocalPort 9092 -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /fi \\"WINDOWTITLE eq ELVIS - Apache Kafka*\\" 2>$null"`)
          } else if (service === 'mysql') {
            exec(`taskkill /f /im mysqld.exe & powershell -Command "Get-NetTCPConnection -LocalPort 3306 -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; taskkill /f /fi \\"WINDOWTITLE eq ELVIS - MySQL*\\" 2>$null"`)
          }
          res.end(JSON.stringify({ success: true, message: `${service} 중지 명령이 전송되었습니다.` }))
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
        target: 'http://127.0.0.1:8081',
        changeOrigin: true
      },
      '/ws': {
        target: 'ws://127.0.0.1:8080',
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
