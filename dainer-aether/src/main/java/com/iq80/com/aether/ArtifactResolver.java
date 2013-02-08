package com.iq80.com.aether;


import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.apache.maven.repository.internal.MavenServiceLocator;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.connector.async.AsyncRepositoryConnectorFactory;
import org.sonatype.aether.connector.file.FileRepositoryConnectorFactory;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.resolution.DependencyRequest;
import org.sonatype.aether.resolution.DependencyResolutionException;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;
import org.sonatype.aether.util.artifact.JavaScopes;
import org.sonatype.aether.util.filter.DependencyFilterUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ArtifactResolver
{
    public static final String MAVEN_CENTRAL_URI = "http://repo1.maven.org/maven2/";

    private final RepositorySystem repositorySystem;
    private final MavenRepositorySystemSession repositorySystemSession;
    private final List<RemoteRepository> repositories;

    public ArtifactResolver(String localRepositoryDir, String... remoteRepositoryUris)
    {
        this(localRepositoryDir, Arrays.asList(remoteRepositoryUris));
    }

    public MavenRepositorySystemSession getRepositorySystemSession()
    {
        return repositorySystemSession;
    }

    public ArtifactResolver(String localRepositoryDir, List<String> remoteRepositoryUris)
    {
        MavenServiceLocator locator = new MavenServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, FileRepositoryConnectorFactory.class);
        locator.addService(RepositoryConnectorFactory.class, AsyncRepositoryConnectorFactory.class);
        repositorySystem = locator.getService(RepositorySystem.class);

        repositorySystemSession = new MavenRepositorySystemSession();

        LocalRepository localRepo = new LocalRepository(localRepositoryDir);
        repositorySystemSession.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(localRepo));

        repositorySystemSession.setTransferListener(new ConsoleTransferListener());
        repositorySystemSession.setRepositoryListener(new ConsoleRepositoryListener());

        List<RemoteRepository> repositories = new ArrayList<>(remoteRepositoryUris.size());
        int index = 0;
        for (String repositoryUri : remoteRepositoryUris) {
            repositories.add(new RemoteRepository("repo-" + index++, "default", repositoryUri));
        }
        this.repositories = Collections.unmodifiableList(repositories);
    }

    public List<Artifact> resolveArtifacts(Artifact... sourceArtifacts)
            throws DependencyResolutionException
    {
        return resolveArtifacts(Arrays.asList(sourceArtifacts));
    }

    public List<Artifact> resolveArtifacts(List<Artifact> sourceArtifacts)
            throws DependencyResolutionException
    {
        CollectRequest collectRequest = new CollectRequest();
        for (Artifact sourceArtifact : sourceArtifacts) {
            collectRequest.setRoot(new Dependency(sourceArtifact, JavaScopes.RUNTIME));
        }
        for (RemoteRepository repository : repositories) {
            collectRequest.addRepository(repository);
        }

        DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, DependencyFilterUtils.classpathFilter(JavaScopes.RUNTIME));

        List<ArtifactResult> artifactResults = repositorySystem.resolveDependencies(repositorySystemSession, dependencyRequest).getArtifactResults();
        List<Artifact> artifacts = new ArrayList<>(artifactResults.size());
        for (ArtifactResult artifactResult : artifactResults) {
            artifacts.add(artifactResult.getArtifact());
        }

        return Collections.unmodifiableList(artifacts);
    }
}
