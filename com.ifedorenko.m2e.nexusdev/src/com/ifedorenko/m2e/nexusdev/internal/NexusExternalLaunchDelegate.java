package com.ifedorenko.m2e.nexusdev.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.util.DirectoryScanner;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;
import org.eclipse.jdt.internal.launching.JavaSourceLookupDirector;
import org.eclipse.jdt.launching.JavaLaunchDelegate;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaSourceLookupParticipant;

import com.ifedorenko.m2e.sourcelookup.internal.SourceLookupMavenLaunchParticipant;

@SuppressWarnings( "restriction" )
public class NexusExternalLaunchDelegate
    extends JavaLaunchDelegate
{
    public static final String ATTR_INSTALLATION_LOCATION = "nexusdev.installationLocation";

    private static final SourceLookupMavenLaunchParticipant sourcelookup = new SourceLookupMavenLaunchParticipant();

    private ILaunch launch;

    private IProgressMonitor monitor;

    private String mode;

    @Override
    public void launch( final ILaunchConfiguration configuration, final String mode, final ILaunch launch,
                        final IProgressMonitor monitor )
        throws CoreException
    {
        this.mode = mode;
        this.launch = launch;
        this.monitor = monitor;
        try
        {
            JavaSourceLookupDirector sourceLocator = new JavaSourceLookupDirector()
            {
                @Override
                public void initializeParticipants()
                {
                    List<ISourceLookupParticipant> participants = new ArrayList<ISourceLookupParticipant>();
                    if ( ILaunchManager.DEBUG_MODE.equals( mode ) )
                    {
                        participants.addAll( sourcelookup.getSourceLookupParticipants( configuration, launch, monitor ) );
                    }
                    participants.add( new JavaSourceLookupParticipant() );
                    addParticipants( participants.toArray( new ISourceLookupParticipant[participants.size()] ) );
                }
            };
            sourceLocator.initializeParticipants();

            launch.setSourceLocator( sourceLocator );

            super.launch( configuration, mode, launch, monitor );
        }
        finally
        {
            this.mode = null;
            this.launch = null;
            this.monitor = null;
        }
    }

    @Override
    public File verifyWorkingDirectory( ILaunchConfiguration configuration )
        throws CoreException
    {
        return getInstallationDirectory( configuration );
    }

    private File getInstallationDirectory( ILaunchConfiguration configuration )
        throws CoreException
    {
        String location = configuration.getAttribute( ATTR_INSTALLATION_LOCATION, (String) null );

        if ( location == null || "".equals( location.trim() ) )
        {
            throw new CoreException( new Status( IStatus.ERROR, NexusdevActivator.BUNDLE_ID,
                                                 "Installation location is null" ) );
        }

        File directory = new File( location );

        if ( !directory.isDirectory() )
        {
            throw new CoreException( new Status( IStatus.ERROR, NexusdevActivator.BUNDLE_ID,
                                                 "Installation location is not a directory" ) );
        }

        return directory;
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
        ds.setBasedir( getInstallationDirectory( configuration ) );
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
        append( sb, super.getVMArguments( configuration ) );
        if ( ILaunchManager.DEBUG_MODE.equals( mode ) )
        {
            append( sb, sourcelookup.getVMArguments( configuration, launch, monitor ) );
        }
        return sb.toString();
    }

    private void append( StringBuilder sb, String str )
    {
        if ( str != null && !"".equals( str.trim() ) )
        {
            sb.append( ' ' ).append( str );
        }
    }
}
