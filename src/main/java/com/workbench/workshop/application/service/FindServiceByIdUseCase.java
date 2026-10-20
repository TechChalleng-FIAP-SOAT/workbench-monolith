package com.workbench.workshop.application.service;

import com.workbench.workshop.application.service.port.out.ServiceRepository;
import com.workbench.workshop.domain.service.WorkshopService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FindServiceByIdUseCase {

    private final ServiceRepository serviceRepository;

    public WorkshopService execute(UUID id) {
        return serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Servico nao encontrado. ID: " + id));
    }
}
