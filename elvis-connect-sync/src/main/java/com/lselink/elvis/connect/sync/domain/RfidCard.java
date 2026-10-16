package com.lselink.elvis.connect.sync.domain;

import jakarta.persistence.*;
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
    private CardStatus cardStatus = CardStatus.NORMAL;

    /** 카드 만료 일시 */
    @Column(name = "EXPIRY_DATE")
    private LocalDateTime expiryDate;

    /** 선불 잔액 / 결제 한도 (원) */
    @Column(name = "BALANCE", nullable = false, precision = 12, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    /** 최대 허용 동시 충전 세션 수 (기본 1) */
    @Column(name = "MAX_CONCURRENT_TX", nullable = false)
    private int maxConcurrentTx = 1;

    /** 논리 삭제 여부 */
    @Column(name = "IS_DELETED", nullable = false)
    private boolean isDeleted = false;

    /** 낙관적 락 버전 */
    @Version
    @Column(name = "VERSION", nullable = false)
    private int version = 0;

    /** 등록 일시 */
    @CreationTimestamp
    @Column(name = "REG_DT", nullable = false, updatable = false)
    private LocalDateTime regDt;

    /** 수정 일시 */
    @UpdateTimestamp
    @Column(name = "UPDT_DT", nullable = false)
    private LocalDateTime updtDt;

    protected RfidCard() {
    }

    public RfidCard(Long id, String idTag, Long memberId, String memberName, String parentIdTag,
                    CardStatus cardStatus, LocalDateTime expiryDate, BigDecimal balance,
                    int maxConcurrentTx, boolean isDeleted, int version, LocalDateTime regDt, LocalDateTime updtDt) {
        this.id = id;
        this.idTag = idTag;
        this.memberId = memberId;
        this.memberName = memberName;
        this.parentIdTag = parentIdTag;
        this.cardStatus = cardStatus != null ? cardStatus : CardStatus.NORMAL;
        this.expiryDate = expiryDate;
        this.balance = balance != null ? balance : BigDecimal.ZERO;
        this.maxConcurrentTx = maxConcurrentTx;
        this.isDeleted = isDeleted;
        this.version = version;
        this.regDt = regDt;
        this.updtDt = updtDt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIdTag() { return idTag; }
    public void setIdTag(String idTag) { this.idTag = idTag; }

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public String getParentIdTag() { return parentIdTag; }
    public void setParentIdTag(String parentIdTag) { this.parentIdTag = parentIdTag; }

    public CardStatus getCardStatus() { return cardStatus; }
    public void setCardStatus(CardStatus cardStatus) { this.cardStatus = cardStatus; }

    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public int getMaxConcurrentTx() { return maxConcurrentTx; }
    public void setMaxConcurrentTx(int maxConcurrentTx) { this.maxConcurrentTx = maxConcurrentTx; }

    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public LocalDateTime getRegDt() { return regDt; }
    public void setRegDt(LocalDateTime regDt) { this.regDt = regDt; }

    public LocalDateTime getUpdtDt() { return updtDt; }
    public void setUpdtDt(LocalDateTime updtDt) { this.updtDt = updtDt; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String idTag;
        private Long memberId;
        private String memberName;
        private String parentIdTag;
        private CardStatus cardStatus = CardStatus.NORMAL;
        private LocalDateTime expiryDate;
        private BigDecimal balance = BigDecimal.ZERO;
        private int maxConcurrentTx = 1;
        private boolean isDeleted = false;
        private int version = 0;
        private LocalDateTime regDt;
        private LocalDateTime updtDt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder idTag(String idTag) { this.idTag = idTag; return this; }
        public Builder memberId(Long memberId) { this.memberId = memberId; return this; }
        public Builder memberName(String memberName) { this.memberName = memberName; return this; }
        public Builder parentIdTag(String parentIdTag) { this.parentIdTag = parentIdTag; return this; }
        public Builder cardStatus(CardStatus cardStatus) { this.cardStatus = cardStatus; return this; }
        public Builder expiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; return this; }
        public Builder balance(BigDecimal balance) { this.balance = balance; return this; }
        public Builder maxConcurrentTx(int maxConcurrentTx) { this.maxConcurrentTx = maxConcurrentTx; return this; }
        public Builder isDeleted(boolean isDeleted) { this.isDeleted = isDeleted; return this; }
        public Builder version(int version) { this.version = version; return this; }
        public Builder regDt(LocalDateTime regDt) { this.regDt = regDt; return this; }
        public Builder updtDt(LocalDateTime updtDt) { this.updtDt = updtDt; return this; }

        public RfidCard build() {
            return new RfidCard(id, idTag, memberId, memberName, parentIdTag, cardStatus,
                    expiryDate, balance, maxConcurrentTx, isDeleted, version, regDt, updtDt);
        }
    }

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
