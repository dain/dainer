package com.iq80.com.aether;

import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

import static com.iq80.com.aether.ArtifactResolver.MAVEN_CENTRAL_URI;

public class ArtifactResolverTest
{
    @Test
    public void testResolveArtifacts()
            throws Exception
    {
        ArtifactResolver artifactResolver = new ArtifactResolver("target/local-repo", MAVEN_CENTRAL_URI);
        List<Artifact> artifacts = artifactResolver.resolveArtifacts(new DefaultArtifact("org.apache.maven:maven-core:3.0.4"));

        Assert.assertNotNull(artifacts, "artifacts is null");
        for (Artifact artifact : artifacts) {
            Assert.assertNotNull(artifact.getFile(), "Artifact " + artifact + " is not resolved");
        }
    }
}
