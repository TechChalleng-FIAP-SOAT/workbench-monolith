package com.workbench.workshop.adapter.in.web.monitoring;

import com.workbench.workshop.domain.monitoring.AverageTimeEnum;
import lombok.Data;
import java.util.UUID;

@Data
public class GetAverageExecutionTimeByIdRequest {
    AverageTimeEnum timeUnit;
    UUID id;
}
