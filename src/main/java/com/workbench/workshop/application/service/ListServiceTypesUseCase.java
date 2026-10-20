package com.workbench.workshop.application.service;

import com.workbench.workshop.application.service.port.out.ServiceTypeRepository;
import com.workbench.workshop.domain.service.ServiceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListServiceTypesUseCase {

    private final ServiceTypeRepository serviceTypeRepository;

    public List<ServiceType> execute() {
        return serviceTypeRepository.findAll();
    }
}
