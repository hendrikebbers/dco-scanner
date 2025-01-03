package com.openelements.dco.scanner;

import java.util.Objects;
import java.util.Set;

public record Person(String name, String email, String gitHubIdentifier, Set<Role> roles) {

    public Person(String name, String email, Set<Role> roles) {
        this(name, email, null, roles);
    }

    public Person(String name, String email, String gitHubIdentifier, Set<Role> roles) {
        this.name = Objects.requireNonNull(name, "Name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Name must not be blank");
        }

        this.email = Objects.requireNonNull(email, "Email must not be null");
        if (email.isBlank()) {
            throw new IllegalArgumentException("Email must not be blank");
        }

        if (email.endsWith("@users.noreply.github.com") && gitHubIdentifier == null) {
            final String identifier = email.substring(0, email.indexOf('@'));
            if (identifier.contains("+")) {
                this.gitHubIdentifier = identifier.substring(identifier.indexOf('+') + 1);
            } else {
                this.gitHubIdentifier = identifier;
            }
        } else {
            this.gitHubIdentifier = gitHubIdentifier;
        }

        this.roles = Objects.requireNonNull(roles, "Roles must not be null");
        if (roles.isEmpty()) {
            throw new IllegalArgumentException("Roles must not be empty");
        }
    }
}
