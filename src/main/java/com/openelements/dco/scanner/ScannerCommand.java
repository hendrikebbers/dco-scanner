package com.openelements.dco.scanner;

import java.util.List;
import java.util.concurrent.Callable;
import org.eclipse.jgit.api.Git;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Parameters;

@Command(description = "A tool to scan a repository for commits without DCO signing")
public class ScannerCommand implements Callable<Integer> {

    private final static Logger log = LoggerFactory.getLogger(ScannerCommand.class);

    public static void main(String[] args) {
        new CommandLine(new ScannerCommand()).execute("https://github.com/hiero-ledger/hiero-sdk-cpp");
    }

    @Parameters(description = "repository that should be scanned", index = "0")
    private String repositoryUrl;

    @Override
    public Integer call() {
        try {
            final Git git = RepositoryFactory.checkout(repositoryUrl);
            final List<Commit> list = CommitFactory.createFor(git);
            list.stream()
                    .filter(commit -> !isFullySigned(commit))
                    .forEach(commit -> log.info("{}-{}: {}", commit.identifier(), commit.time(), commit.persons()));
        } catch (Exception e) {
            log.error("Error while scanning repository", e);
            return ExitCode.SOFTWARE;
        }
        return ExitCode.OK;
    }

    private boolean isFullySigned(final Commit commit) {
        return !commit.persons().stream().anyMatch(person -> (person.roles().contains(Role.AUTHOR)
                || person.roles().contains(Role.CO_AUTHER) && !person.roles().contains(Role.SIGNER)));
    }

}
