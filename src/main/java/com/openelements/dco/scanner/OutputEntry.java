package com.openelements.dco.scanner;

import java.time.Instant;

public record OutputEntry(String commitLink, Instant time, String name, String email, String githubAccount) {

    public OutputEntry(String repositoryUrl, Commit c, Person p) {
        this(repositoryUrl + "/commit/" + c.identifier(),
                c.time().toInstant(), p.name(), p.email(), p.gitHubIdentifier());
    }

    public OutputEntry(String commitLink, Instant time, String name, String email, String githubAccount) {
        this.commitLink = commitLink;
        this.time = time;
        this.name = name;
        this.email = email;
        this.githubAccount = githubAccount;
    }
}
