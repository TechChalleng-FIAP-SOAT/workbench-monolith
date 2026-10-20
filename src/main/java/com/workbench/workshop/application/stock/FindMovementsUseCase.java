package com.workbench.workshop.application.stock;

import com.workbench.workshop.application.stock.port.out.StockMovementRepository;
import com.workbench.workshop.application.stock.port.out.StockRepository;
import com.workbench.workshop.domain.stock.StockMovement;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FindMovementsUseCase {

    private final StockRepository stockRepository;
    private final StockMovementRepository movementRepository;

    @Transactional(readOnly = true)
    public List<StockMovement> execute(Long productId) {
        var stock = stockRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("Stock not found for product: " + productId));
        return movementRepository.findByStockId(stock.getId());
    }
}
