package com.workbench.workshop.application.vehicle;

import com.workbench.workshop.application.client.port.out.ClientRepository;
import com.workbench.workshop.application.vehicle.port.out.VehicleRepository;
import com.workbench.workshop.domain.client.ClientNotFoundException;
import com.workbench.workshop.domain.vehicle.Vehicle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FindVehiclesByClientUseCase {

    private final VehicleRepository vehicleRepository;
    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public List<Vehicle> execute(Long clientId) {
        clientRepository.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException("id: " + clientId));
        return vehicleRepository.findAllByClientId(clientId);
    }
}
