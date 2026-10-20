package com.workbench.workshop.application.client;

import com.workbench.workshop.application.client.port.out.ClientRepository;
import com.workbench.workshop.application.serviceorder.port.out.ServiceOrderRepository;
import com.workbench.workshop.domain.client.Client;
import com.workbench.workshop.domain.client.ClientNotFoundException;
import com.workbench.workshop.domain.client.Cpf;
import com.workbench.workshop.domain.serviceorder.ServiceOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FindMyOrdersUseCase {

    private final ClientRepository clientRepository;
    private final ServiceOrderRepository serviceOrderRepository;

    @Transactional(readOnly = true)
    public List<ServiceOrder> execute(String email) {
        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new ClientNotFoundException("email: " + email));
        return serviceOrderRepository.findByCpfCnpj(client.getDocument().getValue());
    }

    /** Resolves the client by CPF/document — used for client (CPF) authentication. */
    @Transactional(readOnly = true)
    public List<ServiceOrder> executeByDocument(String document) {
        Client client = clientRepository.findByDocument(new Cpf(document).getValue())
                .orElseThrow(() -> new ClientNotFoundException("document: " + document));
        return serviceOrderRepository.findByCpfCnpj(client.getDocument().getValue());
    }
}
