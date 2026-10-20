package com.workbench.workshop.application.user.port.out;

import com.workbench.workshop.domain.user.User;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface UserRepository {
    Optional<User> findByEmail(String email);
    SignUpResult save(String email, String encryptedPassword, Set<String> roles);

    record SignUpResult(UUID id, String email, Set<String> roles) {}
}
