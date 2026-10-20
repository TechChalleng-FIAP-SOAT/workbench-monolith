package com.workbench.workshop.application.product.supply;

import com.workbench.workshop.application.product.supply.port.out.SupplyRepository;
import com.workbench.workshop.domain.product.Supply;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FindSupplyBySkuUseCase {

    private final SupplyRepository supplyRepository;

    @Transactional(readOnly = true)
    public Supply execute(String sku) {
        return supplyRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("Supply not found with SKU: " + sku));
    }
}
