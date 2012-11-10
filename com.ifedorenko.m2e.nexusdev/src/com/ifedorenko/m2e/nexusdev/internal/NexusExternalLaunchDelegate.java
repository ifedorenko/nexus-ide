package com.ifedorenko.m2e.nexusdev.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.util.DirectoryScanner;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.JavaLaunchDelegate;

public class NexusExternalLaunchDelegate
    extends JavaLaunchDelegate
{
    File basedir = new File( "/opt/nexus/nexus-professional-2.2-01" );

    @Override
    public File verifyWorkingDirectory( ILaunchConfiguration configuration )
        throws CoreException
    {
        return basedir;
    }

    @Override
    public String verifyMainTypeName( ILaunchConfiguration configuration )
        throws CoreException
    {
        return "org.sonatype.nexus.bootstrap.Launcher";
    }

    @Override
    public String[] getClasspath( ILaunchConfiguration configuration )
        throws CoreException
    {

        DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir( basedir );
        ds.setIncludes( new String[] { "lib/*.jar", "conf" } );
        ds.scan();

        List<String> cp = new ArrayList<String>();

        for ( String path : ds.getIncludedFiles() )
        {
            cp.add( path );
        }

        for ( String path : ds.getIncludedDirectories() )
        {
            cp.add( path );
        }

        return cp.toArray( new String[cp.size()] );
    }

    @Override
    public String getProgramArguments( ILaunchConfiguration configuration )
        throws CoreException
    {
        return "./conf/jetty.xml";
    }

    @Override
    public String getVMArguments( ILaunchConfiguration configuration )
        throws CoreException
    {
        StringBuilder sb = new StringBuilder();
        sb.append( super.getVMArguments( configuration ) );
        return sb.toString();
    }
}
