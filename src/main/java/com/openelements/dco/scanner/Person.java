package com.openelements.dco.scanner;

import java.util.Set;

public record Person(String name, String email, String gitHubIdentifier, Set<Role> roles, boolean internal) {


    public Person {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name must not be null or empty");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email must not be null or empty");
        }
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("Roles must not be null or empty");
        }
    }
    
    public boolean isSigned() {
        return roles().contains(Role.SIGNER);
    }

    public boolean isAuthor() {
        return roles().contains(Role.AUTHOR) || roles().contains(Role.CO_AUTHER);
    }
}
