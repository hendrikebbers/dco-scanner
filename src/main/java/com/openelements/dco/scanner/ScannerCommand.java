package com.openelements.dco.scanner;

import java.nio.file.Files;
import java.nio.file.Path;
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
        new CommandLine(new ScannerCommand()).execute("https://github.com/hiero-ledger/hiero-sdk-tck");
    }

    @Parameters(description = "repository that should be scanned", index = "0")
    private String repositoryUrl;

    @Override
    public Integer call() {
        try {

            readFile(Path.of("internal-domains.txt"))
                    .forEach(PersonFactory.getInstance()::addInternalDomain);
            readFile(Path.of("internal-emails.txt")).forEach(PersonFactory.getInstance()::addInternalEmail);
            readFile(Path.of("internal-github-users.txt"))
                    .forEach(PersonFactory.getInstance()::addInternalGitHubUser);

            final Git git = RepositoryFactory.checkout(repositoryUrl);
            final List<Commit> list = CommitFactory.createFor(git);
            list.stream()
                    .filter(commit -> !commit.isValid())
                    .forEach(commit -> log.info("{}-{}: {}", commit.identifier(), commit.time(), commit.persons()));
        } catch (Exception e) {
            log.error("Error while scanning repository", e);
            return ExitCode.SOFTWARE;
        }
        return ExitCode.OK;
    }

    private List<String> readFile(Path path) {
        try {
            return Files.readAllLines(path).stream()
                    .filter(line -> !line.isBlank())
                    .map(line -> line.split("#")[0])
                    .map(String::trim)
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Error while reading file " + path, e);
        }
    }
}
