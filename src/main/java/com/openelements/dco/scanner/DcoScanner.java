package com.openelements.dco.scanner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.eclipse.jgit.api.Git;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DcoScanner {

    private final static Logger log = LoggerFactory.getLogger(DcoScanner.class);

    public static void main(String[] args) throws Exception {
        readFile(Path.of("internal-domains.txt"))
                .forEach(PersonFactory.getInstance()::addInternalDomain);
        readFile(Path.of("internal-emails.txt")).forEach(PersonFactory.getInstance()::addInternalEmail);
        readFile(Path.of("internal-github-users.txt"))
                .forEach(PersonFactory.getInstance()::addInternalGitHubUser);
        final List<String> repositories = readFile(Path.of("repositories.txt"));

        repositories.stream().parallel()
                .forEach(repo -> {
                    try {
                        handleRepository(repo);
                    } catch (Exception e) {
                        throw new RuntimeException("Error while handling repository " + repo, e);
                    }
                });
    }

    private static void handleRepository(String repositoryUrl) throws Exception {
        final String org = repositoryUrl.substring("https://github.com/".length()).split("/")[0];
        final String name = repositoryUrl.substring("https://github.com/".length()).split("/")[1];
        log.info("Scanning repository {}", repositoryUrl);
        final List<Commit> nonValidCommits = scanRepositoryForNonValidCommits(repositoryUrl);
        if (!nonValidCommits.isEmpty()) {
            log.info("Repository {} contains non valid commits", repositoryUrl);
            final Path outputDir = Path.of("out/" + org);
            Files.createDirectories(outputDir);
            final Path path = Paths.get(outputDir.toString(), name + ".txt");
            if (Files.exists(path)) {
                Files.delete(path);
            }
            Files.write(path, nonValidCommits.stream().map(c -> toPrintableString(repositoryUrl, c)).toList());
        } else {
            log.info("Repository {} is clean", repositoryUrl);
        }
    }

    private static String toPrintableString(final String repositoryUrl, final Commit nonValidCommit) {
        return repositoryUrl + "/commit/" + nonValidCommit.identifier() + " " + nonValidCommit.invalidPersons();
    }

    private static List<Commit> scanRepositoryForNonValidCommits(final String repositoryUrl) {
        final Git git = RepositoryFactory.checkout(repositoryUrl);
        final List<Commit> list = CommitFactory.createFor(git);
        return list.stream()
                .filter(commit -> !commit.isValid())
                .toList();
    }

    private static List<String> readFile(Path path) {
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
