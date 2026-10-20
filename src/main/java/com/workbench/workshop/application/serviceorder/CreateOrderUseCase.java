package com.workbench.workshop.application.serviceorder;

import com.workbench.workshop.application.client.FindClientByCpfUseCase;
import com.workbench.workshop.application.notification.OrderStatusNotificationService;
import com.workbench.workshop.application.service.CreateServiceUseCase;
import com.workbench.workshop.application.serviceorder.port.out.ServiceOrderRepository;
import com.workbench.workshop.application.vehicle.FindVehicleByPlateUseCase;
import com.workbench.workshop.domain.serviceorder.ServiceOrder;
import com.workbench.workshop.infrastructure.monitoring.ServiceOrderMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CreateOrderUseCase {

    private final CreateServiceUseCase createServiceUseCase;
    private final FindClientByCpfUseCase findClientByCpfUseCase;
    private final FindVehicleByPlateUseCase findVehicleByPlateUseCase;
    private final ServiceOrderRepository serviceOrderRepository;
    private final OrderStatusNotificationService notificationService;
    private final ServiceOrderMetrics metrics;

    public ServiceOrder execute(String cpfCnpj, String placaVeiculo, List<String> serviceTypes) {
        try {
            findClientByCpfUseCase.execute(cpfCnpj);
            findVehicleByPlateUseCase.execute(placaVeiculo);

            ServiceOrder order = ServiceOrder.create(cpfCnpj, placaVeiculo, serviceTypes);

            serviceTypes.forEach(service -> createServiceUseCase.execute(service, order.getId()));

            ServiceOrder saved = serviceOrderRepository.save(order);

            notificationService.notifyStatusChange(saved);
            metrics.created();

            return saved;
        } catch (RuntimeException exception) {
            metrics.processingFailed("create");
            throw exception;
        }
    }
}
