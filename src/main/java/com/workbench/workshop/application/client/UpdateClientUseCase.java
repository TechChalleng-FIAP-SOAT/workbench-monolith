package com.workbench.workshop.application.client;

import com.workbench.workshop.application.client.port.out.ClientRepository;
import com.workbench.workshop.domain.client.Client;
import com.workbench.workshop.domain.client.ClientNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateClientUseCase {

    private final ClientRepository clientRepository;

    @Transactional
    public Client execute(Long id, String name, String email, String phone) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException("id: " + id));
        client.update(name, email, phone);
        return clientRepository.save(client);
    }
}
