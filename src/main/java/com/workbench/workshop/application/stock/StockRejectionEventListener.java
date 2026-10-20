package com.workbench.workshop.application.stock;

import com.workbench.workshop.application.stock.port.out.StockReservationRepository;
import com.workbench.workshop.domain.serviceorder.OrderRejectedEvent;
import com.workbench.workshop.domain.stock.StockReservationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockRejectionEventListener {

    private final ReleaseReservationUseCase releaseReservationUseCase;
    private final StockReservationRepository reservationRepository;

    @EventListener
    public void onOrderRejected(OrderRejectedEvent event) {
        var activeReservations = reservationRepository
                .findByServiceOrderIdAndStatus(event.serviceOrderId(), StockReservationStatus.ACTIVE);

        if (activeReservations.isEmpty()) {
            log.info("No active reservations to release for rejected OS: {}", event.serviceOrderId());
            return;
        }

        log.info("Releasing {} reservations for rejected OS: {}", activeReservations.size(), event.serviceOrderId());
        releaseReservationUseCase.execute(event.serviceOrderId());
    }
}
