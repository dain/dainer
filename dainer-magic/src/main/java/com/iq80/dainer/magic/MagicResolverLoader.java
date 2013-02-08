package com.iq80.dainer.magic;

import com.iq80.dainer.spi.MavenResolver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class MagicResolverLoader
{
    public static void main(String[] args)
            throws Exception
    {
        MavenResolver mavenResolver = new MagicResolverLoader().loadMavenResolver();
        mavenResolver.resolvePom(new File("pom.xml"));
    }

    public MavenResolver loadMavenResolver()
    {
        try {
            return loadMavenResolver(getClass());
        }
        catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
    }

    private static MavenResolver loadMavenResolver(Class<?> baseClass)
            throws Exception
    {
        URL url = baseClass.getResource(baseClass.getSimpleName() + ".class");
        JarURLConnection urlConnection = (JarURLConnection) url.openConnection();
        JarFile jarFile = urlConnection.getJarFile();
        List<URL> urls = new ArrayList<>();
        for (JarEntry jarEntry : Collections.list(jarFile.entries())) {
            if (!jarEntry.isDirectory() && jarEntry.getName().startsWith("com/iq80/dainer/magic/deps/")) {
                try (InputStream inputStream = jarFile.getInputStream(jarEntry)) {
                    File file = copyToTempFile(inputStream, jarEntry.getName());
                    urls.add(file.toURL());
                }
            }
        }
        URLClassLoader urlClassLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]), baseClass.getClassLoader());
        Thread.currentThread().setContextClassLoader(urlClassLoader);
        ServiceLoader<MavenResolver> mavenResolvers = ServiceLoader.load(MavenResolver.class, urlClassLoader);
        Iterator<MavenResolver> iterator = mavenResolvers.iterator();
        if (!iterator.hasNext()) {
            throw new RuntimeException("Could not load " + MavenResolver.class.getSimpleName());
        }
        MavenResolver mavenResolver = iterator.next();
        if (iterator.hasNext()) {
            throw new RuntimeException("Expected only one " + MavenResolver.class.getSimpleName() + " instance");
        }
        return mavenResolver;
    }

    private static File copyToTempFile(InputStream inputStream, String x)
            throws Exception
    {
        File tempFile = File.createTempFile("loader", "jar");
        try (OutputStream outputStream = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[4096];
            while (true) {
                int size = inputStream.read(buffer);
                if (size == -1) {
                    break;
                }
                outputStream.write(buffer, 0, size);
            }
        }
        return tempFile;
    }
}
