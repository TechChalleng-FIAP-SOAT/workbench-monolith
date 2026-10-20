package com.workbench.workshop.adapter.in.web.service;

import com.workbench.workshop.domain.service.Status;
import com.workbench.workshop.domain.service.WorkshopService;

import java.util.List;
import java.util.UUID;

public record ServiceResponse(UUID id, String serviceTypeName, UUID idOS, List<Status> serviceStatus) {
    public static ServiceResponse from(WorkshopService service) {
        return new ServiceResponse(service.getId(), service.getServiceTypeName(), service.getIdOS(), service.getServiceStatus());
    }
}
