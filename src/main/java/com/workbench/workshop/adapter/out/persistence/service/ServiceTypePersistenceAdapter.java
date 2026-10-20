package com.workbench.workshop.adapter.out.persistence.service;

import com.workbench.workshop.application.service.port.out.ServiceTypeRepository;
import com.workbench.workshop.domain.service.ServiceType;
import com.workbench.workshop.infrastructure.persistence.service.ServiceTypeEntity;
import com.workbench.workshop.infrastructure.persistence.service.ServiceTypeJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ServiceTypePersistenceAdapter implements ServiceTypeRepository {

    private final ServiceTypeJpaRepository serviceTypeJpaRepository;

    @Override
    public Optional<ServiceType> findByName(String name) {
        return serviceTypeJpaRepository.findByName(name).map(this::toDomain);
    }

    @Override
    public List<ServiceType> findAll() {
        return serviceTypeJpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    private ServiceType toDomain(ServiceTypeEntity entity) {
        return new ServiceType(entity.getId(), entity.getName(), entity.getDescription());
    }
}
