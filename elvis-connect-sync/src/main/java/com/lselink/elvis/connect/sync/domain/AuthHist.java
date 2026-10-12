package com.lselink.elvis.connect.sync.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Authorize 인증 감사 이력 엔티티 (Sprint 20: S20-SYNC-0070-DE)
 * <p>
 * 충전기로부터 인입된 Authorize 요청에 대한 최종 판정 결과 및 처리 레이턴시를
 * 비동기(Fire-and-Forget)로 기록하여 감사업무 및 성능 모니터링을 지원합니다.
 * </p>
 */
@Entity
@Table(
        name = "tb_auth_hist",
        indexes = {
                @Index(name = "IX_AUTH_HIST_CHARGER_TIME", columnList = "CHARGE_BOX_ID, REG_DT"),
                @Index(name = "IX_AUTH_HIST_ID_TAG_TIME", columnList = "ID_TAG, REG_DT"),
                @Index(name = "IX_AUTH_HIST_TIME", columnList = "REG_DT")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AuthHist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    /** 요청 충전기 식별자 */
    @Column(name = "CHARGE_BOX_ID", nullable = false, length = 64)
    private String chargeBoxId;

    /** OCPP MessageId */
    @Column(name = "MESSAGE_ID", nullable = false, length = 64)
    private String messageId;

    /** 요청 RFID 태그 식별자 */
    @Column(name = "ID_TAG", nullable = false, length = 64)
    private String idTag;

    /** 인증 판정 결과 */
    @Enumerated(EnumType.STRING)
    @Column(name = "AUTH_STATUS", nullable = false, length = 32)
    private AuthorizationStatus authStatus;

    /** 응답된 상위 태그 식별자 */
    @Column(name = "PARENT_ID_TAG", length = 64)
    private String parentIdTag;

    /** 인증 실패/차단 상세 사유 */
    @Column(name = "FAIL_REASON", length = 128)
    private String failReason;

    /** 캐시 적중 계층 (L1_CAFFEINE, L2_REDIS, L3_DB, MISS) */
    @Column(name = "CACHE_TIER", nullable = false, length = 16)
    @Builder.Default
    private String cacheTier = "L2_REDIS";

    /** 판정 소요 시간 (밀리초) */
    @Column(name = "LATENCY_MS", nullable = false, precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal latencyMs = BigDecimal.ZERO;

    /** 인증 요청 수신 일시 */
    @CreationTimestamp
    @Column(name = "REG_DT", nullable = false, updatable = false)
    private LocalDateTime regDt;
}
