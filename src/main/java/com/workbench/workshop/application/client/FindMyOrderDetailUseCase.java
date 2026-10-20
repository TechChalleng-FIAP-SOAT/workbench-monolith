package com.workbench.workshop.application.client;

import com.workbench.workshop.application.budget.FindBudgetByServiceOrderUseCase;
import com.workbench.workshop.application.client.port.out.ClientRepository;
import com.workbench.workshop.application.serviceorder.port.out.ServiceOrderRepository;
import com.workbench.workshop.domain.budget.Budget;
import com.workbench.workshop.domain.client.Client;
import com.workbench.workshop.domain.client.ClientNotFoundException;
import com.workbench.workshop.domain.client.Cpf;
import com.workbench.workshop.domain.serviceorder.ServiceOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FindMyOrderDetailUseCase {

    private final ClientRepository clientRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final FindBudgetByServiceOrderUseCase findBudgetByServiceOrderUseCase;

    @Transactional(readOnly = true)
    public FindMyOrderDetailResult execute(String email, UUID orderId) {
        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new ClientNotFoundException("email: " + email));
        return detailFor(client, orderId);
    }

    /** Resolves the client by CPF/document — used for client (CPF) authentication. */
    @Transactional(readOnly = true)
    public FindMyOrderDetailResult executeByDocument(String document, UUID orderId) {
        Client client = clientRepository.findByDocument(new Cpf(document).getValue())
                .orElseThrow(() -> new ClientNotFoundException("document: " + document));
        return detailFor(client, orderId);
    }

    private FindMyOrderDetailResult detailFor(Client client, UUID orderId) {
        ServiceOrder order = serviceOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + orderId));

        if (!order.getCpfCnpj().equals(client.getDocument().getValue())) {
            throw new IllegalArgumentException("Order " + orderId + " does not belong to this client");
        }

        Budget budget = findBudgetByServiceOrderUseCase.execute(orderId).orElse(null);
        return new FindMyOrderDetailResult(order, budget);
    }

    public record FindMyOrderDetailResult(ServiceOrder order, Budget budget) {}
}
