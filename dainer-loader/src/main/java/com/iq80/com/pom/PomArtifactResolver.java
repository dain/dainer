package com.iq80.com.pom;

import com.google.inject.AbstractModule;
import com.iq80.com.aether.ArtifactResolver;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.logging.Logger;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.resolution.DependencyResolutionException;
import org.sonatype.aether.util.artifact.DefaultArtifact;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PomArtifactResolver
{
    private final ArtifactResolver artifactResolver;

    public PomArtifactResolver(ArtifactResolver artifactResolver)
    {
        this.artifactResolver = artifactResolver;
    }

    public List<Artifact> resolvePom(File pomFile)
            throws DependencyResolutionException
    {
        if (pomFile == null) {
            throw new RuntimeException("pomFile is null");
        }

        MavenProject pom;
        try {
            PlexusContainer container = container();
            org.apache.maven.repository.RepositorySystem lrs = container.lookup(org.apache.maven.repository.RepositorySystem.class);
            ProjectBuilder projectBuilder = container.lookup(ProjectBuilder.class);
            ProjectBuildingRequest request = new DefaultProjectBuildingRequest();
            request.setRepositorySession(artifactResolver.getRepositorySystemSession());
            request.setProcessPlugins(false);
            request.setLocalRepository(lrs.createDefaultLocalRepository());
            request.setRemoteRepositories(Arrays.asList(new ArtifactRepository[]{lrs.createDefaultRemoteRepository()}.clone()));
            ProjectBuildingResult result = projectBuilder.build(pomFile, request);
            pom = result.getProject();
        }
        catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException("Error loading pom: " + pomFile.getAbsolutePath(), e);
        }

        List<Artifact> sourceArtifacts = new ArrayList<>();
        for (Dependency dependency : pom.getDependencies()) {
            Artifact artifact = new DefaultArtifact(dependency.getGroupId(), dependency.getArtifactId(), dependency.getClassifier(), dependency.getType(), dependency.getVersion());
            sourceArtifacts.add(artifact);
        }
        return artifactResolver.resolveArtifacts(sourceArtifacts);
    }

    private static PlexusContainer container()
    {
        try {
            ClassWorld classWorld = new ClassWorld("plexus.core", Thread.currentThread().getContextClassLoader());

            ContainerConfiguration cc = new DefaultContainerConfiguration()
                    .setClassWorld(classWorld)
                    .setRealm(null).setClassPathScanning(PlexusConstants.SCANNING_INDEX)
                    .setAutoWiring(true)
                    .setName("maven");

            DefaultPlexusContainer container = new DefaultPlexusContainer(cc, new AbstractModule()
            {
                protected void configure()
                {
                    bind(ILoggerFactory.class).toInstance(LoggerFactory.getILoggerFactory());
                }
            });

            // NOTE: To avoid inconsistencies, we'll use the TCCL exclusively for lookups
            container.setLookupRealm(null);
            container.setLoggerManager(new Slf4jLoggerManager());
            container.getLoggerManager().setThresholds(Logger.LEVEL_INFO);
            Thread.currentThread().setContextClassLoader(container.getContainerRealm());

            return container;
        }
        catch (PlexusContainerException e) {
            throw new RuntimeException("Error loading Maven system", e);
        }
    }
}
