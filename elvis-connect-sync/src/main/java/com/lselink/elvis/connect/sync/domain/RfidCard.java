package com.lselink.elvis.connect.sync.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 회원 RFID 카드 마스터 엔티티 (Sprint 20: S20-SYNC-0070-DE)
 * <p>
 * OCPP Authorize 수신 시 RFID 태그의 유효성 검증(상태, 유효기간, 잔액, 동시 세션)의
 * 영속성 원장(L3 DB) 역할을 수행합니다.
 * </p>
 */
@Entity
@Table(
        name = "tb_rfid_card",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_RFID_CARD_ID_TAG", columnNames = {"ID_TAG"})
        },
        indexes = {
                @Index(name = "IX_RFID_CARD_MEMBER", columnList = "MEMBER_ID, CARD_STATUS"),
                @Index(name = "IX_RFID_CARD_PARENT", columnList = "PARENT_ID_TAG")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@DynamicUpdate
public class RfidCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    /** RFID 태그 식별자 (대문자 정규화) */
    @Column(name = "ID_TAG", nullable = false, length = 64)
    private String idTag;

    /** 회원 식별자 (tb_member 매핑) */
    @Column(name = "MEMBER_ID", nullable = false)
    private Long memberId;

    /** 회원명 (마스킹) */
    @Column(name = "MEMBER_NAME", length = 64)
    private String memberName;

    /** 상위/법인 대표 태그 식별자 */
    @Column(name = "PARENT_ID_TAG", length = 64)
    private String parentIdTag;

    /** 카드 발급 상태 */
    @Enumerated(EnumType.STRING)
    @Column(name = "CARD_STATUS", nullable = false, length = 32)
    @Builder.Default
    private CardStatus cardStatus = CardStatus.NORMAL;

    /** 카드 만료 일시 */
    @Column(name = "EXPIRY_DATE")
    private LocalDateTime expiryDate;

    /** 선불 잔액 / 결제 한도 (원) */
    @Column(name = "BALANCE", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    /** 최대 허용 동시 충전 세션 수 (기본 1) */
    @Column(name = "MAX_CONCURRENT_TX", nullable = false)
    @Builder.Default
    private int maxConcurrentTx = 1;

    /** 논리 삭제 여부 */
    @Column(name = "IS_DELETED", nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    /** 낙관적 락 버전 */
    @Version
    @Column(name = "VERSION", nullable = false)
    @Builder.Default
    private int version = 0;

    /** 등록 일시 */
    @CreationTimestamp
    @Column(name = "REG_DT", nullable = false, updatable = false)
    private LocalDateTime regDt;

    /** 수정 일시 */
    @UpdateTimestamp
    @Column(name = "UPDT_DT", nullable = false)
    private LocalDateTime updtDt;

    /**
     * 카드가 유효기간 및 상태 측면에서 정상인지 검사
     */
    public AuthorizationStatus evaluateStatus(LocalDateTime now) {
        if (isDeleted) {
            return AuthorizationStatus.Invalid;
        }
        if (cardStatus != CardStatus.NORMAL) {
            return AuthorizationStatus.Blocked;
        }
        if (expiryDate != null && expiryDate.isBefore(now)) {
            return AuthorizationStatus.Expired;
        }
        return AuthorizationStatus.Accepted;
    }
}
