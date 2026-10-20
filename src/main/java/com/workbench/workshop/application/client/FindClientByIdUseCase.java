package com.workbench.workshop.application.client;

import com.workbench.workshop.application.client.port.out.ClientRepository;
import com.workbench.workshop.domain.client.Client;
import com.workbench.workshop.domain.client.ClientNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FindClientByIdUseCase {

    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public Client execute(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException("id: " + id));
    }
}
