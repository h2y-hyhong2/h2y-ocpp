package com.lselink.elvis.connect.sync.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.lselink.elvis.connect.sync.domain.AuthorizationStatus;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * OCPP 1.6-J Authorize.conf 응답의 idTagInfo 페이로드 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IdTagInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 인증 상태 (Accepted, Blocked, Expired, Invalid, ConcurrentTx) */
    private AuthorizationStatus status;

    /** 카드 유효기간 만료일시 (UTC ISO-8601 포맷) */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime expiryDate;

    /** 상위/법인 대표 태그 식별자 (선택) */
    private String parentIdTag;

    /** 캐시 적중 계층 메타데이터 (L1_CAFFEINE, L2_REDIS, L3_DB 등 내부 추적용) */
    private transient String cacheTier;

    /** 판정 소요시간 (밀리초) */
    private transient double latencyMs;

    public static IdTagInfo of(AuthorizationStatus status) {
        return IdTagInfo.builder()
                .status(status)
                .build();
    }

    public static IdTagInfo invalid() {
        return IdTagInfo.builder()
                .status(AuthorizationStatus.Invalid)
                .build();
    }
}
