package com.iq80.com.pom;

import com.iq80.com.aether.ArtifactResolver;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.resolution.DependencyResolutionException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;

import static com.iq80.com.aether.ArtifactResolver.MAVEN_CENTRAL_URI;

public class TestPomArtifactResolver {
    @Test
    public void test()
            throws DependencyResolutionException
    {
        File pomFile = new File("dainer-loader/src/test/poms/pom.xml");
        Assert.assertTrue(pomFile.canRead());

        ArtifactResolver artifactResolver = new ArtifactResolver("target/local-repo", MAVEN_CENTRAL_URI);
        PomArtifactResolver pomArtifactResolver = new PomArtifactResolver(artifactResolver);
        List<Artifact> artifacts = pomArtifactResolver.resolvePom(pomFile);

        Assert.assertNotNull(artifacts, "artifacts is null");
        for (Artifact artifact : artifacts) {
            Assert.assertNotNull(artifact.getFile(), "Artifact " + artifact + " is not resolved");
        }
    }
}
