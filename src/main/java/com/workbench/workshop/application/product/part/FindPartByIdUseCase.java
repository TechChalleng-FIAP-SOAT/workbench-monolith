package com.workbench.workshop.application.product.part;

import com.workbench.workshop.application.product.part.port.out.PartRepository;
import com.workbench.workshop.domain.product.Part;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FindPartByIdUseCase {

    private final PartRepository partRepository;

    @Transactional(readOnly = true)
    public Part execute(Long id) {
        return partRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Part not found with id: " + id));
    }
}
