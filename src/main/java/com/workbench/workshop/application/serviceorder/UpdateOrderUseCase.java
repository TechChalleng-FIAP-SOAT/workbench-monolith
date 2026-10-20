package com.workbench.workshop.application.serviceorder;

import com.workbench.workshop.application.notification.OrderStatusNotificationService;
import com.workbench.workshop.application.serviceorder.port.out.ServiceOrderRepository;
import com.workbench.workshop.domain.serviceorder.OrderServiceStatusEnum;
import com.workbench.workshop.domain.serviceorder.ServiceOrder;
import com.workbench.workshop.infrastructure.monitoring.ServiceOrderMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateOrderUseCase {

    private final ServiceOrderRepository serviceOrderRepository;
    private final OrderStatusNotificationService notificationService;
    private final ServiceOrderMetrics metrics;

    @Transactional
    public void execute(UUID id, OrderServiceStatusEnum status) {
        try {
            if (status == null) {
                throw new IllegalArgumentException("Status da ordem de servico e obrigatorio.");
            }

            ServiceOrder order = serviceOrderRepository.findById(id)
                    .orElseThrow(() -> new NoSuchElementException("Ordem de servico nao encontrada com id: " + id));

            OrderServiceStatusEnum previousStatus = order.getStatus();
            var enteredAt = order.getUpdatedAt();
            order.advanceTo(status);
            ServiceOrder saved = serviceOrderRepository.save(order);

            notificationService.notifyStatusChange(saved);
            metrics.recordTimeInStatus(previousStatus, enteredAt);
        } catch (RuntimeException exception) {
            metrics.processingFailed("update_status");
            throw exception;
        }
    }
}
