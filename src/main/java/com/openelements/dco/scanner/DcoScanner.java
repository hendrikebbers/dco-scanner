package com.openelements.dco.scanner;

import de.siegmar.fastcsv.writer.CsvWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.eclipse.jgit.api.Git;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class DcoScanner {

    private final static Logger log = LoggerFactory.getLogger(DcoScanner.class);

    public static void main(String[] args) throws Exception {
        readFile(Path.of("internal-domains.txt"))
                .forEach(PersonFactory.getInstance()::addInternalDomain);
        readFile(Path.of("internal-emails.txt")).forEach(PersonFactory.getInstance()::addInternalEmail);
        readFile(Path.of("internal-github-users.txt"))
                .forEach(PersonFactory.getInstance()::addInternalGitHubUser);
        final List<String> repositories = readFile(Path.of("repositories.txt"));

        final List<OutputEntry> allNonValidCommits = new CopyOnWriteArrayList<>();
        repositories.stream().parallel()
                .forEach(repo -> {
                    try {
                        MDC.put("repository", repo);
                        log.info("Scanning repository {}", repo);
                        final List<Commit> nonValidCommits = scanRepositoryForNonValidCommits(repo);
                        if (!nonValidCommits.isEmpty()) {
                            log.info("Repository {} contains non valid commits", repo);
                            final String org = repo.substring("https://github.com/".length()).split("/")[0];
                            final String name = repo.substring("https://github.com/".length()).split("/")[1];
                            final Path outputDir = Path.of("out/" + org);
                            Files.createDirectories(outputDir);
                            final Path path = Paths.get(outputDir.toString(), name + ".txt");
                            if (Files.exists(path)) {
                                Files.delete(path);
                            }
                            Files.write(path,
                                    nonValidCommits.stream().map(c -> toPrintableString(repo, c)).toList());
                            nonValidCommits.stream().forEach(c -> c.invalidPersons().stream()
                                    .map(p -> new OutputEntry(repo, c, p))
                                    .forEach(allNonValidCommits::add));
                        } else {
                            log.info("Repository {} is clean", repo);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Error while handling repository " + repo, e);
                    } finally {
                        MDC.remove("repository");
                    }
                });
        final Path outputDir = Path.of("out/all.txt");
        if (Files.exists(outputDir)) {
            Files.delete(outputDir);
        }
        try (CsvWriter csv = CsvWriter.builder().build(outputDir)) {
            csv.writeRecord("commit", "name", "email", "GithubAccount");
            allNonValidCommits.stream()
                    .forEach(d -> {
                        if (d.githubAccount() != null) {
                            csv.writeRecord(d.commitLink(), d.name(), "-", "hhtps://github/" + d.githubAccount());
                        } else {
                            csv.writeRecord(d.commitLink(), d.name(), d.email(), "-");
                        }
                    });
        }

        Files.write(outputDir, allNonValidCommits.stream().map(c -> c.toString()).toList());
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
