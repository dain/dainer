package com.iq80.com;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.repository.internal.DefaultServiceLocator;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
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

import com.iq80.com.aether.ConsoleDependencyGraphDumper;
import com.iq80.com.aether.ConsoleRepositoryListener;
import com.iq80.com.aether.ConsoleTransferListener;

public class Dainer {

  public static void main(String[] args) throws Exception {
    Dainer dainer = new Dainer();
    dainer.resolve(new File(System.getProperty("user.dir"), "pom.xml"));
  }

  public void resolve(File pom) throws Exception {
    MavenXpp3Reader reader = new MavenXpp3Reader();
    Model model = reader.read(new FileInputStream(pom));

    CollectRequest collectRequest = new CollectRequest();

    for (Dependency dependency : model.getDependencies()) {
      Artifact artifact = new DefaultArtifact(dependency.getGroupId(), dependency.getArtifactId(), dependency.getClassifier(), dependency.getType(), dependency.getVersion());
      collectRequest.addDependency(new org.sonatype.aether.graph.Dependency(artifact, JavaScopes.RUNTIME));
    }

    resolve(collectRequest);
  }

  public void resolve(String coordinate) throws Exception {
    Artifact artifact = new DefaultArtifact(coordinate);
    CollectRequest collectRequest = new CollectRequest();
    collectRequest.setRoot(new org.sonatype.aether.graph.Dependency(artifact, JavaScopes.RUNTIME));
    resolve(collectRequest);
  }
  
  
  public void resolve(CollectRequest collectRequest) throws Exception {
    collectRequest.addRepository(newCentralRepository());
    
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

  public static RepositorySystem newRepositorySystem() {
    DefaultServiceLocator locator = new DefaultServiceLocator();
    locator.addService(RepositoryConnectorFactory.class, FileRepositoryConnectorFactory.class);
    locator.addService(RepositoryConnectorFactory.class, AsyncRepositoryConnectorFactory.class);
    return locator.getService(RepositorySystem.class);
  }

  public static DefaultRepositorySystemSession newRepositorySystemSession(RepositorySystem system) {
    MavenRepositorySystemSession session = new MavenRepositorySystemSession();

    LocalRepository localRepo = new LocalRepository("target/local-repo");
    session.setLocalRepositoryManager(system.newLocalRepositoryManager(localRepo));

    session.setTransferListener(new ConsoleTransferListener());
    session.setRepositoryListener(new ConsoleRepositoryListener());

    return session;
  }

  public static RemoteRepository newCentralRepository() {
    return new RemoteRepository("central", "default", "http://repo1.maven.org/maven2/");
  }

}
