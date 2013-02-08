package com.iq80.dainer.magic;

import com.iq80.dainer.spi.MavenResolver;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

public class MagicResolverLoader
{
    public static void main(String[] args)
    {
        new MagicResolverLoader().loadMavenResolver();
    }

    public MavenResolver loadMavenResolver()
    {
        try {
            URL url = new File("dainer-loader/target/dainer-loader-1.0-SNAPSHOT.jar").toURL();
            URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{url});
            Class<? extends MavenResolver> mavenResolverClass = urlClassLoader.loadClass("com.iq80.com.Dainer").asSubclass(MavenResolver.class);
            MavenResolver mavenResolver = mavenResolverClass.newInstance();
            return mavenResolver;
        }
        catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
    }
}
