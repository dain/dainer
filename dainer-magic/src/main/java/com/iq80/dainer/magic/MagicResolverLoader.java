package com.iq80.dainer.magic;

import com.iq80.dainer.spi.MavenResolver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;

public class MagicResolverLoader
{
    public static void main(String[] args)
            throws Exception
    {
        MavenResolver mavenResolver = new MagicResolverLoader().loadMavenResolver();
        mavenResolver.resolvePom(new File(args[0]));
    }

    public MavenResolver loadMavenResolver()
    {
        try {
            File tempFile = File.createTempFile("loader", "jar");
            URL url = getClass().getResource("loader.jar");
            try (
                    InputStream inputStream = url.openStream();
                    OutputStream outputStream = new FileOutputStream(tempFile);
            ) {
                byte[] buffer = new byte[4096];
                while (true) {
                    int size = inputStream.read(buffer);
                    if (size == -1) {
                        break;
                    }
                    outputStream.write(buffer, 0, size);
                }
            }


            URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{tempFile.toURL()});
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
