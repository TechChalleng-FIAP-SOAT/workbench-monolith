package com.workbench.workshop.application.stock;

import com.workbench.workshop.application.stock.port.out.StockRepository;
import com.workbench.workshop.domain.stock.Stock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FindStockByProductIdUseCase {

    private final StockRepository stockRepository;

    @Transactional(readOnly = true)
    public Stock execute(Long productId) {
        return stockRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("Stock not found for product: " + productId));
    }
}
