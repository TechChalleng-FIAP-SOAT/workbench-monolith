package com.workbench.workshop.application.vehicle;

import com.workbench.workshop.application.vehicle.port.out.VehicleRepository;
import com.workbench.workshop.domain.vehicle.Vehicle;
import com.workbench.workshop.domain.vehicle.VehicleNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeactivateVehicleUseCase {

    private final VehicleRepository vehicleRepository;

    @Transactional
    public void execute(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new VehicleNotFoundException("id: " + id));
        vehicle.deactivate();
        vehicleRepository.save(vehicle);
    }
}
