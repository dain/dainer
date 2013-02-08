package com.iq80.dainer.magic;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public class Foo
{

    public static void main(String[] args)
            throws Exception
    {
        List<URL> urls = new ArrayList<>();
        urls.add(new File("../dainer-spi/target/dainer-spi-1.0-SNAPSHOT.jar").toURL());
        for (File file : new File("target/foo").listFiles()) {
            if (file.isFile()) {
                urls.add(file.toURL());
            }
        }

        URLClassLoader urlClassLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]), null);
        Thread.currentThread().setContextClassLoader(urlClassLoader);
        Class<?> aClass = urlClassLoader.loadClass("com.iq80.com.Dainer");
        aClass.getMethod("main", String[].class).invoke(null, new Object[] {new String[]{"pom.xml"}});
    }
}
//
////        File file = new File("target/dainer-magic-1.0-SNAPSHOT-magic.jar");
////        if (!file.exists()) {
////            throw new RuntimeException("No file " + file.getAbsolutePath());
////        }
////        URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{file.toURL()}.clone(), null);
////        Class<?> aClass = urlClassLoader.loadClass("com.iq80.dainer.magic.MagicResolverLoader");
//        aClass.getMethod("main", String[].class).invoke(null, new Object[] {new String[]{"pom.xml"}});
//
//
////        MavenResolver mavenResolver = loadMavenResolver(aClass);
////        mavenResolver.resolvePom(new File("pom.xml"));
//    }
////
////    private static MavenResolver loadMavenResolver(Class<?> baseClass)
////            throws Exception
////    {
////        URL url = baseClass.getResource(baseClass.getSimpleName() + ".class");
////        JarURLConnection urlConnection = (JarURLConnection) url.openConnection();
////        JarFile jarFile = urlConnection.getJarFile();
////        List<URL> urls = new ArrayList<>();
////        for (JarEntry jarEntry : Collections.list(jarFile.entries())) {
////            if (jarEntry.getName().startsWith("com/iq80/dainer/magic/deps/")) {
////                try (InputStream inputStream = jarFile.getInputStream(jarEntry)) {
////                    File file = copyToTempFile(inputStream);
////                    urls.add(file.toURL());
////                    System.out.println(jarEntry.getName());
////                }
////            }
////        }
////        URLClassLoader urlClassLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]), null);
////        ServiceLoader<MavenResolver> mavenResolvers = ServiceLoader.load(MavenResolver.class, urlClassLoader);
////        Iterator<MavenResolver> iterator = mavenResolvers.iterator();
////        if (!iterator.hasNext()) {
////            throw new RuntimeException("Could not load " + MavenResolver.class.getSimpleName());
////        }
////        MavenResolver mavenResolver = iterator.next();
////        if (iterator.hasNext()) {
////            throw new RuntimeException("Expected only one " + MavenResolver.class.getSimpleName() + " instance");
////        }
////        return mavenResolver;
////    }
////
////    private static File copyToTempFile(InputStream inputStream)
////            throws Exception
////    {
////        File tempFile = File.createTempFile("loader", "jar");
////        try (OutputStream outputStream = new FileOutputStream(tempFile)) {
////            byte[] buffer = new byte[4096];
////            while (true) {
////                int size = inputStream.read(buffer);
////                if (size == -1) {
////                    break;
////                }
////                outputStream.write(buffer, 0, size);
////            }
////        }
////        return tempFile;
////    }
//}
