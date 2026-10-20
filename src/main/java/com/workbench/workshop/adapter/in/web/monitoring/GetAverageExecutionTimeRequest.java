package com.workbench.workshop.adapter.in.web.monitoring;

import com.workbench.workshop.domain.monitoring.AverageTimeEnum;
import lombok.Data;

@Data
public class GetAverageExecutionTimeRequest {
    AverageTimeEnum timeUnit;
}
