package com.workbench.workshop.application.client;

import com.workbench.workshop.application.client.port.out.ClientRepository;
import com.workbench.workshop.domain.client.Client;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListClientsUseCase {

    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public List<Client> execute() {
        return clientRepository.findAllActive();
    }
}
