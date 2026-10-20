package com.workbench.workshop.application.budget;

import com.workbench.workshop.domain.stock.ReservationChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BudgetEventListener {

    private final RecalculateBudgetUseCase recalculateBudgetUseCase;

    @EventListener
    public void onReservationChanged(ReservationChangedEvent event) {
        recalculateBudgetUseCase.execute(event.serviceOrderId());
    }
}
