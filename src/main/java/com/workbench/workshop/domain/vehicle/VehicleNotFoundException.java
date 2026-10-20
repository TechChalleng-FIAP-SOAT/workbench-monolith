package com.workbench.workshop.domain.vehicle;

public class VehicleNotFoundException extends RuntimeException {
    public VehicleNotFoundException(String identifier) {
        super("Vehicle not found: " + identifier);
    }
}
