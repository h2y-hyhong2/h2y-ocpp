---
description: [h2y-ocpp 전용] 작업 완료 시 Git 커밋 메시지 표준 규칙
---

# 📦 h2y-ocpp Git 커밋 메시지 표준 규칙

`h2y-ocpp` (CSMS Lite / 과제 3368) 저장소에서 작업이 완료되면 아래 **표준 커밋 메시지 형식**으로 커밋을 수행합니다.

## 1. 커밋 메시지 표준 포맷
```text
[과제ID]WBS-ID 세부작업명
```

## 2. 세부 작성 규칙
1. **과제 ID**: 대괄호 안에 과제 식별자 표기 (기본값: `[3368]`)
2. **WBS 작업 ID**: 대괄호 닫는 기호(`]`) 직후 공백 없이 공식 WBS ID 명시 (예: `WBS-COMMON-PP-001`, `WBS-CONNECT-WS-102-CO` 등)
3. **세부 작업명**: WBS ID 뒤에 반드시 한 칸 공백을 두고 공정/WBS 대장에 등록된 공식 세부작업명을 그대로 기재

## 3. 표준 예시
- `[3368]WBS-COMMON-PP-001 백엔드 런타임 및 Gradle 멀티모듈 스캐폴딩`
- `[3368]WBS-CONNECT-WS-102-CO WS Handshake & Auth 개발`
- `[3368]WBS-CONNECT-WS-103-CO 세션 맵 & 헬스체크 개발`
- `[3368]WBS-CONNECT-WS-104-CO Kafka 인바운드 Producer 개발`
- `[3368]WBS-CONNECT-WS-105-CO 하향 Listener & WS 중계 개발`
- `[3368]WBS-CONTROL-PT-101-PT 데스크톱 메인 프레임 & 반응형 레이아웃 프로토타입 다중 샘플 개발`
