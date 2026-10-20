package com.workbench.workshop.adapter.in.web.client;

import com.workbench.workshop.infrastructure.config.UpperCase;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateClientRequest(
        @NotBlank @UpperCase String name,
        @NotBlank String document,
        @Email String email,
        String phone
) {}
