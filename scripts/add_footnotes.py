#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
CPO CSMS Lite 문서 약어 주석 자동화 스크립트 (add_footnotes.py)

이 스크립트는 프로젝트 내 마크다운(.md) 및 HTML(.html) 문서를 스캔하여
정의된 주요 약어들이 본문 내에 처음 등장하는 위치에 자동으로 주석(Footnote)을 추가합니다.

사용법:
    1. 프로젝트 루트 디렉터리에서 실행합니다.
    2. python scripts/add_footnotes.py

의존성:
    - 외부 라이브러리(Zero-dependency) 없이 표준 파이썬 3 환경에서 즉시 실행 가능합니다.
"""

import os
import re

# 약어 사전 정의
ABBREVIATIONS = {
    'CPO': ('Charge Point Operator', '전기차 충전소 운영 사업자'),
    'CSMS': ('Charging Station Management System', '충전기 통합 관리 시스템'),
    'OCPP': ('Open Charge Point Protocol', '개방형 충전 통신 규격'),
    'EIP': ('Enterprise Integration Patterns', '기업 통합 패턴 (소프트웨어 아키텍처 디자인 패턴)'),
    'HA': ('High Availability', '고가용성 (서버 다중화 등을 통한 서비스 중단 최소화 설계)'),
    'OLTP': ('Online Transaction Processing', '실시간 트랜잭션 처리'),
    'OLAP': ('Online Analytical Processing', '실시간 데이터 분석 처리'),
    'FOTA': ('Firmware Over-The-Air', '무선 펌웨어 업데이트'),
    'PnC': ('Plug and Charge', '플러그 앤 차지 (충전 커넥터 연결 시 자동 인증 및 결제)'),
    'RBAC': ('Role-Based Access Control', '역할 기반 접근 제어 (사용자 권한 관리)'),
    'PG': ('Payment Gateway', '전자 결제 대행사'),
    'EAI': ('Enterprise Application Integration', '기업 애플리케이션 통합'),
    'CQRS': ('Command Query Responsibility Segregation', '명령 및 조회 책임 분리 패턴'),
    'CE': ('Community Edition', '커뮤니티 에디션 (오픈소스 버전)'),
    'EE': ('Enterprise Edition', '엔터프라이즈 에디션 (상용 버전)'),
    'GW': ('Gateway', '게이트웨이 (서버/기기 간의 통신 접점)'),
    'FND': ('Flexible Numeric Display', '세그먼트 표시기 (충전기 전면부 숫자 화면)'),
    'FMS': ('Fleet Management System', '차량 관제 시스템 / 플릿 관리 시스템'),
    'RFID': ('Radio Frequency Identification', '무선 주파수 식별 (충전 회원 카드 등)'),
    'TPS': ('Transactions Per Second', '초당 트랜잭션 처리 수'),
    'BID': ('Bus Identification / Billing ID', '충전기 식별 ID 또는 빌링 연동 식별 정보 (서울시/환경부 연동 규격)'),
    'WAL': ('Write-Ahead Logging', '로그 선행 기입 (데이터베이스의 트랜잭션 안정성 확보 로그)'),
    'OOM': ('Out Of Memory', '메모리 부족 현상'),
    'SLA': ('Service Level Agreement', '서비스 수준 합의 (시스템 가동률 등 보장 표준)'),
    'E2E': ('End-to-End', '종단 간 (처음부터 끝까지 전체 경로)'),
    'DTO': ('Data Transfer Object', '데이터 전송 객체'),
    'CRUD': ('Create, Read, Update, Delete', '생성, 조회, 수정, 삭제의 기본 데이터 가동 기능'),
    'SoC': ('State of Charge', '배터리 충전 상태 비율'),
    'SOH': ('State of Health', '배터리 수명 상태 비율'),
    'UI': ('User Interface', '사용자 인터페이스'),
    'UX': ('User Experience', '사용자 경험'),
    'API': ('Application Programming Interface', '응용 프로그램 프로그래밍 인터페이스'),
    'DB': ('Database', '데이터베이스'),
    'JVM': ('Java Virtual Machine', '자바 가상 머신'),
    'SSL': ('Secure Sockets Layer', '보안 소켓 레이어 (보안 통신 표준)'),
    'TLS': ('Transport Layer Security', '전송 계층 보안 (보안 통신 표준)'),
    'RDB': ('Relational Database', '관계형 데이터베이스'),
    'IP': ('Internet Protocol', '인터넷 프로토콜 (네트워크 주소 규격)'),
    'LTE': ('Long Term Evolution', '4세대 무선 통신 규격'),
    'TOU': ('Time of Use', '계절별/시간대별 차등 요금제'),
    'CPU': ('Central Processing Unit', '중앙 처리 장치'),
    'RAM': ('Random Access Memory', '주기억장치 (메모리)'),
    'SSD': ('Solid State Drive', '반도체 기반 대용량 저장 장치')
}

# 마크다운 내 무시할 패턴 정의 (코드 블록, 인라인 코드, 링크, HTML 태그)
md_ignore_pattern = re.compile(
    r'(?P<code_block>```.*?```)'
    r'|(?P<inline_code>`[^`\n]+`)'
    r'|(?P<link>\[[^\]]+\]\([^)]+\))'
    r'|(?P<html_tag><[^>]+>)',
    re.DOTALL
)

# HTML 구문 파싱용 정규식
html_token_re = re.compile(r'(<!--.*?-->|<[^>]+>)', re.DOTALL)


def add_footnotes_to_markdown(filepath):
    """마크다운 파일에 약어 주석을 추가합니다."""
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()

    sorted_abbrevs = sorted(ABBREVIATIONS.keys(), key=len, reverse=True)
    abbrev_re = re.compile(r'\b(' + '|'.join(re.escape(a) for a in sorted_abbrevs) + r')\b')

    # 마크다운 데이터 세그먼트 분할
    segments = []
    last_idx = 0
    for match in md_ignore_pattern.finditer(content):
        start, end = match.span()
        if start > last_idx:
            segments.append((content[last_idx:start], False))  # 일반 텍스트
        segments.append((content[start:end], True))  # 무시 대상 블록
        last_idx = end
    if last_idx < len(content):
        segments.append((content[last_idx:], False))

    seen_abbrevs = set()
    new_segments = []

    # 일반 텍스트 영역 내 약어 최초 등장 매칭 수행
    for text, is_ignored in segments:
        if is_ignored:
            new_segments.append(text)
        else:
            pos = 0
            replaced_text = []
            for match in abbrev_re.finditer(text):
                match_start, match_end = match.span()
                abbrev = match.group(1)
                replaced_text.append(text[pos:match_start])
                if abbrev not in seen_abbrevs:
                    seen_abbrevs.add(abbrev)
                    replaced_text.append(f"{abbrev}[^{abbrev}]")
                else:
                    replaced_text.append(abbrev)
                pos = match_end
            replaced_text.append(text[pos:])
            new_segments.append("".join(replaced_text))

    new_content = "".join(new_segments)

    # 발견된 약어가 있고 기존 정의 목록에 없는 것만 추가
    if seen_abbrevs:
        found_sorted = sorted(list(seen_abbrevs))
        existing_definitions = re.findall(r'^\[\^([^\]]+)\]:', new_content, re.MULTILINE)
        to_add = [a for a in found_sorted if a not in existing_definitions]

        if to_add:
            footnotes_block = ["\n\n---\n"]
            for abbrev in to_add:
                full_eng, desc_kor = ABBREVIATIONS[abbrev]
                footnotes_block.append(f"[^{abbrev}]: **{abbrev} ({full_eng}):** {desc_kor}\n")
            new_content = new_content.rstrip() + "".join(footnotes_block)

    return new_content, len(seen_abbrevs)


def add_footnotes_to_html(filepath):
    """HTML 파일에 약어 하이퍼링크와 주석 목록을 추가합니다."""
    with open(filepath, "r", encoding="utf-8") as f:
        html_content = f.read()

    sorted_abbrevs = sorted(ABBREVIATIONS.keys(), key=len, reverse=True)
    abbrev_re = re.compile(r'\b(' + '|'.join(re.escape(a) for a in sorted_abbrevs) + r')\b')

    tokens = html_token_re.split(html_content)

    in_ignored_block = False
    ignored_tag = None
    seen_abbrevs = set()
    new_tokens = []

    for token in tokens:
        if not token:
            continue
        if token.startswith('<') or token.startswith('<!--'):
            # HTML 태그 또는 주석 처리
            lower_token = token.lower()
            if lower_token.startswith('<script') or lower_token.startswith('<style') or lower_token.startswith('<pre') or lower_token.startswith('<code'):
                in_ignored_block = True
                ignored_tag = lower_token.replace('<', '').replace('>', '').split()[0]
            elif in_ignored_block and lower_token.startswith(f'</{ignored_tag}'):
                in_ignored_block = False
                ignored_tag = None
            new_tokens.append(token)
        else:
            if in_ignored_block:
                new_tokens.append(token)
            else:
                pos = 0
                replaced_text = []
                for match in abbrev_re.finditer(token):
                    match_start, match_end = match.span()
                    abbrev = match.group(1)
                    replaced_text.append(token[pos:match_start])
                    if abbrev not in seen_abbrevs:
                        seen_abbrevs.add(abbrev)
                        abbrev_lower = abbrev.lower()
                        replaced_text.append(
                            f'{abbrev}<a href="#fn-{abbrev_lower}" id="fnref-{abbrev_lower}" style="text-decoration: none; color: var(--primary-color); vertical-align: super; font-size: 0.75rem; margin-left: 2px;">[{abbrev}]</a>'
                        )
                    else:
                        replaced_text.append(abbrev)
                    pos = match_end
                replaced_text.append(token[pos:])
                new_tokens.append("".join(replaced_text))

    result_html = "".join(new_tokens)

    # 하단 주석 영역 섹션 주입 (중복 생성 방지 기능 포함)
    if seen_abbrevs and 'class="footnotes"' not in result_html:
        found_sorted = sorted(list(seen_abbrevs))
        footnotes_block = []
        footnotes_block.append('      <!-- Footnotes Section -->\n')
        footnotes_block.append('      <hr style="margin-top: 40px; border: 0; border-top: 1px solid var(--border-color);">\n')
        footnotes_block.append('      <section class="footnotes" style="font-size: 0.9rem; color: var(--text-secondary); line-height: 1.6; margin-top: 20px;">\n')
        footnotes_block.append('        <h4 style="margin-bottom: 10px; color: var(--text-primary);">약어 정의 (Footnotes)</h4>\n')
        footnotes_block.append('        <ul style="list-style-type: none; padding-left: 0;">\n')

        for abbrev in found_sorted:
            abbrev_lower = abbrev.lower()
            full_eng, desc_kor = ABBREVIATIONS[abbrev]
            footnotes_block.append(
                f'          <li id="fn-{abbrev_lower}" style="margin-bottom: 6px;">\n'
                f'            <a href="#fnref-{abbrev_lower}" style="text-decoration: none; color: var(--primary-color);"><sup>[{abbrev}]</sup></a>\n'
                f'            <strong>{abbrev} ({full_eng}):</strong> {desc_kor}\n'
                f'          </li>\n'
            )
        footnotes_block.append('        </ul>\n')
        footnotes_block.append('      </section>\n')

        footnotes_str = "".join(footnotes_block)

        if '</main>' in result_html:
            result_html = result_html.replace('</main>', footnotes_str + '</main>')
        else:
            result_html = result_html.replace('</body>', footnotes_str + '</body>')

    return result_html, len(seen_abbrevs)


def main():
    # 처리 대상 파일 명시 (상대 경로로 작성하여 범용 구동 보장)
    files_to_process = [
        # doc/01.standard_solution/
        ("doc/01.standard_solution/01.prd.md", "md"),
        ("doc/01.standard_solution/01.prd.html", "html"),
        ("doc/01.standard_solution/02.feature_specification.md", "md"),
        ("doc/01.standard_solution/02.feature_specification.html", "html"),
        ("doc/01.standard_solution/03.timeline.md", "md"),
        ("doc/01.standard_solution/03.timeline.html", "html"),
        ("doc/01.standard_solution/04.software_architecture.md", "md"),
        ("doc/01.standard_solution/04.software_architecture.html", "html"),
        ("doc/01.standard_solution/05.poc.md", "md"),
        ("doc/01.standard_solution/05.poc.html", "html"),

        # doc/02.open_source_sales/
        ("doc/02.open_source_sales/01.prd.md", "md"),
        ("doc/02.open_source_sales/01.prd.html", "html"),
        ("doc/02.open_source_sales/02.open_source_strategy.md", "md"),
        ("doc/02.open_source_sales/02.open_source_strategy.html", "html"),

        # doc/03.development_process/
        ("doc/03.development_process/01.requirements_definition.md", "md"),
        ("doc/03.development_process/02.system_analysis.md", "md"),
        ("doc/03.development_process/03.architecture_design.md", "md"),
        ("doc/03.development_process/04.implementation_and_deployment.md", "md"),

        # 루트 레벨 파일
        ("README.md", "md"),
        ("start_transaction_process_flows.md", "md")
    ]

    print("약어 주석 추가 처리를 진행합니다...")
    summary = []
    
    for rel_path, filetype in files_to_process:
        # 프로젝트 루트 기준으로 한 절대 경로 획득
        filepath = os.path.abspath(rel_path)
        
        if not os.path.exists(filepath):
            print(f"파일을 찾을 수 없어 건너뜁니다: {rel_path}")
            continue

        try:
            if filetype == "md":
                new_content, count = add_footnotes_to_markdown(filepath)
            else:  # html
                new_content, count = add_footnotes_to_html(filepath)
                
            with open(filepath, "w", encoding="utf-8") as f:
                f.write(new_content)
                
            print(f"완료: {rel_path} | 타입: {filetype} | 주석 추가된 약어: {count}개")
            summary.append((rel_path, filetype, count, "성공"))
        except Exception as e:
            print(f"오류 발생 ({rel_path}): {e}")
            summary.append((rel_path, filetype, 0, f"실패 ({e})"))

    print("\n" + "="*20 + " 처리 결과 요약 " + "="*20)
    for path, ftype, count, status in summary:
        print(f" - {path} ({ftype.upper()}): {status} (약어 {count}개 반영)")


if __name__ == "__main__":
    main()
