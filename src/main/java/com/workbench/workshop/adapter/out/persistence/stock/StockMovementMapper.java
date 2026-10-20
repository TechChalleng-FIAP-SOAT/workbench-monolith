package com.workbench.workshop.adapter.out.persistence.stock;

import com.workbench.workshop.domain.stock.StockMovement;
import com.workbench.workshop.infrastructure.persistence.stock.StockMovementEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StockMovementMapper {
    StockMovement toDomain(StockMovementEntity entity);
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    StockMovementEntity toEntity(StockMovement movement);
}
