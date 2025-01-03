package com.openelements.dco.scanner;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.MDC;

public class CommitFactory {

    private final static Logger log = org.slf4j.LoggerFactory.getLogger(CommitFactory.class);

    private final static boolean STRICT = true;

    private CommitFactory() {
    }

    public static List<Commit> createFor(final Git git) {
        Objects.requireNonNull(git, "Git must not be null");
        log.debug("Scanning commits for {}", git.getRepository().getDirectory());
        try {
            final Iterable<RevCommit> commitIterable = git.log().call();
            return toStream(commitIterable)
                    .map(revCommit -> CommitFactory.of(revCommit))
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Error while scanning commits of repository", e);
        }
    }

    public static Commit of(final RevCommit revCommit) {
        Objects.requireNonNull(revCommit, "RevCommit must not be null");
        final String identifier = revCommit.getName();
        MDC.put("commit", identifier);
        try {
            log.debug("Processing commit {}", identifier);
            final ZonedDateTime time = ZonedDateTime.ofInstant(revCommit.getAuthorIdent().getWhen().toInstant(),
                    revCommit.getAuthorIdent().getTimeZone().toZoneId());
            final String fullMessage = revCommit.getFullMessage();
            final String shortMessage = revCommit.getShortMessage();
            final Set<Person> basicPersons = new HashSet<>();
            final Person author = PersonFactory.getInstance().create(revCommit.getAuthorIdent().getName(),
                    revCommit.getAuthorIdent().getEmailAddress(), Set.of(Role.AUTHOR));
            basicPersons.add(author);
            Optional.ofNullable(revCommit.getCommitterIdent())
                    .map(committerIdent -> PersonFactory.getInstance()
                            .create(committerIdent.getName(), committerIdent.getEmailAddress(),
                                    Set.of(Role.COMMITTER)))
                    .ifPresent(basicPersons::add);
            final Set<Person> coAuthors = extractFromMessage(fullMessage, "Co-authored-by:", Role.CO_AUTHER, false);
            final Set<Person> coDevelopers = extractFromMessage(fullMessage, "Co-developed-by:", Role.CO_AUTHER, false);
            final Set<Person> signers = extractFromMessage(fullMessage, "Signed-off-by:", Role.SIGNER, true);
            final Set<Person> mergedPersons = merge(basicPersons, coAuthors, coDevelopers, signers);
            return new Commit(identifier, time, fullMessage, shortMessage, Collections.unmodifiableSet(mergedPersons));
        } finally {
            MDC.remove("commit");
        }
    }

    private static Set<Person> extractFromMessage(final String fullMessage, final String identifierInMessage,
            final Role role, final boolean strict) {
        Objects.requireNonNull(fullMessage, "Full message must not be null");
        Objects.requireNonNull(identifierInMessage, "Identifier in message must not be null");
        Objects.requireNonNull(role, "Role must not be null");
        try {
            return fullMessage.lines()
                    .filter(line -> line.contains(identifierInMessage))
                    .map(line -> {
                        try {
                            if (STRICT) {
                                try {
                                    String term = line.substring(identifierInMessage.length() + 1);
                                    final List<String> sections = Arrays.asList(term.split(" "));
                                    if (sections.size() >= 2) {
                                        final String name = sections.subList(0, sections.size() - 1).stream()
                                                .collect(Collectors.joining(" "));
                                        final String emailSection = sections.get(sections.size() - 1);
                                        if (!emailSection.startsWith("<")) {
                                            throw new IllegalArgumentException("Email must start with '<'");
                                        }
                                        if (!emailSection.endsWith(">")) {
                                            throw new IllegalArgumentException("Email must end with '>'");
                                        }
                                        final String email = emailSection.substring(1, emailSection.length() - 1);
                                        return PersonFactory.getInstance().create(name, email, Set.of(role));
                                    } else {
                                        throw new IllegalArgumentException("Term must not be null or empty");
                                    }
                                } catch (Exception e) {
                                    log.warn("Strict mode violation in line: " + line);
                                    return null;
                                }
                            } else {
                                final int start = line.indexOf(identifierInMessage) + identifierInMessage.length();
                                final String term = line.substring(start).trim();
                                if (term.isBlank()) {
                                    throw new IllegalArgumentException("Term must not be null or empty");
                                }
                                if (term.contains("<") && term.contains(">")) {
                                    final int emailStart = term.indexOf("<");
                                    final String name = term.substring(0, emailStart).trim();
                                    final int emailEnd = term.indexOf(">");
                                    final String email = term.substring(emailStart + 1, emailEnd).trim();
                                    return PersonFactory.getInstance().create(name, email, Set.of(role));
                                } else {
                                    final List<String> sections = Arrays.asList(term.split(" "));
                                    if (sections.size() >= 2) {
                                        final String name = sections.subList(0, sections.size() - 1).stream()
                                                .collect(Collectors.joining(" "));
                                        final String email = sections.get(sections.size() - 1);
                                        return PersonFactory.getInstance().create(name, email, Set.of(role));
                                    } else if (sections.size() == 1) {
                                        final String name = sections.get(0);
                                        if (name.contains("@")) {
                                            return PersonFactory.getInstance().create("UNKNOWN", name, Set.of(role));
                                        } else {
                                            return PersonFactory.getInstance().create(name, "UNKNOWN", Set.of(role));
                                        }
                                    } else {
                                        throw new IllegalArgumentException("Term must not be null or empty");
                                    }
                                }
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(
                                    "Error while extracting " + identifierInMessage + " from line: " + line,
                                    e);
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toUnmodifiableSet());
        } catch (Exception e) {
            log.error("Error while extracting '" + identifierInMessage + "' from message", e);
            return Set.of(new Person("UNKNOWN", "UNKNOWN", null, Set.of(role), false));
        }
    }

    private static Set<Person> merge(Set<Person>... sets) {
        Objects.requireNonNull(sets, "Sets must not be null");
        if (sets.length == 0) {
            return Set.of();
        }
        if (sets.length == 1) {
            return Collections.unmodifiableSet(sets[0]);
        }
        final Set<Person> merged = new HashSet<>();
        for (Set<Person> set : sets) {
            set.stream().forEach(person -> {
                final Optional<Person> existingPerson = merged.stream()
                        .filter(p -> Objects.equals(p.email(), person.email())).findAny();
                if (existingPerson.isPresent()) {
                    final HashSet<Role> mergedRoles = new HashSet<>(existingPerson.get().roles());
                    mergedRoles.addAll(person.roles());
                    final Person updatedPerson = PersonFactory.getInstance()
                            .create(existingPerson.get().name(), existingPerson.get().email(), mergedRoles);
                    merged.remove(existingPerson.get());
                    merged.add(updatedPerson);
                } else {
                    merged.add(person);
                }
            });
        }
        return Collections.unmodifiableSet(merged);
    }

    private static <T> Stream<T> toStream(Iterable<T> iterable) {
        return toStream(iterable.iterator());
    }

    private static <T> Stream<T> toStream(Iterator<T> iterator) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
    }
}
