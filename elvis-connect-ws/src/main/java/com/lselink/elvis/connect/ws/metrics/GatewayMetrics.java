package com.lselink.elvis.connect.ws.metrics;

import com.lselink.elvis.connect.ws.session.LocalSessionStore;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 게이트웨이 WebSocket 세션 및 인바운드/아웃바운드 TPS 실시간 모니터링 메트릭 (Micrometer & Prometheus 연동)
 * 요구사항: REQ-004, REQ-005
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayMetrics {

    private final MeterRegistry meterRegistry;
    private final LocalSessionStore sessionStore;

    private Counter inboundPacketCounter;
    private Counter outboundCommandCounter;
    private Timer packetProcessingTimer;

    @PostConstruct
    public void init() {
        // 1. 활성 충전기 동시 연결 수 게이지 (Gauge)
        Gauge.builder("elvis.ws.connected.chargers", sessionStore, LocalSessionStore::getActiveSessionCount)
                .description("현재 게이트웨이에 연결된 활성 충전기 소켓 세션 수")
                .tag("service", "elvis-connect-ws")
                .register(meterRegistry);

        // 2. 인바운드 패킷 카운터 (초당 TPS 산출용 Counter)
        inboundPacketCounter = Counter.builder("elvis.ws.inbound.packets")
                .description("충전기로부터 수신된 총 Inbound OCPP 패킷 건수")
                .tag("service", "elvis-connect-ws")
                .register(meterRegistry);

        // 3. 아웃바운드 커맨드 카운터
        outboundCommandCounter = Counter.builder("elvis.ws.outbound.commands")
                .description("충전기로 하향 전송된 총 Outbound 커맨드 건수")
                .tag("service", "elvis-connect-ws")
                .register(meterRegistry);

        // 4. 패킷 처리 레이턴시 타이머
        packetProcessingTimer = Timer.builder("elvis.ws.packet.latency")
                .description("인바운드 패킷 수신부터 Kafka 발행 완료까지 소요 시간")
                .tag("service", "elvis-connect-ws")
                .register(meterRegistry);

        log.info("[GatewayMetrics] Micrometer 모니터링 메트릭 초기화 완료 (Gauge, Counter, Timer)");
    }

    public void recordInboundPacket() {
        if (inboundPacketCounter != null) {
            inboundPacketCounter.increment();
        }
    }

    public void recordOutboundCommand() {
        if (outboundCommandCounter != null) {
            outboundCommandCounter.increment();
        }
    }

    public Timer getPacketProcessingTimer() {
        return packetProcessingTimer;
    }
}
