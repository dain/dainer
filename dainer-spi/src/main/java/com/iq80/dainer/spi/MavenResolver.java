package com.iq80.dainer.spi;

import java.io.File;

public interface MavenResolver
{
    void resolveCoordinate(String coordinate)
            throws Exception;

    void resolvePom(File pom)
            throws Exception;
}
