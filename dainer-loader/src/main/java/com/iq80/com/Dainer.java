package com.iq80.com;

import com.google.inject.AbstractModule;
import com.iq80.com.aether.ConsoleDependencyGraphDumper;
import com.iq80.com.aether.ConsoleRepositoryListener;
import com.iq80.com.aether.ConsoleTransferListener;
import com.iq80.com.aether.Slf4jLoggerManager;
import com.iq80.dainer.spi.MavenResolver;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.apache.maven.repository.internal.DefaultServiceLocator;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.logging.Logger;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.CollectResult;
import org.sonatype.aether.connector.async.AsyncRepositoryConnectorFactory;
import org.sonatype.aether.connector.file.FileRepositoryConnectorFactory;
import org.sonatype.aether.graph.DependencyFilter;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.resolution.DependencyRequest;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;
import org.sonatype.aether.util.DefaultRepositorySystemSession;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.artifact.JavaScopes;
import org.sonatype.aether.util.filter.DependencyFilterUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class Dainer
    implements MavenResolver
{

    public static void main(String[] args)
            throws Exception
    {
        Dainer dainer = new Dainer();
        dainer.resolvePom(new File(args[0]));
    }

    @Override
    public void resolvePom(File pom)
            throws Exception
    {

        RemoteRepository repo = newCentralRepository();
        RepositorySystem system = newRepositorySystem();
        RepositorySystemSession session = newRepositorySystemSession(system);

        PlexusContainer container = container();
        org.apache.maven.repository.RepositorySystem lrs = container.lookup(org.apache.maven.repository.RepositorySystem.class);
        ProjectBuilder projectBuilder = container.lookup(ProjectBuilder.class);
        ProjectBuildingRequest request = new DefaultProjectBuildingRequest();
        request.setRepositorySession(session);
        request.setProcessPlugins(false);
        request.setLocalRepository(lrs.createDefaultLocalRepository());
        request.setRemoteRepositories(Arrays.asList(new ArtifactRepository[]{lrs.createDefaultRemoteRepository()}));
        ProjectBuildingResult result = projectBuilder.build(pom, request);
        System.out.println(result.getProject());

        resolve(result.getProject());
    }

    public void resolve(MavenProject pom)
            throws Exception
    {

        CollectRequest collectRequest = new CollectRequest();

        for (Dependency dependency : pom.getDependencies()) {
            Artifact artifact = new DefaultArtifact(dependency.getGroupId(), dependency.getArtifactId(), dependency.getClassifier(), dependency.getType(), dependency.getVersion());
            collectRequest.addDependency(new org.sonatype.aether.graph.Dependency(artifact, JavaScopes.RUNTIME));
        }
        resolve(collectRequest);
    }

    @Override
    public void resolveCoordinate(String coordinate)
            throws Exception
    {
        Artifact artifact = new DefaultArtifact(coordinate);
        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new org.sonatype.aether.graph.Dependency(artifact, JavaScopes.RUNTIME));
        resolve(collectRequest);
    }

    public void resolve(CollectRequest collectRequest)
            throws Exception
    {
        collectRequest.addRepository(newCentralRepository());

        RemoteRepository repo = newCentralRepository();
        RepositorySystem system = newRepositorySystem();
        RepositorySystemSession session = newRepositorySystemSession(system);

        DependencyFilter classpathFlter = DependencyFilterUtils.classpathFilter(JavaScopes.RUNTIME);

        // If you want the grab the file and do somethign with them: like make a classloader
        DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, classpathFlter);
        List<ArtifactResult> artifactResults = system.resolveDependencies(session, dependencyRequest).getArtifactResults();
        for (ArtifactResult artifactResult : artifactResults) {
            System.out.println(artifactResult.getArtifact() + " resolved to " + artifactResult.getArtifact().getFile());
        }

        // If you want to show the result: say in a tree
        CollectResult collectResult = system.collectDependencies(session, collectRequest);
        collectResult.getRoot().accept(new ConsoleDependencyGraphDumper());
    }

    public static RepositorySystem newRepositorySystem()
    {
        DefaultServiceLocator locator = new DefaultServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, FileRepositoryConnectorFactory.class);
        locator.addService(RepositoryConnectorFactory.class, AsyncRepositoryConnectorFactory.class);
        return locator.getService(RepositorySystem.class);
    }

    public static DefaultRepositorySystemSession newRepositorySystemSession(RepositorySystem system)
    {
        MavenRepositorySystemSession session = new MavenRepositorySystemSession();

        LocalRepository localRepo = new LocalRepository("target/local-repo");
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(localRepo));

        session.setTransferListener(new ConsoleTransferListener());
        session.setRepositoryListener(new ConsoleRepositoryListener());

        return session;
    }

    public static RemoteRepository newCentralRepository()
    {
        return new RemoteRepository("central", "default", "http://repo1.maven.org/maven2/");
    }

    public PlexusContainer container()
            throws Exception
    {

        ClassWorld classWorld = new ClassWorld("plexus.core", Thread.currentThread().getContextClassLoader());

        DefaultPlexusContainer container = null;

        ContainerConfiguration cc = new DefaultContainerConfiguration()
                .setClassWorld(classWorld)
                .setRealm(null).setClassPathScanning(PlexusConstants.SCANNING_INDEX)
                .setAutoWiring(true)
                .setName("maven");

        container = new DefaultPlexusContainer(cc, new AbstractModule()
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

}
