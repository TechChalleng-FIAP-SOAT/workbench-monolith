package com.workbench.workshop.domain.stock;

import java.util.UUID;

public record ReservationChangedEvent(UUID serviceOrderId) {}
