package com.openelements.dco.scanner;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.jgit.revwalk.RevCommit;

public record Commit(String identifier, ZonedDateTime time, String fullMessage, String shortMessage, Set<Person> persons) {

    public Commit {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Identifier must not be null or empty");
        }
        if (fullMessage == null || fullMessage.isBlank()) {
            throw new IllegalArgumentException("Full message must not be null or empty");
        }
        if (shortMessage == null || shortMessage.isBlank()) {
            throw new IllegalArgumentException("Short message must not be null or empty");
        }
        if (persons == null || persons.isEmpty()) {
            throw new IllegalArgumentException("Persons must not be null or empty");
        }
    }
}
