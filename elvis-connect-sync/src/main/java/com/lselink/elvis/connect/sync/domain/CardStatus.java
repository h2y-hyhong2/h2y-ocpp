package com.lselink.elvis.connect.sync.domain;

/**
 * RFID 카드 발급/운영 상태 열거형 (tb_rfid_card.CARD_STATUS)
 */
public enum CardStatus {
    NORMAL("정상 카드"),
    BLOCKED("관리자 수동 차단"),
    LOST("분실/도난 신고"),
    SUSPENDED("요금 미납 또는 일시 정지");

    private final String description;

    CardStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isValid() {
        return this == NORMAL;
    }
}
