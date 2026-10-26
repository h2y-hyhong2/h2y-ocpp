package com.lselink.elvis.control.service;

import com.lselink.elvis.control.dto.UiNotificationDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 최근 발생한 장애 알림을 보관하는 인메모리 링 버퍼 (Sprint 28: S28-API-0060-CO)
 * <p>
 * 신규 접속자나 새로고침 클라이언트에게 최근 N건(기본 50건)의 장애 목록을
 * DB 부하 없이 즉시 제공하기 위한 스레드 안전 FIFO 링 버퍼 캐시입니다.
 * </p>
 */
@Component
public class RecentNotificationBuffer {

    private final int maxCapacity;
    private final ConcurrentLinkedDeque<UiNotificationDto> buffer;

    public RecentNotificationBuffer(
            @Value("${elvis.control.notification.buffer-size:50}") int maxCapacity) {
        this.maxCapacity = maxCapacity;
        this.buffer = new ConcurrentLinkedDeque<>();
    }

    /**
     * 신규 알림 적재 (최신순 전진 삽입 후 최대 용량 초과 시 오래된 항목 제거)
     */
    public void add(UiNotificationDto notification) {
        if (notification == null) {
            return;
        }

        buffer.addFirst(notification);
        while (buffer.size() > maxCapacity) {
            buffer.pollLast();
        }
    }

    /**
     * 최근 알림 N건 최신순 조회
     */
    public List<UiNotificationDto> getRecentList(int limit) {
        int targetLimit = limit;
        if (targetLimit <= 0) {
            targetLimit = 50;
        } else if (targetLimit > maxCapacity) {
            targetLimit = maxCapacity;
        }

        List<UiNotificationDto> resultList = new ArrayList<>();
        int count = 0;
        for (UiNotificationDto item : buffer) {
            if (count >= targetLimit) {
                break;
            }
            resultList.add(item);
            count++;
        }
        return resultList;
    }

    public int size() {
        return buffer.size();
    }

    public void clear() {
        buffer.clear();
    }
}
