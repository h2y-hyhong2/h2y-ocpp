# ELVIS-CSMS 설정 파일 관리 및 인스톨러 배포 가이드

본 디렉토리(`config/`)는 운영 환경 및 인스톨러 배포 시 외부에서 변경/커스텀할 수 있는 모든 소프트웨어 및 미들웨어의 설정 파일을 통합 관리합니다.

---

## 1. 디렉터리 구조

```text
config/
├── mysql/
│   └── my.ini                 # MySQL 9.71 인코딩(utf8mb4), 타임존(KST), 커넥션 풀(500) 설정
├── kafka/
│   └── server.properties      # Apache Kafka 4.3.1 KRaft 단독 모드 브로커/리스너 설정
├── connect-ws/
│   └── application.yml        # WebSocket Gateway 포트(8080), Kafka 토픽, 세션 타임아웃
├── connect-sync/
│   └── application.yml        # Parser & Sync Engine DB/ClickHouse 연결 및 컨슈머 설정
├── control-api/
│   └── application.yml        # Admin REST API 포트(8081), JWT 시크릿, DB 접속 설정
└── README.md                  # 본 가이드 문서
```

---

## 2. 모듈별 실행 시 설정 파일 적용 방법

### 2.1 MySQL 9.71
```cmd
mysqld.exe --defaults-file=config/mysql/my.ini --console
```

### 2.2 Apache Kafka 4.3.1 (KRaft)
```cmd
kafka-server-start.bat config/kafka/server.properties
```

### 2.3 Spring Boot 마이크로서비스 (`elvis-connect-ws` 등)
배포 시 외부에 노출된 `config/` 디렉터리의 설정을 최우선으로 로딩하도록 실행합니다:
```cmd
java -jar elvis-connect-ws-1.0.0.jar --spring.config.additional-location=file:./config/connect-ws/application.yml
```

---

## 3. Sprint 47 단일 인스톨러(Installer) 패키징 반영 계획

- **Linux (`Makeself`) & Windows (`Inno Setup`) 패키징 시**:
  - `config/` 디렉터리 전체를 설치 타겟 디렉터리 루트(`C:\elvis-csms\config\` 또는 `/opt/elvis-csms/config/`)로 복사 배치합니다.
  - 설치 마법사(또는 대화형 쉘)를 통해 포트 번호, DB 패스워드, 외부 IP 등을 입력받으면 해당 `config/` 파일들을 자동으로 치환합니다.
  - 서비스 기동 스크립트가 해당 `config/` 디렉터리의 설정 파일을 최우선 참조하여 기동됩니다.
