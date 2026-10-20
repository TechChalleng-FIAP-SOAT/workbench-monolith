package com.workbench.workshop.application.stock;

import com.workbench.workshop.application.stock.port.out.StockMovementRepository;
import com.workbench.workshop.application.stock.port.out.StockRepository;
import com.workbench.workshop.domain.stock.Stock;
import com.workbench.workshop.domain.stock.StockMovement;
import com.workbench.workshop.domain.stock.StockMovementType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class StockExitUseCase {

    private final StockRepository stockRepository;
    private final StockMovementRepository movementRepository;

    @Transactional
    public Stock execute(Long productId, BigDecimal quantity, String reason) {
        Stock stock = stockRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("Stock not found for product: " + productId));
        stock.removeQuantity(quantity);
        Stock saved = stockRepository.save(stock);

        StockMovement movement = new StockMovement();
        movement.setStockId(saved.getId());
        movement.setType(StockMovementType.EXIT);
        movement.setQuantity(quantity);
        movement.setReason(reason);
        movementRepository.save(movement);

        return saved;
    }
}
