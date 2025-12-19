package com.roovies.concertreservation.payments.application.listener;

import com.roovies.concertreservation.payments.application.port.out.PaymentKafkaEventPort;
import com.roovies.concertreservation.shared.domain.event.ReservationCompletedKafkaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final PaymentKafkaEventPort paymentKafkaEventPort;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentCompleted(final ReservationCompletedKafkaEvent event) {
        log.info("[PaymentEventListener] 결제 완료 이벤트 수신 - paymentId: {}, userId: {}",
                event.paymentId(),
                event.userId());

        // 트랜잭션 커밋 완료 후 Kafka로 이벤트 발행
        paymentKafkaEventPort.publishReservationCompleted(event);

        log.info("[PaymentEventListener] Kafka 이벤트 발행 요청 완료 - paymentId: {}, userId: {}",
                event.paymentId(),
                event.userId());
    }
}
