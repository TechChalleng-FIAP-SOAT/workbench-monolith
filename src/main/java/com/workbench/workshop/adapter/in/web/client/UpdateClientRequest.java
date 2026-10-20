package com.workbench.workshop.adapter.in.web.client;

import com.workbench.workshop.infrastructure.config.UpperCase;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateClientRequest(
        @NotBlank @UpperCase String name,
        @NotBlank String cpf,
        @Email String email,
        String phone
) {}
