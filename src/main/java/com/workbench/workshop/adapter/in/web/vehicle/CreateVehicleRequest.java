package com.workbench.workshop.adapter.in.web.vehicle;

import com.workbench.workshop.domain.vehicle.VehicleType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateVehicleRequest(
        @NotNull @Positive Long clientId,
        @NotBlank String plate,
        @NotBlank String brand,
        @NotBlank String model,
        @Min(1886) int year,
        String color,
        @NotNull VehicleType type
) {}
