package com.workbench.workshop.application.stock;

import com.workbench.workshop.application.stock.port.out.StockRepository;
import com.workbench.workshop.domain.stock.Stock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListStocksUseCase {

    private final StockRepository stockRepository;

    @Transactional(readOnly = true)
    public List<Stock> execute() {
        return stockRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Stock> executeLowStock() {
        return stockRepository.findAll().stream().filter(Stock::isLowStock).toList();
    }
}
