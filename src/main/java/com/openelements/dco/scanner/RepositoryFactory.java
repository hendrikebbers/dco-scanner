package com.openelements.dco.scanner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.eclipse.jgit.api.Git;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryFactory {

    private final static Logger log = LoggerFactory.getLogger(RepositoryFactory.class);


    private RepositoryFactory() {

    }

    public static Git checkout(final String repositoryUrl) {
        Objects.requireNonNull(repositoryUrl, "Repository URL must not be null");
        if (repositoryUrl.isBlank()) {
            throw new IllegalArgumentException("Repository URL must not be blank");
        }
        final Path tempDirectory = createTempDirectory();
        log.debug("Cloning repository {} to {}", repositoryUrl, tempDirectory);
        try {
            final Git git = Git.cloneRepository()
                    .setURI(repositoryUrl)
                    .setDirectory(tempDirectory.toFile())
                    .call();
            return git;
        } catch (Exception e) {
            throw new RuntimeException("Error while checking out repository '" + repositoryUrl + "'", e);
        }
    }

    private static Path createTempDirectory() {
        try {
            Path tempDir = Files.createTempDirectory("dco-scanner");
            tempDir.toFile().deleteOnExit();
            return tempDir;
        } catch (IOException e) {
            throw new RuntimeException("Unable to create Temp repository", e);
        }
    }
}
