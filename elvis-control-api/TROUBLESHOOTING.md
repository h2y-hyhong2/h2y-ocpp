# elvis-control-api 오류 해결 이력 (TROUBLESHOOTING)

> 관제 REST API 서버(`:9082`)의 빌드·기동·JWT 보안·DB/Kafka 연동 오류 및 해결 이력을 관리합니다.

## 기록 규칙
- **기록 대상**: 재발 가능성이 있는 Spring Security/JWT, JPA/QueryDSL, DB 접속, Kafka 연동 오류
- **필수 항목**: 발생 일시, 증상/에러 메시지, 근본 원인, 해결 방법, 관련 파일

---

## 오류 이력

### [TEMPLATE] 오류 제목
- **발생 일시**: `YYYY-MM-DD`
- **증상 / 에러 메시지**:
  ```text
  여기에 실제 콘솔/로그 에러 메시지를 기록합니다.
  ```
- **근본 원인**: 원인 분석 내용
- **해결 방법**: 적용한 조치 및 재발 방지 방안
- **관련 파일**: `elvis-control-api/...`
