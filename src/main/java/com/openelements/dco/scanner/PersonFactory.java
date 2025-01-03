package com.openelements.dco.scanner;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PersonFactory {

    private final static PersonFactory INSTANCE = new PersonFactory();

    private final List<String> internalEmails;

    private final List<String> internalDomains;

    private final List<String> internalGithubUsers;

    private PersonFactory() {
        internalEmails = new ArrayList<>();
        internalDomains = new ArrayList<>();
        internalGithubUsers = new ArrayList<>();
    }

    public void addInternalEmail(String email) {
        internalEmails.add(email);
    }

    public void addInternalDomain(String domain) {
        internalDomains.add(domain);
    }

    public void addInternalGitHubUser(String user) {
        internalGithubUsers.add(user);
    }

    public Person create(String name, String email, Set<Role> roles) {
        final String gitHubIdentifier;
        if (email.endsWith("@users.noreply.github.com")) {
            final String identifier = email.substring(0, email.indexOf("@users.noreply.github.com"));
            if (identifier.contains("+")) {
                gitHubIdentifier = identifier.substring(identifier.indexOf('+') + 1);
            } else {
                gitHubIdentifier = identifier;
            }
        } else {
            gitHubIdentifier = null;
        }
        final boolean internal = isInternal(email, gitHubIdentifier);
        return new Person(name, email, gitHubIdentifier, roles, internal);
    }

    private boolean isInternal(String email, String gitHubIdentifier) {
        if (internalEmails.contains(email)) {
            return true;
        }
        if (internalDomains.stream().anyMatch(domain -> email.endsWith("@" + domain))) {
            return true;
        }
        if (gitHubIdentifier != null && internalGithubUsers.contains(gitHubIdentifier)) {
            return true;
        }
        return false;
    }

    public static PersonFactory getInstance() {
        return INSTANCE;
    }
}
