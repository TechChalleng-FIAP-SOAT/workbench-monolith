package com.workbench.workshop.adapter.in.web.service;

import com.workbench.workshop.domain.service.ServiceStatusEnum;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateServiceStatusRequest {
    private ServiceStatusEnum status;
    private UUID id;
}
